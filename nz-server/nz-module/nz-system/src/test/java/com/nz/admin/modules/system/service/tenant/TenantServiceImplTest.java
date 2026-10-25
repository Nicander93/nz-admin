package com.nz.admin.modules.system.service.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.tenant.config.TenantProperties;
import com.nz.admin.framework.test.core.ut.BaseMockitoUnitTest;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantPackageDO;
import com.nz.admin.modules.system.entity.dto.tenant.TenantCreateRequest;
import com.nz.admin.modules.system.entity.dto.tenant.TenantUpdateRequest;
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
import static org.mockito.Mockito.never;

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
        when(tenantMapper.selectOne(any())).thenReturn(tenant);
        when(tenantProperties.getDefaultTenantId()).thenReturn(1L);

        assertThrows(BusinessException.class, () -> tenantService.deactivate(1L));
    }

    @Test
    void tenantAuthVersionIsNotPartOfJson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        TenantDO tenant = objectMapper.readValue("{\"authVersion\":43}", TenantDO.class);

        assertThat(tenant.getAuthVersion()).isNull();
        assertThat(objectMapper.writeValueAsString(new TenantDO().setAuthVersion(43L))).doesNotContain("authVersion");
    }

    @Test
    void updateShouldIncrementVersionForSecurityChanges() {
        TenantDO existing = new TenantDO()
                .setId(9L)
                .setTenantCode("acme")
                .setTenantName("Acme")
                .setPackageId(2L)
                .setExpireTime(LocalDateTime.now().plusDays(1))
                .setAccountCount(20)
                .setStatus(0);
        when(tenantMapper.selectOne(any())).thenReturn(existing);
        when(tenantMapper.selectCount(any())).thenReturn(0L);
        when(tenantPackageService.getRequired(2L)).thenReturn(new TenantPackageDO().setId(2L));
        when(tenantProperties.getDefaultTenantId()).thenReturn(1L);
        when(tenantMapper.update(any(TenantDO.class), any())).thenReturn(1);

        tenantService.update(updateRequest(9L, 1, existing.getExpireTime(), 2L, "Changed"));

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TenantDO>> wrapper = ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(tenantMapper).update(any(TenantDO.class), wrapper.capture());
        assertThat(wrapper.getValue().getSqlSet()).contains("auth_version = auth_version + 1");
    }

    @Test
    void updateShouldNotIncrementVersionForOrdinaryChanges() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(1);
        TenantDO existing = new TenantDO()
                .setId(9L)
                .setTenantCode("acme")
                .setTenantName("Acme")
                .setPackageId(2L)
                .setExpireTime(expiry)
                .setAccountCount(20)
                .setStatus(0);
        when(tenantMapper.selectOne(any())).thenReturn(existing);
        when(tenantMapper.selectCount(any())).thenReturn(0L);
        when(tenantPackageService.getRequired(2L)).thenReturn(new TenantPackageDO().setId(2L));
        when(tenantProperties.getDefaultTenantId()).thenReturn(1L);
        when(tenantMapper.update(any(TenantDO.class), any())).thenReturn(1);

        tenantService.update(updateRequest(9L, 0, expiry, 2L, "Changed"));

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TenantDO>> wrapper = ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(tenantMapper).update(any(TenantDO.class), wrapper.capture());
        assertThat(wrapper.getValue().getSqlSet()).doesNotContain("auth_version");
    }

    @Test
    void updateMustNotDisableDefaultTenant() {
        TenantDO existing = new TenantDO()
                .setId(1L)
                .setTenantCode("default")
                .setPackageId(2L)
                .setStatus(0);
        when(tenantMapper.selectOne(any())).thenReturn(existing);
        when(tenantProperties.getDefaultTenantId()).thenReturn(1L);

        assertThrows(BusinessException.class, () -> tenantService.update(
                updateRequest(1L, 1, null, 2L, "Default")));
        verify(tenantMapper, never()).update(any(TenantDO.class), any());
    }

    private TenantUpdateRequest updateRequest(Long id, Integer status, LocalDateTime expireTime, Long packageId, String name) {
        TenantUpdateRequest request = new TenantUpdateRequest();
        request.setId(id);
        request.setTenantCode(id == 1L ? "default" : "acme");
        request.setTenantName(name);
        request.setPackageId(packageId);
        request.setExpireTime(expireTime);
        request.setAccountCount(20);
        request.setStatus(status);
        return request;
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
