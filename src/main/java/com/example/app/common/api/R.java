package com.example.app.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 统一响应体。
 * <p>code = "0" 表示成功；非 0 响应码参见 {@link com.example.app.common.exception.ErrorCode}，
 * 遵循手册错误码分级：A 用户端 / B 系统执行侧 / C 第三方服务侧。</p>
 *
 * @param <T> 业务数据类型
 */
@Data
@Schema(description = "统一响应体")
public class R<T> {

    /** 成功响应码 */
    public static final String SUCCESS_CODE = "0";

    @Schema(description = "响应码，0 表示成功", example = "0")
    private String code;

    @Schema(description = "响应信息（英文）", example = "success")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = SUCCESS_CODE;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static R<Void> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(String code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
