package com.example.app.common.enums;

import java.util.Objects;

/**
 * 员工状态枚举：以枚举替代魔法值，业务代码禁止出现 status == 1 之类的裸判断。
 */
public enum EmployeeStatus {

    /** 试用期 */
    TRIAL(0, "试用期"),
    /** 正式 */
    REGULAR(1, "正式"),
    /** 离职 */
    RESIGNED(2, "离职");

    /** 状态码下界，用于入参快速校验 */
    public static final int MIN_CODE = 0;
    /** 状态码上界 */
    public static final int MAX_CODE = 2;

    private final Integer code;
    private final String desc;

    EmployeeStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 状态码是否合法 */
    public static boolean isValid(Integer code) {
        for (EmployeeStatus status : values()) {
            if (Objects.equals(status.code, code)) {
                return true;
            }
        }
        return false;
    }

    /** 是否离职状态 */
    public static boolean isResigned(Integer code) {
        return Objects.equals(RESIGNED.code, code);
    }

    /** 状态描述，未知编码返回兜底文案 */
    public static String descOf(Integer code) {
        for (EmployeeStatus status : values()) {
            if (Objects.equals(status.code, code)) {
                return status.desc;
            }
        }
        return "未知状态";
    }
}
