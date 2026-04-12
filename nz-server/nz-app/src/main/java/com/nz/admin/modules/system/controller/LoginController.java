package com.nz.admin.modules.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.nz.admin.common.R;
import com.nz.admin.modules.system.entity.SysUser;
import com.nz.admin.modules.system.service.SysPermissionService;
import com.nz.admin.modules.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    @Autowired
    private SysUserService userService;
    @Autowired
    private SysPermissionService permissionService;

    public record LoginBody(String username, String password) {}

    @PostMapping("/login")
    public R<String> login(@RequestBody LoginBody body) {
        SysUser user = userService.getByUsername(body.username());
        if (user == null || !BCrypt.checkpw(body.password(), user.getPassword())) {
            return R.fail("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            return R.fail("账号已被禁用");
        }
        StpUtil.login(user.getId());
        return R.ok(StpUtil.getTokenValue());
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @GetMapping("/info")
    public R<Map<String, Object>> info() {
        long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userService.getById(userId);
        user.setPassword(null);

        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("roles", permissionService.getRoleKeysByUserId(userId));
        result.put("permissions", permissionService.getPermsByUserId(userId));
        return R.ok(result);
    }
}
