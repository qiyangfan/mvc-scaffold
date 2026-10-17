package com.example.app.common.exception;

import com.example.app.common.api.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * 全局异常处理：兜底所有未捕获异常，保证响应体结构统一。
 * <p>约定：业务异常返回 HTTP 200 + 业务码（国内后台前端惯例）；参数错误 400；未知错误 500。
 * 未知异常只透出通用文案，完整堆栈仅进日志。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** @RequestBody 上的 @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .orElse(ErrorCode.PARAM_INVALID.getMessage());
        log.warn("parameter validation failed: {}", message);
        return R.fail(ErrorCode.PARAM_INVALID.getCode(), message);
    }

    /** @PathVariable / @RequestParam 上的约束校验失败（Spring 6.1+ 方法级校验） */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        log.warn("handler method validation failed: {}", e.getMessage());
        return R.fail(ErrorCode.PARAM_INVALID.getCode(), ErrorCode.PARAM_INVALID.getMessage());
    }

    /** 方法参数级 @Validated 约束校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("constraint violation: {}", e.getMessage());
        return R.fail(ErrorCode.PARAM_INVALID.getCode(), ErrorCode.PARAM_INVALID.getMessage());
    }

    /** 请求体不可读（JSON 格式错误等） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("request body not readable: {}", e.getMessage());
        return R.fail(ErrorCode.PARAM_INVALID.getCode(), ErrorCode.PARAM_INVALID.getMessage());
    }

    /**
     * 入参类型不匹配：路径变量/查询参数与目标类型对不上（主键不是合法 UUID、状态枚举取值非法等）。
     * <p>Spring MVC 抛的是 {@link org.springframework.web.method.annotation.MethodArgumentTypeMismatchException}，
     * 它继承自 {@link TypeMismatchException}（beans 包）而非 ConversionException。没有这个处理器时，
     * 类型不匹配会落进未知异常兜底返回 500——把客户端的输入错误暴露成服务端故障。</p>
     */
    @ExceptionHandler(TypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleTypeMismatch(TypeMismatchException e) {
        log.warn("argument type mismatch: {}", e.getMessage());
        return R.fail(ErrorCode.PARAM_INVALID.getCode(), ErrorCode.PARAM_INVALID.getMessage());
    }

    /** 业务异常：正常业务流内的规则拒绝，不打 error 级别日志 */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleBizException(BizException e) {
        log.info("business exception, code:{}, message:{}", e.getErrorCode().getCode(), e.getMessage());
        return R.fail(e.getErrorCode().getCode(), e.getMessage());
    }

    /** 未知异常：兜底，堆栈只进日志，响应只透出通用文案 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleUnknown(Exception e) {
        log.error("unexpected error", e);
        return R.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }
}
