package com.nz.admin.framework.auth.datascope;

/**
 * 数据权限查询参数承载接口。
 */
public interface DataScopeParam {

    /**
     * 写入数据权限 SQL 条件。
     */
    void setDataScopeSql(String dataScopeSql);
}
