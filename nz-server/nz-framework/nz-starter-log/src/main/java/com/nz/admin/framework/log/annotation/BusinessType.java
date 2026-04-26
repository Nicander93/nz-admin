package com.nz.admin.framework.log.annotation;

/**
 * 操作类型。
 */
public enum BusinessType {

    QUERY(0, "查询"),
    INSERT(1, "新增"),
    UPDATE(2, "修改"),
    DELETE(3, "删除");

    private final int code;
    private final String desc;

    BusinessType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
