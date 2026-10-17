package com.example.app.common.enums;

/**
 * 员工状态枚举：以枚举替代魔法值，业务代码禁止出现 status == 1 之类的裸判断。
 * <p>持久化值为状态名字符串（{@code status VARCHAR + CHECK}），与数据库 CHECK 约束共用同一份清单——
 * 不再用 TINYINT 数字码，避免"1=正常 0=禁用"这类只能靠注释解读的脏数据。</p>
 * <p>入参直接绑定该枚举：非法取值在 Controller 层就被 Spring 转成 400，不会带着脏值进 Service。</p>
 */
public enum EmployeeStatus {

    /** 试用期 */
    TRIAL("TRIAL", "试用期"),
    /** 正式 */
    REGULAR("REGULAR", "正式"),
    /** 离职 */
    RESIGNED("RESIGNED", "离职");

    private final String code;
    private final String desc;

    EmployeeStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 持久化编码：与 {@link #name()} 一致，数据库 CHECK 约束以此为准 */
    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 是否离职状态 */
    public boolean isResigned() {
        return this == RESIGNED;
    }

    /** 状态描述，未知编码返回兜底文案 */
    public static String descOf(String code) {
        for (EmployeeStatus status : values()) {
            if (status.code.equals(code)) {
                return status.desc;
            }
        }
        return "未知状态";
    }
}
