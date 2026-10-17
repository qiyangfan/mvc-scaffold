package com.example.app.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页查询基类：所有分页入参继承本类。
 * <p>pageSize 设上限，防止恶意大分页拖垮数据库（多实例部署下的稳定性底线）。</p>
 */
@Data
@Schema(description = "分页入参基类")
public class PageQuery {

    /**
     * 每页条数上限：<b>单一事实源</b>。
     * <p>历史上门禁只写在 api 校验注解上，PageHelper 侧没有任何兜底，一旦有代码绕过注解直接查库
     * 就会失去防护。收敛成常量后，校验注解与后续任何兜底钳制都引用同一份值。</p>
     */
    public static final int MAX_PAGE_SIZE = 200;

    @Schema(description = "页码，从 1 开始", example = "1")
    @Min(value = 1, message = "pageNum must be greater than or equal to 1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，上限 200", example = "10")
    @Min(value = 1, message = "pageSize must be greater than or equal to 1")
    @Max(value = MAX_PAGE_SIZE, message = "pageSize must be less than or equal to 200")
    private Integer pageSize = 10;
}
