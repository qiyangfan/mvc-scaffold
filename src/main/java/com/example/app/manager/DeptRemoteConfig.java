package com.example.app.manager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 远程部门调用的 HTTP 客户端装配（dept.provider=remote 时生效）。
 *
 * <p>把超时与 baseUrl 这类<b>HTTP 管道参数</b>从 Manager 里挪到装配层：Manager 只该表达
 * "怎么调用部门服务"的业务语义，不该知道 {@code ClientHttpRequestFactory} 的存在。
 * 由此 {@link RemoteDeptManager} 变成纯构造注入，可以脱离 Spring 容器单测
 * （见 {@code RemoteDeptManagerTest}）。</p>
 *
 * <p>条件与 {@link RemoteDeptManager} 保持一致：只有 remote 模式才装配。
 * 否则 local 模式下这个 bean 也会被创建，进而因 {@code dept.remote.base-url} 未配置而起不来。</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "dept", name = "provider", havingValue = "remote")
public class DeptRemoteConfig {

    /** 连接超时：外部调用必须显式设置，无超时的外部调用会拖垮线程池 */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);

    /** 读取超时 */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

    @Bean
    public RestClient deptRestClient(DeptProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .baseUrl(properties.getRemote().getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
