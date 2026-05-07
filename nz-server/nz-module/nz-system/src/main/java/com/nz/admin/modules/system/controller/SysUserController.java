package com.nz.admin.modules.system.controller;

import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.common.R;
import com.nz.admin.framework.log.annotation.BusinessType;
import com.nz.admin.framework.log.annotation.Log;
import com.nz.admin.modules.system.entity.po.SysUserDO;
import com.nz.admin.modules.system.entity.query.SysUserQuery;
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
    @Log(title = "用户管理", businessType = BusinessType.QUERY)
    @GetMapping("/page")
    public R<Page<SysUserDO>> page(SysUserQuery query) {
        Page<SysUserDO> page = userService.listPage(query);
        page.getRecords().forEach(u -> u.setPassword(null));
        return R.ok(page);
    }

    @SaCheckPermission("system:user:query")
    @Log(title = "用户管理", businessType = BusinessType.QUERY)
    @GetMapping("/{id}")
    public R<SysUserDO> getById(@PathVariable Long id) {
        SysUserDO user = userService.getById(id);
        user.setPassword(null);
        return R.ok(user);
    }

    @SaCheckPermission("system:user:add")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody SysUserDO user) {
        user.setPassword(BCrypt.hashpw(user.getPassword()));
        userService.save(user);
        return R.ok();
    }

    @SaCheckPermission("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> update(@RequestBody SysUserDO user) {
        if (StrUtil.isNotBlank(user.getPassword())) {
            user.setPassword(BCrypt.hashpw(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        userService.updateById(user);
        return R.ok();
    }

    @SaCheckPermission("system:user:remove")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
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
