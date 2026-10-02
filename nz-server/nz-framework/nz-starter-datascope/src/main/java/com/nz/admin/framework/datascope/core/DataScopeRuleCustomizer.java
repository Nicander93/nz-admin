package com.nz.admin.framework.datascope.core;

import java.util.List;

/** 各业务模块注册受保护表的归属字段。 */
@FunctionalInterface
public interface DataScopeRuleCustomizer {
    void customize(List<DataScopeRule> rules);
}
