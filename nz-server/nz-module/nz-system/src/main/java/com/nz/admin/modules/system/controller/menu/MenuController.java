package com.nz.admin.modules.system.controller.menu;

import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.common.core.R;
import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;
import com.nz.admin.modules.system.service.menu.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/menu")
public class MenuController {

    @Autowired
    private MenuService menuService;

    @SaCheckPermission("system:menu:list")
    @GetMapping("/list")
    public R<List<MenuDO>> list() {
        return R.ok(menuService.listAll());
    }

    @SaCheckPermission("system:menu:query")
    @GetMapping("/{id}")
    public R<MenuDO> getById(@PathVariable Long id) {
        return R.ok(menuService.getById(id));
    }

    @SaCheckPermission("system:menu:add")
    @PostMapping
    public R<Void> add(@RequestBody MenuDO menu) {
        menuService.save(menu);
        return R.ok();
    }

    @SaCheckPermission("system:menu:edit")
    @PutMapping
    public R<Void> update(@RequestBody MenuDO menu) {
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
