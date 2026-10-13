package com.example.app.common.exception;

/**
 * 错误码枚举：遵循手册错误码分级思想——A 用户端 / B 系统执行侧 / C 第三方服务侧。
 * <p>message 为英文（面向国际化 HTTP 响应），具体业务语义由 code 承载。</p>
 */
public enum ErrorCode {

    /** 请求参数非法 */
    PARAM_INVALID("A0400", "invalid request parameter"),
    /** 系统内部错误 */
    SYSTEM_ERROR("B0001", "internal system error"),
    /** 业务规则冲突 */
    BIZ_RULE_CONFLICT("B0100", "business rule conflict"),
    /** 员工不存在 */
    EMPLOYEE_NOT_FOUND("B0101", "employee not found"),
    /** 部门不存在 */
    DEPT_NOT_FOUND("B0102", "department not found"),
    /** 部门已禁用 */
    DEPT_DISABLED("B0103", "department disabled"),
    /** 第三方服务调用失败 */
    THIRD_PARTY_ERROR("C0101", "third-party service error");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
