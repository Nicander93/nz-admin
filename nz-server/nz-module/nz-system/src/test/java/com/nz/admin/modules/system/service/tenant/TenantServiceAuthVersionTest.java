package com.nz.admin.modules.system.service.tenant;

import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.tenant.config.TenantProperties;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantPackageDO;
import com.nz.admin.modules.system.entity.dto.tenant.TenantUpdateRequest;
import com.nz.admin.modules.system.mapper.tenant.TenantMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Import(NzSystemTestApplication.class)
class TenantServiceAuthVersionTest extends BaseDbUnitTest {

    @Resource
    private TenantServiceImpl tenantService;
    @Resource
    private TenantMapper tenantMapper;
    @MockBean
    private TenantPackageService tenantPackageService;
    @MockBean
    private TenantProvisioningService provisioningService;
    @MockBean
    private TenantProperties tenantProperties;

    @Test
    void updateSecurityChangeIncrementsVersion() {
        TenantDO tenant = insertTenant("versioned", 2L);
        when(tenantPackageService.getRequired(2L)).thenReturn(new TenantPackageDO().setId(2L));
        when(tenantProperties.getDefaultTenantId()).thenReturn(-1L);

        tenantService.update(updateRequest(tenant, 1, 2L));

        TenantDO updated = tenantMapper.selectById(tenant.getId());
        assertEquals(1L, updated.getAuthVersion());
        assertEquals(1, updated.getStatus());
    }

    @Test
    void updateOrdinaryChangeKeepsVersion() {
        TenantDO tenant = insertTenant("ordinary", 2L);
        when(tenantPackageService.getRequired(2L)).thenReturn(new TenantPackageDO().setId(2L));
        when(tenantProperties.getDefaultTenantId()).thenReturn(-1L);
        TenantUpdateRequest request = updateRequest(tenant, 0, 2L);
        request.setTenantName("Renamed tenant");

        tenantService.update(request);

        TenantDO updated = tenantMapper.selectById(tenant.getId());
        assertEquals(0L, updated.getAuthVersion());
        assertEquals("Renamed tenant", updated.getTenantName());
    }

    @Test
    void failedPackageProvisioningRollsBackTenantVersionAndData() {
        TenantDO tenant = insertTenant("rollback", 2L);
        when(tenantPackageService.getRequired(3L)).thenReturn(new TenantPackageDO().setId(3L));
        when(tenantProperties.getDefaultTenantId()).thenReturn(-1L);
        doThrow(new IllegalStateException("provisioning failed"))
                .when(provisioningService).applyPackageMenus(tenant.getId(), 3L);

        assertThrows(IllegalStateException.class, () -> tenantService.update(updateRequest(tenant, 0, 3L)));

        TenantDO unchanged = tenantMapper.selectById(tenant.getId());
        assertEquals(0L, unchanged.getAuthVersion());
        assertEquals(2L, unchanged.getPackageId());
    }

    @Test
    void deactivateIncrementsVersion() {
        TenantDO tenant = insertTenant("deactivate", 2L);
        when(tenantProperties.getDefaultTenantId()).thenReturn(-1L);

        tenantService.deactivate(tenant.getId());

        TenantDO updated = tenantMapper.selectById(tenant.getId());
        assertEquals(1L, updated.getAuthVersion());
        assertEquals(1, updated.getStatus());
    }

    @Test
    void updateCannotDisableDefaultTenant() {
        TenantDO tenant = insertTenant("default", 2L);
        when(tenantProperties.getDefaultTenantId()).thenReturn(tenant.getId());

        assertThrows(BusinessException.class, () -> tenantService.update(updateRequest(tenant, 1, 2L)));
        assertEquals(0L, tenantMapper.selectById(tenant.getId()).getAuthVersion());
    }

    private TenantDO insertTenant(String code, Long packageId) {
        TenantDO tenant = new TenantDO()
                .setTenantCode(code)
                .setTenantName(code)
                .setPackageId(packageId)
                .setAccountCount(10)
                .setStatus(0);
        tenantMapper.insert(tenant);
        return tenant;
    }

    private TenantUpdateRequest updateRequest(TenantDO tenant, Integer status, Long packageId) {
        TenantUpdateRequest request = new TenantUpdateRequest();
        request.setId(tenant.getId());
        request.setTenantCode(tenant.getTenantCode());
        request.setTenantName(tenant.getTenantName());
        request.setPackageId(packageId);
        request.setExpireTime(tenant.getExpireTime());
        request.setAccountCount(tenant.getAccountCount());
        request.setStatus(status);
        request.setRemark(tenant.getRemark());
        return request;
    }
}
