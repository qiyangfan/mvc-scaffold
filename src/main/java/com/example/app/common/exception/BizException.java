package com.example.app.common.exception;

import lombok.Getter;

/**
 * 业务异常：业务规则不满足时抛出，由全局异常处理器统一转换为 {@link com.example.app.common.api.R}。
 */
@Getter
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
