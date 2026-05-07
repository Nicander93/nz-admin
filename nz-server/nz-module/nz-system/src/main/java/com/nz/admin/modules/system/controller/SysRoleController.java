package com.nz.admin.modules.system.controller;

import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.auth.annotation.PermissionMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.R;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.po.SysRoleDO;
import com.nz.admin.modules.system.entity.query.SysRoleQuery;
import com.nz.admin.modules.system.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/role")
public class SysRoleController {

    @Autowired
    private SysRoleService roleService;

    @SaCheckPermission("system:role:list")
    @Log(title = "角色管理", businessType = BusinessType.QUERY)
    @GetMapping("/page")
    public R<Page<SysRoleDO>> page(SysRoleQuery query) {
        return R.ok(roleService.listPage(query));
    }

    @GetMapping("/listAll")
    public R<List<SysRoleDO>> listAll() {
        return R.ok(roleService.listAll());
    }

    @SaCheckPermission("system:role:query")
    @Log(title = "角色管理", businessType = BusinessType.QUERY)
    @GetMapping("/{id}")
    public R<SysRoleDO> getById(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @SaCheckPermission("system:role:add")
    @Log(title = "角色管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody SysRoleDO role) {
        roleService.save(role);
        return R.ok();
    }

    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> update(@RequestBody SysRoleDO role) {
        roleService.updateById(role);
        return R.ok();
    }

    @SaCheckPermission("system:role:remove")
    @Log(title = "角色管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.removeById(id);
        return R.ok();
    }

    @SaCheckPermission(value = {"system:role:edit", "system:role:query"}, mode = PermissionMode.OR)
    @Log(title = "角色管理", businessType = BusinessType.QUERY)
    @GetMapping("/{roleId}/menuIds")
    public R<List<Long>> getMenuIds(@PathVariable Long roleId) {
        return R.ok(roleService.getMenuIdsByRoleId(roleId));
    }

    @SaCheckPermission("system:role:edit")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{roleId}/menus")
    public R<Void> assignMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(roleId, menuIds);
        return R.ok();
    }
}
