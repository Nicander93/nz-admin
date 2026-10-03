package com.nz.admin.framework.tenant.core;

import java.util.Set;

/** 业务模块用此端口注册隔离表，无需改应用的中央配置。 */
@FunctionalInterface
public interface TenantTableRuleCustomizer {
    void customize(Set<String> tables);
}
