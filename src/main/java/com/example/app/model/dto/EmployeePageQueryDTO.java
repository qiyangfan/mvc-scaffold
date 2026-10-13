package com.example.app.model.dto;

import com.example.app.common.api.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 员工分页查询对象：Query 是手册领域模型规约中独立的查询请求对象，各层通用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "员工分页查询入参")
public class EmployeePageQueryDTO extends PageQuery {

    @Schema(description = "姓名，模糊匹配", example = "Zhang")
    private String name;

    @Schema(description = "部门 ID，精确匹配", example = "1")
    private Long deptId;

    @Schema(description = "状态：0 试用 1 正式 2 离职", example = "1")
    private Integer status;
}
