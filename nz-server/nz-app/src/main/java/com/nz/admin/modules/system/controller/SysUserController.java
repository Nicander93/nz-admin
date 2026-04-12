package com.nz.admin.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.R;
import com.nz.admin.modules.system.entity.SysUser;
import com.nz.admin.modules.system.query.SysUserQuery;
import com.nz.admin.modules.system.service.SysPermissionService;
import com.nz.admin.modules.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/user")
public class SysUserController {

    @Autowired
    private SysUserService userService;
    @Autowired
    private SysPermissionService permissionService;

    @SaCheckPermission("system:user:list")
    @GetMapping("/page")
    public R<Page<SysUser>> page(SysUserQuery query) {
        Page<SysUser> page = userService.listPage(query);
        page.getRecords().forEach(u -> u.setPassword(null));
        return R.ok(page);
    }

    @SaCheckPermission("system:user:query")
    @GetMapping("/{id}")
    public R<SysUser> getById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        user.setPassword(null);
        return R.ok(user);
    }

    @SaCheckPermission("system:user:add")
    @PostMapping
    public R<Void> add(@RequestBody SysUser user) {
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        userService.save(user);
        return R.ok();
    }

    @SaCheckPermission("system:user:edit")
    @PutMapping
    public R<Void> update(@RequestBody SysUser user) {
        if (StrUtil.isNotBlank(user.getPassword())) {
            user.setPassword(BCrypt.hashpw(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userService.updateById(user);
        return R.ok();
    }

    @SaCheckPermission("system:user:remove")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return R.ok();
    }

    @SaCheckPermission("system:user:edit")
    @GetMapping("/{userId}/roleIds")
    public R<List<Long>> getRoleIds(@PathVariable Long userId) {
        return R.ok(permissionService.getRoleIdsByUserId(userId));
    }

    @SaCheckPermission("system:user:edit")
    @PutMapping("/{userId}/roles")
    public R<Void> assignRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        permissionService.assignUserRoles(userId, roleIds);
        return R.ok();
    }
}
