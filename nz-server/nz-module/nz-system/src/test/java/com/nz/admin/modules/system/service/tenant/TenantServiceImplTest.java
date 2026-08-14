package com.nz.admin.modules.system.service.tenant;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.tenant.config.TenantProperties;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantPackageDO;
import com.nz.admin.modules.system.entity.dto.tenant.TenantCreateRequest;
import com.nz.admin.modules.system.mapper.tenant.TenantMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 租户服务测试。
 */
class TenantServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TenantServiceImpl tenantService;
    @Mock
    private TenantMapper tenantMapper;
    @Mock
    private TenantPackageService tenantPackageService;
    @Mock
    private TenantProvisioningService provisioningService;
    @Mock
    private TenantProperties tenantProperties;

    @Test
    void validateLoginTenantShouldRejectExpiredTenant() {
        TenantDO tenant = new TenantDO();
        tenant.setStatus(0);
        tenant.setExpireTime(LocalDateTime.now().minusMinutes(1));
        when(tenantMapper.selectByCode("expired")).thenReturn(tenant);

        assertThrows(BusinessException.class, () -> tenantService.validateLoginTenant("expired"));
    }

    @Test
    void createShouldPersistTenantBeforeProvisioning() {
        TenantCreateRequest request = createRequest();
        when(tenantMapper.selectCount(any())).thenReturn(0L);
        when(tenantPackageService.getRequired(2L)).thenReturn(new TenantPackageDO().setId(2L));
        doAnswer(invocation -> {
            TenantDO tenant = invocation.getArgument(0);
            tenant.setId(9L);
            return 1;
        }).when(tenantMapper).insert(any(TenantDO.class));

        Long tenantId = tenantService.create(request);

        assertThat(tenantId).isEqualTo(9L);
        ArgumentCaptor<TenantDO> tenantCaptor = ArgumentCaptor.forClass(TenantDO.class);
        verify(provisioningService).provision(tenantCaptor.capture(), org.mockito.ArgumentMatchers.same(request));
        assertThat(tenantCaptor.getValue().getTenantCode()).isEqualTo("acme");
        assertThat(tenantCaptor.getValue().getPackageId()).isEqualTo(2L);
    }

    @Test
    void deactivateShouldProtectDefaultTenant() {
        TenantDO tenant = new TenantDO();
        tenant.setId(1L);
        when(tenantMapper.selectById(1L)).thenReturn(tenant);
        when(tenantProperties.getDefaultTenantId()).thenReturn(1L);

        assertThrows(BusinessException.class, () -> tenantService.deactivate(1L));
    }

    private TenantCreateRequest createRequest() {
        TenantCreateRequest request = new TenantCreateRequest();
        request.setTenantCode("acme");
        request.setTenantName("Acme");
        request.setPackageId(2L);
        request.setAccountCount(20);
        request.setStatus(0);
        request.setAdminUsername("admin");
        request.setAdminPassword("secret123");
        return request;
    }
}
