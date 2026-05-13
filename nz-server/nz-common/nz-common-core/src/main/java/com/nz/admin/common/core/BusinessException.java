package com.nz.admin.common.core;

/**
 * 业务异常。
 */
public class BusinessException extends RuntimeException {

    private final int code;
    private final String message;

    public BusinessException(String message) {
        this(CommonErrorCode.BUSINESS_ERROR.getCode(), message);
    }

    public BusinessException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
