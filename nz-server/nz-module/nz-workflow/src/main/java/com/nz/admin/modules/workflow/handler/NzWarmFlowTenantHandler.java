package com.nz.admin.modules.workflow.handler;

import com.nz.admin.framework.tenant.config.TenantProperties;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import org.dromara.warm.flow.core.handler.TenantHandler;
import org.springframework.stereotype.Component;

/**
 * 将 Warm-Flow 的租户读取接入项目租户上下文。
 */
@Component
public class NzWarmFlowTenantHandler implements TenantHandler {

    private final TenantProperties tenantProperties;

    public NzWarmFlowTenantHandler(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    @Override
    public String getTenantId() {
        Long tenantId = TenantContextHolder.getTenantIdOrNull();
        if (tenantId == null) {
            tenantId = tenantProperties.getDefaultTenantId();
        }
        return tenantId == null ? null : tenantId.toString();
    }
}
