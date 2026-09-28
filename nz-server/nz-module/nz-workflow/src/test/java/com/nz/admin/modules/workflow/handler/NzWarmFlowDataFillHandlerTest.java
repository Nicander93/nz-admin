package com.nz.admin.modules.workflow.handler;

import org.dromara.warm.flow.core.handler.TenantHandler;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NzWarmFlowDataFillHandlerTest {

    private final TenantHandler tenantHandler = mock(TenantHandler.class);
    private final NzWarmFlowDataFillHandler handler = new NzWarmFlowDataFillHandler(tenantHandler);

    @Test
    void fillsTenantAndAuditTimeForNewEntity() {
        when(tenantHandler.getTenantId()).thenReturn("9");
        FlowDefinition definition = new FlowDefinition();

        handler.insertFill(definition);

        assertThat(definition.getTenantId()).isEqualTo("9");
        assertThat(definition.getCreateTime()).isNotNull();
        assertThat(definition.getUpdateTime()).isNotNull();
    }

    @Test
    void preservesExplicitTenant() {
        when(tenantHandler.getTenantId()).thenReturn("9");
        FlowDefinition definition = new FlowDefinition().setTenantId("12");

        handler.insertFill(definition);

        assertThat(definition.getTenantId()).isEqualTo("12");
    }
}
