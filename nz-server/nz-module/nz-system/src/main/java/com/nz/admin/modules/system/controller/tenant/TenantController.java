package com.nz.admin.modules.system.controller.tenant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dto.tenant.TenantCreateRequest;
import com.nz.admin.modules.system.entity.dto.tenant.TenantUpdateRequest;
import com.nz.admin.modules.system.service.tenant.TenantAdminGuard;
import com.nz.admin.modules.system.service.tenant.TenantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 租户管理接口。
 */
@RestController
@RequestMapping("/api/system/tenant")
public class TenantController {

    private final TenantService tenantService;
    private final TenantAdminGuard tenantAdminGuard;

    public TenantController(TenantService tenantService, TenantAdminGuard tenantAdminGuard) {
        this.tenantService = tenantService;
        this.tenantAdminGuard = tenantAdminGuard;
    }

    @SaCheckPermission("system:tenant:list")
    @GetMapping("/page")
    public R<PageResult<TenantDO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String tenantCode,
                                        @RequestParam(required = false) String tenantName,
                                        @RequestParam(required = false) Integer status) {
        tenantAdminGuard.requireDefaultTenant();
        Page<TenantDO> page = tenantService.page(pageNum, pageSize, tenantCode, tenantName, status);
        return R.ok(PageResult.of(page));
    }

    @SaCheckPermission("system:tenant:query")
    @GetMapping("/{id}")
    public R<TenantDO> get(@PathVariable Long id) {
        tenantAdminGuard.requireDefaultTenant();
        return R.ok(tenantService.getRequired(id));
    }

    @Log(title = "租户管理", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:tenant:add")
    @PostMapping
    public R<Long> create(@Valid @RequestBody TenantCreateRequest request) {
        tenantAdminGuard.requireDefaultTenant();
        return R.ok(tenantService.create(request));
    }

    @Log(title = "租户管理", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:tenant:edit")
    @PutMapping
    public R<Void> update(@Valid @RequestBody TenantUpdateRequest request) {
        tenantAdminGuard.requireDefaultTenant();
        tenantService.update(request);
        return R.ok();
    }

    @Log(title = "租户管理", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:tenant:remove")
    @DeleteMapping("/{id}")
    public R<Void> deactivate(@PathVariable Long id) {
        tenantAdminGuard.requireDefaultTenant();
        tenantService.deactivate(id);
        return R.ok();
    }
}
