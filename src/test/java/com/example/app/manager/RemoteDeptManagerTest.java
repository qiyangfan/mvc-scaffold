package com.example.app.manager;

import com.example.app.common.exception.BizException;
import com.example.app.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 远程部门 Manager 单测：用 {@link MockRestServiceServer} 拦截 HTTP，不需要起真服务、
 * 也不需要 Spring 容器——因为 {@link RestClient} 是构造注入的（见 {@link DeptRemoteConfig}）。
 *
 * <p>覆盖防腐层的四段职责：外部模型转换、异常翻译、空响应兜底、批量容错。</p>
 */
class RemoteDeptManagerTest {

    private static final String BASE_URL = "http://dept.internal";

    private MockRestServiceServer server;

    private RemoteDeptManager manager;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        // 必须先 bindTo 再 build：Mock 的请求工厂借此注入 builder，拦截后续所有请求
        server = MockRestServiceServer.bindTo(builder).build();
        manager = new RemoteDeptManager(builder.build());
    }

    @Test
    void getDeptInfoShouldConvertExternalModelToInternalModel() {
        server.expect(requestTo(BASE_URL + "/api/depts/1"))
                .andRespond(withSuccess(
                        """
                        {"code":"0","message":"ok","data":{"deptId":1,"deptName":"平台研发部","enabled":true}}
                        """,
                        MediaType.APPLICATION_JSON));

        DeptManager.DeptInfo info = manager.getDeptInfo(1L);

        // 防腐关键：拿到的是本工程内部模型，外部响应结构不外泄
        assertThat(info.deptId()).isEqualTo(1L);
        assertThat(info.deptName()).isEqualTo("平台研发部");
        assertThat(info.enabled()).isTrue();
        server.verify();
    }

    @Test
    void getDeptInfoShouldRejectDisabledDept() {
        server.expect(requestTo(BASE_URL + "/api/depts/3"))
                .andRespond(withSuccess(
                        """
                        {"code":"0","message":"ok","data":{"deptId":3,"deptName":"已裁撤部门","enabled":false}}
                        """,
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> manager.getDeptInfo(3L))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPT_DISABLED);
    }

    /** 响应体为 null：外部服务返回 200 但空 body，不能当成"部门存在" */
    @Test
    void getDeptInfoShouldTreatNullBodyAsNotFound() {
        server.expect(requestTo(BASE_URL + "/api/depts/9"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> manager.getDeptInfo(9L))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPT_NOT_FOUND);
    }

    /** data 段为 null：同样是不存在，且不得在解包时空指针 */
    @Test
    void getDeptInfoShouldTreatNullDataAsNotFound() {
        server.expect(requestTo(BASE_URL + "/api/depts/9"))
                .andRespond(withSuccess(
                        """
                        {"code":"0","message":"ok","data":null}
                        """,
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> manager.getDeptInfo(9L))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.DEPT_NOT_FOUND);
    }

    /**
     * 异常翻译：HTTP 层故障（5xx）不得以 RestClientException 形态泄漏到 Service 层。
     * <p>必须让服务端返回错误状态码才会走到 catch 分支——返回 200 但 data 为空
     * 走的是上面的 DEPT_NOT_FOUND 分支，两者不是同一条路径。</p>
     */
    @Test
    void getDeptInfoShouldTranslateHttpFailureToThirdPartyError() {
        server.expect(requestTo(BASE_URL + "/api/depts/1"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> manager.getDeptInfo(1L))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getErrorCode())
                .isEqualTo(ErrorCode.THIRD_PARTY_ERROR);
    }

    @Test
    void listDeptInfosShouldReturnEmptyForNullOrEmptyInput() {
        assertThat(manager.listDeptInfos(null)).isEmpty();
        assertThat(manager.listDeptInfos(List.of())).isEmpty();
        server.verify();
    }

    /** 批量容错：单个部门缺失只跳过并告警，不能让整页因为一个坏部门而失败 */
    @Test
    void listDeptInfosShouldSkipMissingDeptAndKeepTheRest() {
        server.expect(requestTo(BASE_URL + "/api/depts/1"))
                .andRespond(withSuccess(
                        """
                        {"code":"0","message":"ok","data":{"deptId":1,"deptName":"平台研发部","enabled":true}}
                        """,
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/depts/404"))
                .andRespond(withSuccess(
                        """
                        {"code":"0","message":"ok","data":null}
                        """,
                        MediaType.APPLICATION_JSON));

        List<DeptManager.DeptInfo> result = manager.listDeptInfos(List.of(1L, 404L));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().deptId()).isEqualTo(1L);
        server.verify();
    }
}
