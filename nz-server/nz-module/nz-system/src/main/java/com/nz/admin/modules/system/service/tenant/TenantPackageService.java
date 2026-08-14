package com.nz.admin.modules.system.service.tenant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantPackageDO;
import com.nz.admin.modules.system.entity.dto.tenant.TenantPackageCreateRequest;
import com.nz.admin.modules.system.entity.dto.tenant.TenantPackageUpdateRequest;
import com.nz.admin.modules.system.entity.vo.tenant.TenantPackageVO;

import java.util.List;
import java.util.Set;

public interface TenantPackageService {

    Page<TenantPackageDO> page(Integer pageNum, Integer pageSize, String packageName, Integer status);

    List<TenantPackageDO> listAllEnabled();

    TenantPackageDO getRequired(Long id);

    TenantPackageVO getDetail(Long id);

    Long create(TenantPackageCreateRequest request);

    void update(TenantPackageUpdateRequest request);

    void delete(Long id);

    Set<Long> getMenuIds(Long packageId);
}
