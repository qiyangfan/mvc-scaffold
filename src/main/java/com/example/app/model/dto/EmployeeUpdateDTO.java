package com.example.app.model.dto;

import com.example.app.common.enums.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 更新员工入参：除 id 外均为可选字段，做选择性更新；至少要有一个业务字段非空，由 Service 校验。
 */
@Data
@Schema(description = "更新员工入参")
public class EmployeeUpdateDTO {

    @Schema(description = "员工 ID：UUID v7 字符串", example = "019b1f2c-7a3e-7c4b-8f1d-2e6a9b0c1d2e")
    @NotNull(message = "must not be null")
    private String id;

    @Schema(description = "姓名", example = "Zhang San")
    @Size(max = 64, message = "length must be less than or equal to 64")
    private String name;

    @Schema(description = "月薪", example = "25000.00")
    @DecimalMin(value = "0.00", message = "must be greater than or equal to 0")
    @Digits(integer = 10, fraction = 2, message = "at most 10 integer digits and 2 fraction digits")
    private BigDecimal salary;

    @Schema(description = "状态编码，取值见 EmployeeStatus", example = "REGULAR",
            allowableValues = {"TRIAL", "REGULAR", "RESIGNED"})
    private EmployeeStatus status;

    @Schema(description = "入职日期", example = "2026-09-01")
    @PastOrPresent(message = "must not be in the future")
    private LocalDate hireDate;
}
