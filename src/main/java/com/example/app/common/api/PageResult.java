package com.example.app.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 统一分页响应体：total + list 二段式，前端无需解析分页插件结构。
 *
 * @param <T> 行数据类型
 */
@Data
@Schema(description = "统一分页响应体")
public class PageResult<T> {

    @Schema(description = "总条数", example = "100")
    private Long total;

    @Schema(description = "当前页数据")
    private List<T> list;

    public static <T> PageResult<T> of(List<T> list, Long total) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.total = total;
        // 空集合不返回 null
        pageResult.list = list == null ? Collections.emptyList() : list;
        return pageResult;
    }
}
