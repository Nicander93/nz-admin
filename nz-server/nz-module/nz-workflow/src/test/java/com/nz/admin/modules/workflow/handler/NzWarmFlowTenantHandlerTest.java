package com.nz.admin.modules.workflow.handler;

import com.nz.admin.framework.tenant.config.TenantProperties;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NzWarmFlowTenantHandlerTest {

    private final TenantProperties tenantProperties = new TenantProperties();
    private final NzWarmFlowTenantHandler handler = new NzWarmFlowTenantHandler(tenantProperties);

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void usesConfiguredDefaultTenantOutsideRequestContext() {
        tenantProperties.setDefaultTenantId(3L);

        assertThat(handler.getTenantId()).isEqualTo("3");
    }

    @Test
    void usesCurrentTenantWhenRequestContextExists() {
        String tenantId = TenantContextHolder.callWithTenantId(9L, handler::getTenantId);

        assertThat(tenantId).isEqualTo("9");
    }
}
