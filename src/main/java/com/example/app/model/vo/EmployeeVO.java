package com.example.app.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工出参 VO：仅供 Controller 层组装返回，字段与前端展示对齐（含部门名、状态描述等冗余展示字段）。
 */
@Data
@Schema(description = "员工详情")
public class EmployeeVO {

    @Schema(description = "员工 ID", example = "1")
    private Long id;

    @Schema(description = "工号", example = "EMP000001")
    private String empNo;

    @Schema(description = "姓名", example = "Zhang San")
    private String name;

    @Schema(description = "部门 ID", example = "1")
    private Long deptId;

    @Schema(description = "部门名称（来自部门服务防腐层）", example = "平台研发部")
    private String deptName;

    @Schema(description = "月薪", example = "20000.00")
    private BigDecimal salary;

    @Schema(description = "状态码", example = "1")
    private Integer status;

    @Schema(description = "状态描述", example = "正式")
    private String statusDesc;

    @Schema(description = "入职日期", example = "2026-09-01")
    private LocalDate hireDate;

    @Schema(description = "创建时间", example = "2026-09-21 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gmtCreate;
}
