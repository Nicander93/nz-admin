package com.nz.admin.framework.tenant.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextHolderTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void restoresPreviousTenantAfterNestedCall() {
        TenantContextHolder.setTenantId(1L);

        Long resolved = TenantContextHolder.callWithTenantId(9L, TenantContextHolder::getTenantIdOrNull);

        assertThat(resolved).isEqualTo(9L);
        assertThat(TenantContextHolder.getTenantIdOrNull()).isEqualTo(1L);
    }

    @Test
    void clearsContextWhenActionFails() {
        assertThatThrownBy(() -> TenantContextHolder.runWithTenantId(9L, () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(TenantContextHolder.getTenantIdOrNull()).isNull();
    }
}
