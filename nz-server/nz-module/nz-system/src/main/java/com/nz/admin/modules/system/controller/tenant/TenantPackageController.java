package com.nz.admin.modules.system.controller.tenant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.core.PageResult;
import com.nz.admin.common.core.R;
import com.nz.admin.framework.auth.annotation.PermissionMode;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantPackageDO;
import com.nz.admin.modules.system.entity.dto.tenant.TenantPackageCreateRequest;
import com.nz.admin.modules.system.entity.dto.tenant.TenantPackageUpdateRequest;
import com.nz.admin.modules.system.entity.vo.tenant.TenantPackageVO;
import com.nz.admin.modules.system.service.tenant.TenantAdminGuard;
import com.nz.admin.modules.system.service.tenant.TenantPackageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户套餐管理接口。
 */
@RestController
@RequestMapping("/api/system/tenant-package")
public class TenantPackageController {

    private final TenantPackageService tenantPackageService;
    private final TenantAdminGuard tenantAdminGuard;

    public TenantPackageController(TenantPackageService tenantPackageService, TenantAdminGuard tenantAdminGuard) {
        this.tenantPackageService = tenantPackageService;
        this.tenantAdminGuard = tenantAdminGuard;
    }

    @SaCheckPermission("system:tenantpackage:list")
    @GetMapping("/page")
    public R<PageResult<TenantPackageDO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) String packageName,
                                               @RequestParam(required = false) Integer status) {
        tenantAdminGuard.requireDefaultTenant();
        Page<TenantPackageDO> page = tenantPackageService.page(pageNum, pageSize, packageName, status);
        return R.ok(PageResult.of(page));
    }

    @SaCheckPermission(value = {"system:tenant:query", "system:tenant:add", "system:tenant:edit"}, mode = PermissionMode.OR)
    @GetMapping("/list-all")
    public R<List<TenantPackageDO>> listAll() {
        tenantAdminGuard.requireDefaultTenant();
        return R.ok(tenantPackageService.listAllEnabled());
    }

    @SaCheckPermission("system:tenantpackage:query")
    @GetMapping("/{id}")
    public R<TenantPackageVO> get(@PathVariable Long id) {
        tenantAdminGuard.requireDefaultTenant();
        return R.ok(tenantPackageService.getDetail(id));
    }

    @Log(title = "租户套餐", businessType = BusinessType.INSERT)
    @SaCheckPermission("system:tenantpackage:add")
    @PostMapping
    public R<Long> create(@Valid @RequestBody TenantPackageCreateRequest request) {
        tenantAdminGuard.requireDefaultTenant();
        return R.ok(tenantPackageService.create(request));
    }

    @Log(title = "租户套餐", businessType = BusinessType.UPDATE)
    @SaCheckPermission("system:tenantpackage:edit")
    @PutMapping
    public R<Void> update(@Valid @RequestBody TenantPackageUpdateRequest request) {
        tenantAdminGuard.requireDefaultTenant();
        tenantPackageService.update(request);
        return R.ok();
    }

    @Log(title = "租户套餐", businessType = BusinessType.DELETE)
    @SaCheckPermission("system:tenantpackage:remove")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        tenantAdminGuard.requireDefaultTenant();
        tenantPackageService.delete(id);
        return R.ok();
    }
}
