package com.nz.admin.modules.system.service.tenant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dto.tenant.TenantCreateRequest;
import com.nz.admin.modules.system.entity.dto.tenant.TenantUpdateRequest;

public interface TenantService {

    Page<TenantDO> page(Integer pageNum, Integer pageSize, String tenantCode, String tenantName, Integer status);

    TenantDO getRequired(Long id);

    TenantDO getByCode(String tenantCode);

    TenantDO validateLoginTenant(String tenantCode);

    Long create(TenantCreateRequest request);

    void update(TenantUpdateRequest request);

    void deactivate(Long id);
}
