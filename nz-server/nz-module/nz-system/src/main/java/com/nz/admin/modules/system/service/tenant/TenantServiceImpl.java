package com.nz.admin.modules.system.service.tenant;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.tenant.config.TenantProperties;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dto.tenant.TenantCreateRequest;
import com.nz.admin.modules.system.entity.dto.tenant.TenantUpdateRequest;
import com.nz.admin.modules.system.mapper.tenant.TenantMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 租户管理服务。
 */
@Service
public class TenantServiceImpl implements TenantService {

    private final TenantMapper tenantMapper;
    private final TenantPackageService tenantPackageService;
    private final TenantProvisioningService provisioningService;
    private final TenantProperties tenantProperties;

    public TenantServiceImpl(TenantMapper tenantMapper,
                             TenantPackageService tenantPackageService,
                             TenantProvisioningService provisioningService,
                             TenantProperties tenantProperties) {
        this.tenantMapper = tenantMapper;
        this.tenantPackageService = tenantPackageService;
        this.provisioningService = provisioningService;
        this.tenantProperties = tenantProperties;
    }

    @Override
    public Page<TenantDO> page(Integer pageNum, Integer pageSize, String tenantCode, String tenantName, Integer status) {
        return tenantMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<TenantDO>()
                        .like(StrUtil.isNotBlank(tenantCode), TenantDO::getTenantCode, tenantCode)
                        .like(StrUtil.isNotBlank(tenantName), TenantDO::getTenantName, tenantName)
                        .eq(status != null, TenantDO::getStatus, status)
                        .orderByAsc(TenantDO::getId));
    }

    @Override
    public TenantDO getRequired(Long id) {
        TenantDO tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }
        return tenant;
    }

    @Override
    public TenantDO getByCode(String tenantCode) {
        return tenantMapper.selectByCode(tenantCode);
    }

    @Override
    public TenantDO validateLoginTenant(String tenantCode) {
        TenantDO tenant = getByCode(tenantCode);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }
        if (tenant.getStatus() != null && tenant.getStatus() != 0) {
            throw new BusinessException("租户已停用");
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("租户已过期");
        }
        return tenant;
    }

    @Override
    @Transactional
    public Long create(TenantCreateRequest request) {
        ensureCodeUnique(request.getTenantCode(), null);
        tenantPackageService.getRequired(request.getPackageId());

        TenantDO tenant = new TenantDO();
        copyCreate(request, tenant);
        tenantMapper.insert(tenant);
        provisioningService.provision(tenant, request);
        return tenant.getId();
    }

    @Override
    @Transactional
    public void update(TenantUpdateRequest request) {
        TenantDO existing = getRequired(request.getId());
        if (tenantProperties.getDefaultTenantId().equals(request.getId())
                && !"default".equals(request.getTenantCode())) {
            throw new BusinessException("默认租户编码不能修改");
        }
        ensureCodeUnique(request.getTenantCode(), request.getId());
        tenantPackageService.getRequired(request.getPackageId());

        TenantDO tenant = new TenantDO();
        copyUpdate(request, tenant);
        tenantMapper.updateById(tenant);
        if (!request.getPackageId().equals(existing.getPackageId())) {
            provisioningService.applyPackageMenus(request.getId(), request.getPackageId());
        }
    }

    @Override
    public void deactivate(Long id) {
        getRequired(id);
        if (tenantProperties.getDefaultTenantId().equals(id)) {
            throw new BusinessException("默认租户不能停用");
        }
        TenantDO tenant = new TenantDO();
        tenant.setId(id);
        tenant.setStatus(1);
        tenantMapper.updateById(tenant);
    }

    private void ensureCodeUnique(String tenantCode, Long excludedId) {
        Long count = tenantMapper.selectCount(new LambdaQueryWrapper<TenantDO>()
                .eq(TenantDO::getTenantCode, tenantCode)
                .ne(excludedId != null, TenantDO::getId, excludedId));
        if (count != null && count > 0) {
            throw new BusinessException("租户编码已存在");
        }
    }

    private void copyCreate(TenantCreateRequest request, TenantDO tenant) {
        tenant.setTenantCode(request.getTenantCode());
        tenant.setTenantName(request.getTenantName());
        tenant.setContactUser(request.getContactUser());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setPackageId(request.getPackageId());
        tenant.setExpireTime(request.getExpireTime());
        tenant.setAccountCount(request.getAccountCount());
        tenant.setStatus(request.getStatus());
        tenant.setRemark(request.getRemark());
    }

    private void copyUpdate(TenantUpdateRequest request, TenantDO tenant) {
        tenant.setId(request.getId());
        tenant.setTenantCode(request.getTenantCode());
        tenant.setTenantName(request.getTenantName());
        tenant.setContactUser(request.getContactUser());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setPackageId(request.getPackageId());
        tenant.setExpireTime(request.getExpireTime());
        tenant.setAccountCount(request.getAccountCount());
        tenant.setStatus(request.getStatus());
        tenant.setRemark(request.getRemark());
    }
}
