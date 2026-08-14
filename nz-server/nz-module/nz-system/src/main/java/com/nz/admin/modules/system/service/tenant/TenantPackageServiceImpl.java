package com.nz.admin.modules.system.service.tenant;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantPackageDO;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantPackageMenuDO;
import com.nz.admin.modules.system.entity.dto.tenant.TenantPackageCreateRequest;
import com.nz.admin.modules.system.entity.dto.tenant.TenantPackageUpdateRequest;
import com.nz.admin.modules.system.entity.vo.tenant.TenantPackageVO;
import com.nz.admin.modules.system.mapper.tenant.TenantMapper;
import com.nz.admin.modules.system.mapper.tenant.TenantPackageMapper;
import com.nz.admin.modules.system.mapper.tenant.TenantPackageMenuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 租户套餐管理服务。
 */
@Service
public class TenantPackageServiceImpl implements TenantPackageService {

    private final TenantPackageMapper tenantPackageMapper;
    private final TenantPackageMenuMapper tenantPackageMenuMapper;
    private final TenantMapper tenantMapper;
    private final TenantProvisioningService provisioningService;

    public TenantPackageServiceImpl(TenantPackageMapper tenantPackageMapper,
                                    TenantPackageMenuMapper tenantPackageMenuMapper,
                                    TenantMapper tenantMapper,
                                    TenantProvisioningService provisioningService) {
        this.tenantPackageMapper = tenantPackageMapper;
        this.tenantPackageMenuMapper = tenantPackageMenuMapper;
        this.tenantMapper = tenantMapper;
        this.provisioningService = provisioningService;
    }

    @Override
    public Page<TenantPackageDO> page(Integer pageNum, Integer pageSize, String packageName, Integer status) {
        return tenantPackageMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<TenantPackageDO>()
                        .like(StrUtil.isNotBlank(packageName), TenantPackageDO::getPackageName, packageName)
                        .eq(status != null, TenantPackageDO::getStatus, status)
                        .orderByDesc(TenantPackageDO::getId));
    }

    @Override
    public List<TenantPackageDO> listAllEnabled() {
        return tenantPackageMapper.selectList(new LambdaQueryWrapper<TenantPackageDO>()
                .eq(TenantPackageDO::getStatus, 0)
                .orderByAsc(TenantPackageDO::getId));
    }

    @Override
    public TenantPackageDO getRequired(Long id) {
        TenantPackageDO tenantPackage = tenantPackageMapper.selectById(id);
        if (tenantPackage == null) {
            throw new BusinessException("租户套餐不存在");
        }
        return tenantPackage;
    }

    @Override
    public TenantPackageVO getDetail(Long id) {
        TenantPackageDO tenantPackage = getRequired(id);
        TenantPackageVO vo = new TenantPackageVO();
        vo.setId(tenantPackage.getId());
        vo.setPackageName(tenantPackage.getPackageName());
        vo.setStatus(tenantPackage.getStatus());
        vo.setRemark(tenantPackage.getRemark());
        vo.setCreateTime(tenantPackage.getCreateTime());
        vo.setUpdateTime(tenantPackage.getUpdateTime());
        vo.setMenuIds(getMenuIds(id).stream().toList());
        return vo;
    }

    @Override
    @Transactional
    public Long create(TenantPackageCreateRequest request) {
        ensureNameUnique(request.getPackageName(), null);
        TenantPackageDO tenantPackage = new TenantPackageDO();
        tenantPackage.setPackageName(request.getPackageName());
        tenantPackage.setStatus(request.getStatus());
        tenantPackage.setRemark(request.getRemark());
        tenantPackageMapper.insert(tenantPackage);
        replaceMenus(tenantPackage.getId(), request.getMenuIds());
        return tenantPackage.getId();
    }

    @Override
    @Transactional
    public void update(TenantPackageUpdateRequest request) {
        getRequired(request.getId());
        ensureNameUnique(request.getPackageName(), request.getId());

        TenantPackageDO tenantPackage = new TenantPackageDO();
        tenantPackage.setId(request.getId());
        tenantPackage.setPackageName(request.getPackageName());
        tenantPackage.setStatus(request.getStatus());
        tenantPackage.setRemark(request.getRemark());
        tenantPackageMapper.updateById(tenantPackage);
        replaceMenus(request.getId(), request.getMenuIds());

        List<TenantDO> tenants = tenantMapper.selectList(new LambdaQueryWrapper<TenantDO>()
                .eq(TenantDO::getPackageId, request.getId()));
        for (TenantDO tenant : tenants) {
            provisioningService.applyPackageMenus(tenant.getId(), request.getId());
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getRequired(id);
        Long tenantCount = tenantMapper.selectCount(new LambdaQueryWrapper<TenantDO>()
                .eq(TenantDO::getPackageId, id));
        if (tenantCount != null && tenantCount > 0) {
            throw new BusinessException("套餐仍被租户使用，不能删除");
        }
        tenantPackageMenuMapper.deleteByPackageId(id);
        tenantPackageMapper.deleteById(id);
    }

    @Override
    public Set<Long> getMenuIds(Long packageId) {
        Set<Long> menuIds = new LinkedHashSet<>();
        tenantPackageMenuMapper.selectByPackageId(packageId)
                .forEach(row -> menuIds.add(row.getMenuId()));
        return menuIds;
    }

    private void replaceMenus(Long packageId, List<Long> menuIds) {
        tenantPackageMenuMapper.deleteByPackageId(packageId);
        if (menuIds == null) {
            return;
        }
        for (Long menuId : new LinkedHashSet<>(menuIds)) {
            if (menuId == null) {
                continue;
            }
            TenantPackageMenuDO relation = new TenantPackageMenuDO();
            relation.setPackageId(packageId);
            relation.setMenuId(menuId);
            tenantPackageMenuMapper.insert(relation);
        }
    }

    private void ensureNameUnique(String packageName, Long excludedId) {
        Long count = tenantPackageMapper.selectCount(new LambdaQueryWrapper<TenantPackageDO>()
                .eq(TenantPackageDO::getPackageName, packageName)
                .ne(excludedId != null, TenantPackageDO::getId, excludedId));
        if (count != null && count > 0) {
            throw new BusinessException("租户套餐名称已存在");
        }
    }
}
