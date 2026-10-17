package com.example.app.model.dto;

import com.example.app.common.enums.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建员工入参：校验 message 一律英文（国际化）。
 */
@Data
@Schema(description = "创建员工入参")
public class EmployeeCreateDTO {

    @Schema(description = "姓名", example = "Zhang San")
    @NotBlank(message = "must not be blank")
    @Size(max = 64, message = "length must be less than or equal to 64")
    private String name;

    @Schema(description = "部门 ID", example = "1")
    @NotNull(message = "must not be null")
    private Long deptId;

    @Schema(description = "月薪", example = "20000.00")
    @NotNull(message = "must not be null")
    @DecimalMin(value = "0.00", message = "must be greater than or equal to 0")
    @Digits(integer = 10, fraction = 2, message = "at most 10 integer digits and 2 fraction digits")
    private BigDecimal salary;

    @Schema(description = "状态编码，取值见 EmployeeStatus", example = "TRIAL",
            allowableValues = {"TRIAL", "REGULAR", "RESIGNED"})
    @NotNull(message = "must not be null")
    private EmployeeStatus status;

    @Schema(description = "入职日期", example = "2026-09-01")
    @NotNull(message = "must not be null")
    @PastOrPresent(message = "must not be in the future")
    private LocalDate hireDate;
}
