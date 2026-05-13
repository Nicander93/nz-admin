package com.nz.admin.modules.system.config;

import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.framework.auth.core.PermissionResolver;
import org.springframework.stereotype.Component;

/**
 * 系统模块权限解析实现。
 */
@Component
public class SystemPermissionResolver implements PermissionResolver {

    @Override
    public void checkPermission(String permission) {
        StpUtil.checkPermission(permission);
    }

    @Override
    public boolean hasPermission(String permission) {
        return StpUtil.hasPermission(permission);
    }

    @Override
    public void checkRole(String role) {
        StpUtil.checkRole(role);
    }

    @Override
    public boolean hasRole(String role) {
        return StpUtil.hasRole(role);
    }
}
