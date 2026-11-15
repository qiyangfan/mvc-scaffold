package com.example.app.manager;

import com.example.app.common.exception.BizException;
import com.example.app.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 远程部门 Manager：通过 HTTP 调用部门服务（dept.provider=remote 时生效）。
 * <p>防腐要点：外部响应模型（{@link ExternalDeptResponse}）不出本类；HTTP 异常统一翻译为 THIRD_PARTY_ERROR。</p>
 * <p>事实纪律：空响应体/非成功业务码属来源故障（THIRD_PARTY_ERROR），不得伪装成「确认不存在」；
 * 批量读取只容忍确认未命中，来源故障必须上抛，fail 方向 fail-closed。</p>
 * <p>{@link RestClient} 由 {@link DeptRemoteConfig} 装配后注入——本类不关心超时与 baseUrl
 * 从哪来，也因此可以在单测里换掉客户端（见 {@code RemoteDeptManagerTest}）。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "dept", name = "provider", havingValue = "remote")
@RequiredArgsConstructor
public class RemoteDeptManager implements DeptManager {

    /** 外部部门服务成功码：外部契约与本工程 R 同构（"0"=成功） */
    private static final String EXTERNAL_SUCCESS_CODE = "0";

    private final RestClient restClient;

    @Override
    public DeptInfo getDeptInfo(Long deptId) {
        ExternalDeptResponse response;
        try {
            response = restClient.get()
                    .uri("/api/depts/{id}", deptId)
                    .retrieve()
                    .body(ExternalDeptResponse.class);
        } catch (RestClientException e) {
            log.error("call dept service failed, deptId:{}", deptId, e);
            throw new BizException(ErrorCode.THIRD_PARTY_ERROR, e);
        }
        if (response == null || !EXTERNAL_SUCCESS_CODE.equals(response.code())) {
            // 空响应体或非成功业务码均属来源异常，不是契约内的「确认不存在」
            throw new BizException(ErrorCode.THIRD_PARTY_ERROR);
        }
        if (response.data() == null) {
            throw new BizException(ErrorCode.DEPT_NOT_FOUND);
        }
        ExternalDeptResponse.DeptPayload payload = response.data();
        if (payload.enabled()) {
            return new DeptInfo(payload.deptId(), payload.deptName(), true);
        }
        throw new BizException(ErrorCode.DEPT_DISABLED);
    }

    @Override
    public List<DeptInfo> listDeptInfos(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return List.of();
        }
        List<DeptInfo> result = new ArrayList<>(deptIds.size());
        // 真实实现建议批量接口一次拉取；此处保持接口语义，逐个调用并只容忍确认未命中
        for (Long deptId : deptIds) {
            try {
                result.add(getDeptInfo(deptId));
            } catch (BizException e) {
                // 来源故障必须上抛，不得静默降级为「部门缺失」
                if (!Objects.equals(e.getErrorCode(), ErrorCode.DEPT_NOT_FOUND)
                        && !Objects.equals(e.getErrorCode(), ErrorCode.DEPT_DISABLED)) {
                    throw e;
                }
                log.warn("skip missing dept, deptId:{}, code:{}", deptId, e.getErrorCode().getCode());
            }
        }
        return result;
    }

    /**
     * 部门服务外部响应模型（防腐内部模型，禁止外泄）。
     */
    record ExternalDeptResponse(String code, String message, DeptPayload data) {

        record DeptPayload(Long deptId, String deptName, boolean enabled) {
        }
    }
}
