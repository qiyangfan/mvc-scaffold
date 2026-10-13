package com.example.app.manager;

import com.example.app.common.exception.BizException;
import com.example.app.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 远程部门 Manager：通过 HTTP 调用部门服务（dept.provider=remote 时生效）。
 * <p>防腐要点：外部响应模型（{@link ExternalDeptResponse}）不出本类；HTTP 异常统一翻译为 THIRD_PARTY_ERROR。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "dept", name = "provider", havingValue = "remote")
public class RemoteDeptManager implements DeptManager {

    private final RestClient restClient;

    public RemoteDeptManager(@Value("${dept.remote.base-url}") String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 超时必须显式设置：多实例部署下，无超时的外部调用会拖垮线程池
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

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
        if (response == null || response.data() == null) {
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
        // 真实实现建议批量接口一次拉取；此处保持接口语义，逐个调用并容忍单点失败
        for (Long deptId : deptIds) {
            try {
                result.add(getDeptInfo(deptId));
            } catch (BizException e) {
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
