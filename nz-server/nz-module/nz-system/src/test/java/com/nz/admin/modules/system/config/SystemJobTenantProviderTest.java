package com.nz.admin.modules.system.config;

import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.mapper.tenant.TenantMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemJobTenantProviderTest {
    @Mock TenantMapper tenantMapper;
    @InjectMocks SystemJobTenantProvider provider;

    @Test
    void onlyReturnsEnabledAndUnexpiredTenants() {
        var enabled = new TenantDO().setId(1L).setStatus(0);
        var noExpiry = new TenantDO().setId(2L).setStatus(0);
        var expired = new TenantDO().setId(3L).setStatus(0).setExpireTime(LocalDateTime.now().minusSeconds(1));
        var disabled = new TenantDO().setId(4L).setStatus(1);
        var futureExpiry = new TenantDO().setId(5L).setStatus(0).setExpireTime(LocalDateTime.now().plusDays(1));
        when(tenantMapper.selectList(isNull())).thenReturn(List.of(enabled, noExpiry, expired, disabled, futureExpiry));
        assertThat(provider.enabledTenantIds()).containsExactly(1L, 2L, 5L);
    }

    @Test
    void runtimeLookupRejectsMissingDisabledAndExpiredTenants() {
        when(tenantMapper.selectById(3L)).thenReturn(new TenantDO().setId(3L).setStatus(0).setExpireTime(LocalDateTime.now().minusSeconds(1)));
        when(tenantMapper.selectById(4L)).thenReturn(new TenantDO().setId(4L).setStatus(1));
        when(tenantMapper.selectById(5L)).thenReturn(new TenantDO().setId(5L).setStatus(0));
        when(tenantMapper.selectById(6L)).thenReturn(null);
        assertThat(provider.isEnabledTenant(null)).isFalse();
        assertThat(provider.isEnabledTenant(3L)).isFalse();
        assertThat(provider.isEnabledTenant(4L)).isFalse();
        assertThat(provider.isEnabledTenant(5L)).isTrue();
        assertThat(provider.isEnabledTenant(6L)).isFalse();
    }
}
