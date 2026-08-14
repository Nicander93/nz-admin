package com.nz.admin.modules.system.service.tenant;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.tenant.config.TenantProperties;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 跨租户管理入口保护测试。
 */
class TenantAdminGuardTest {

    private final TenantProperties properties = new TenantProperties();
    private final TenantAdminGuard guard = new TenantAdminGuard(properties);

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void allowsDefaultTenant() {
        TenantContextHolder.setTenantId(1L);

        assertThatCode(guard::requireDefaultTenant).doesNotThrowAnyException();
    }

    @Test
    void rejectsBusinessTenant() {
        TenantContextHolder.setTenantId(8L);

        assertThrows(BusinessException.class, guard::requireDefaultTenant);
    }
}
