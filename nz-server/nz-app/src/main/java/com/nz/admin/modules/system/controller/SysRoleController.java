package com.nz.admin.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.R;
import com.nz.admin.modules.system.entity.SysRole;
import com.nz.admin.modules.system.query.SysRoleQuery;
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
    @GetMapping("/page")
    public R<Page<SysRole>> page(SysRoleQuery query) {
        return R.ok(roleService.listPage(query));
    }

    @GetMapping("/listAll")
    public R<List<SysRole>> listAll() {
        return R.ok(roleService.listAll());
    }

    @SaCheckPermission("system:role:query")
    @GetMapping("/{id}")
    public R<SysRole> getById(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @SaCheckPermission("system:role:add")
    @PostMapping
    public R<Void> add(@RequestBody SysRole role) {
        roleService.save(role);
        return R.ok();
    }

    @SaCheckPermission("system:role:edit")
    @PutMapping
    public R<Void> update(@RequestBody SysRole role) {
        roleService.updateById(role);
        return R.ok();
    }

    @SaCheckPermission("system:role:remove")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        roleService.removeById(id);
        return R.ok();
    }

    @SaCheckPermission("system:role:edit")
    @GetMapping("/{roleId}/menuIds")
    public R<List<Long>> getMenuIds(@PathVariable Long roleId) {
        return R.ok(roleService.getMenuIdsByRoleId(roleId));
    }

    @SaCheckPermission("system:role:edit")
    @PutMapping("/{roleId}/menus")
    public R<Void> assignMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(roleId, menuIds);
        return R.ok();
    }
}
