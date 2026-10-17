package com.example.app.manager;

/**
 * 防腐层提供方模式：配置 {@code dept.provider} 的合法取值。
 * <p>绑定为枚举而非字符串是刻意的 fail-closed 设计：取值非法（既非 local 也非 remote）时
 * Spring 类型转换直接失败、应用起不来。历史上这类开关用字符串 + if/else 兜底，
 * 拼写错误会被静默忽略并一路带到生产环境才暴露。</p>
 */
public enum DeptProviderMode {

    /** 本地配置模拟（演示/测试） */
    LOCAL,

    /** 远程 HTTP 调用部门服务（生产） */
    REMOTE
}
