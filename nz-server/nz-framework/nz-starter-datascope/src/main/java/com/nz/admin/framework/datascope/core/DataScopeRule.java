package com.nz.admin.framework.datascope.core;

/** 字段来自服务端配置，不接受请求参数或 SQL 表达式。 */
public record DataScopeRule(String table, String deptColumn, String userColumn) {
    public DataScopeRule {
        validate(table);
        if (deptColumn != null) {
            validate(deptColumn);
        }
        if (userColumn != null) {
            validate(userColumn);
        }
        if (deptColumn == null && userColumn == null) {
            throw new IllegalArgumentException("数据归属字段不能为空");
        }
    }

    private static void validate(String value) {
        if (value == null || !value.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
            throw new IllegalArgumentException("数据权限标识符不合法");
        }
    }
}
