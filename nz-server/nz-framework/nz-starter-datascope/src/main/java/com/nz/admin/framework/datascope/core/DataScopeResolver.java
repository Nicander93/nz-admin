package com.nz.admin.framework.datascope.core;

/** 由业务模块解析当前身份的角色和部门范围。 */
public interface DataScopeResolver {
    DataScopeResult resolve();
}
