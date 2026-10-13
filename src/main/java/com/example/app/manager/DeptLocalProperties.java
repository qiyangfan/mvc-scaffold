package com.example.app.manager;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 本地部门数据配置：demo 环境无真实部门服务，用配置文件模拟部门数据源。
 */
@Data
@ConfigurationProperties(prefix = "dept.local")
public class DeptLocalProperties {

    /** 模拟的部门列表 */
    private List<LocalDept> depts;

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
