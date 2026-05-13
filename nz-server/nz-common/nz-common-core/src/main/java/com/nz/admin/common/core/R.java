package com.nz.admin.common.core;

import lombok.Data;

@Data
public class R<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.setCode(CommonErrorCode.SUCCESS.getCode());
        r.setMsg(CommonErrorCode.SUCCESS.getMessage());
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = ok();
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(String msg) {
        return fail(CommonErrorCode.BUSINESS_ERROR.getCode(), msg);
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> R<T> fail(BusinessException exception) {
        return fail(exception.getCode(), exception.getMessage());
    }
}
