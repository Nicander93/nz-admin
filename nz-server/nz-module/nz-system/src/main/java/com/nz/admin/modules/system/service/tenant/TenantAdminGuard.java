package com.nz.admin.modules.system.service.tenant;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.tenant.config.TenantProperties;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import org.springframework.stereotype.Component;

/**
 * 保护跨租户管理入口，只允许默认租户操作。
 */
@Component
public class TenantAdminGuard {

    private final TenantProperties properties;

    public TenantAdminGuard(TenantProperties properties) {
        this.properties = properties;
    }

    public void requireDefaultTenant() {
        Long currentTenantId = TenantContextHolder.getTenantIdOrNull();
        if (currentTenantId == null || !properties.getDefaultTenantId().equals(currentTenantId)) {
            throw new BusinessException("仅默认租户可管理租户与套餐");
        }
    }
}
