package com.nz.admin.common;

import lombok.Data;

@Data
public class R<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMsg("success");
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = ok();
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(String msg) {
        return fail(500, msg);
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
