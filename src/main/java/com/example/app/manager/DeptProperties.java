package com.example.app.manager;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 防腐层配置：{@code dept.provider} 的合法取值由 {@link DeptProviderMode} 枚举约束。
 * <p>与旧实现的差别：旧配置只把 provider 当字符串散落在两个 Manager 的
 * {@code @ConditionalOnProperty} 上，没有任何地方声明"合法取值有哪些"，
 * 拼成 local/remot 之类只会静默回落到默认实现。这里显式建模后，
 * 非法取值在启动期即失败。</p>
 */
@Data
@ConfigurationProperties(prefix = "dept")
public class DeptProperties {

    /** 提供方模式：local=配置模拟（默认），remote=HTTP 调用部门服务 */
    private DeptProviderMode provider = DeptProviderMode.LOCAL;

    /** 本地模拟数据 */
    private final Local local = new Local();

    /** 远程调用配置 */
    private final Remote remote = new Remote();

    @Data
    public static class Local {

        /** 模拟的部门列表 */
        private List<LocalDept> depts;
    }

    @Data
    public static class Remote {

        /** 部门服务地址 */
        private String baseUrl;
    }

    @Data
    public static class LocalDept {

        /** 部门 ID */
        private Long id;
        /** 部门名称 */
        private String name;
        /** 是否启用（包装类型，可区分未配置） */
        private Boolean enabled;
    }
}
