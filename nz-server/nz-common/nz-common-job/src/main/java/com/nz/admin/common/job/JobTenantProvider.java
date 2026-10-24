package com.nz.admin.common.job;

import java.util.List;

/** 由租户业务提供当前启用且未过期的租户，任务模块不直接查询租户表。 */
public interface JobTenantProvider {
    List<Long> enabledTenantIds();

    /** 任务触发时再次校验，不能只相信启动时的租户状态。 */
    default boolean isEnabledTenant(Long tenantId) {
        return tenantId != null && enabledTenantIds().contains(tenantId);
    }
}
