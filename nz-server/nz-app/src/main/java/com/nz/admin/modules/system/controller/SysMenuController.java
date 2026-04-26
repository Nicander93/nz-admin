package com.nz.admin.modules.system.controller;

import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.common.R;
import com.nz.admin.modules.system.entity.SysMenu;
import com.nz.admin.modules.system.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/menu")
public class SysMenuController {

    @Autowired
    private SysMenuService menuService;

    @SaCheckPermission("system:menu:list")
    @GetMapping("/list")
    public R<List<SysMenu>> list() {
        return R.ok(menuService.listAll());
    }

    @SaCheckPermission("system:menu:query")
    @GetMapping("/{id}")
    public R<SysMenu> getById(@PathVariable Long id) {
        return R.ok(menuService.getById(id));
    }

    @SaCheckPermission("system:menu:add")
    @PostMapping
    public R<Void> add(@RequestBody SysMenu menu) {
        menuService.save(menu);
        return R.ok();
    }

    @SaCheckPermission("system:menu:edit")
    @PutMapping
    public R<Void> update(@RequestBody SysMenu menu) {
        menuService.updateById(menu);
        return R.ok();
    }

    @SaCheckPermission("system:menu:remove")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        menuService.removeById(id);
        return R.ok();
    }
}
