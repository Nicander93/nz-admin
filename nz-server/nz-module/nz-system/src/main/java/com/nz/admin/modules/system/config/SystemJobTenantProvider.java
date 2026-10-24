package com.nz.admin.modules.system.config;

import com.nz.admin.common.job.JobTenantProvider;
import com.nz.admin.modules.system.mapper.tenant.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** 租户有效性规则留在 system，任务模块通过公共协议读取。 */
@Component
@RequiredArgsConstructor
public class SystemJobTenantProvider implements JobTenantProvider {
    private final TenantMapper tenantMapper;

    @Override
    public boolean isEnabledTenant(Long tenantId) {
        if (tenantId == null) return false;
        var tenant = tenantMapper.selectById(tenantId);
        return tenant != null && Integer.valueOf(0).equals(tenant.getStatus())
                && (tenant.getExpireTime() == null || tenant.getExpireTime().isAfter(LocalDateTime.now()));
    }

    @Override
    public List<Long> enabledTenantIds() {
        var now = LocalDateTime.now();
        return tenantMapper.selectList(null).stream()
                .filter(tenant -> Integer.valueOf(0).equals(tenant.getStatus()))
                .filter(tenant -> tenant.getExpireTime() == null || tenant.getExpireTime().isAfter(now))
                .map(tenant -> tenant.getId()).toList();
    }
}
