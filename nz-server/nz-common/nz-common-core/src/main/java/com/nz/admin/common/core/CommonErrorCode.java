package com.nz.admin.common.core;

/**
 * 通用错误码。
 */
public enum CommonErrorCode implements ErrorCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    VALIDATION_ERROR(400, "请求参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有访问权限"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    REPEAT_SUBMIT(429, "请勿重复提交"),
    BUSINESS_ERROR(500, "业务处理失败"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;

    CommonErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
