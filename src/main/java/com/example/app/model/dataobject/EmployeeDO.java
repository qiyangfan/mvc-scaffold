package com.example.app.model.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工 DO：与 t_employee 表结构一一对应，只在本工程内 Service/Manager/Mapper 之间流转，禁止透传到 Controller。
 * <p>主键为 UUID v7 字符串（应用侧生成，不再依赖自增回填），状态为编码字符串。</p>
 */
@Data
public class EmployeeDO {

    /** 主键：UUID v7 字符串 */
    private String id;
    /** 工号，由主键派生，插入前即确定 */
    private String empNo;
    /** 姓名 */
    private String name;
    /** 部门 ID（部门数据在部门服务，不在本库） */
    private Long deptId;
    /** 月薪 */
    private BigDecimal salary;
    /** 状态编码：TRIAL / REGULAR / RESIGNED，库表 CHECK 约束同一份清单 */
    private String status;
    /** 入职日期 */
    private LocalDate hireDate;
    /** 创建时间 */
    private LocalDateTime gmtCreate;
    /** 修改时间 */
    private LocalDateTime gmtModified;
    /** 逻辑删除：0 未删除 1 已删除 */
    private Integer deleted;
}
