package com.example.app.model.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工 DO：与 t_employee 表结构一一对应，只在本工程内 Service/Manager/Mapper 之间流转，禁止透传到 Controller。
 */
@Data
public class EmployeeDO {

    /** 主键 */
    private Long id;
    /** 工号，插入后回填生成 */
    private String empNo;
    /** 姓名 */
    private String name;
    /** 部门 ID（部门数据在部门服务，不在本库） */
    private Long deptId;
    /** 月薪 */
    private BigDecimal salary;
    /** 状态：0 试用 1 正式 2 离职，语义见 {@link com.example.app.common.enums.EmployeeStatus} */
    private Integer status;
    /** 入职日期 */
    private LocalDate hireDate;
    /** 创建时间 */
    private LocalDateTime gmtCreate;
    /** 修改时间 */
    private LocalDateTime gmtModified;
    /** 逻辑删除：0 未删除 1 已删除 */
    private Integer deleted;
}
