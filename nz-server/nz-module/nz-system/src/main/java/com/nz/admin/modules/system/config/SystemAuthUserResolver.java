package com.nz.admin.modules.system.config;

import com.nz.admin.framework.auth.core.AuthUserResolver;
import com.nz.admin.framework.auth.core.LoginUser;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.service.permission.PermissionService;
import com.nz.admin.modules.system.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;

/**
 * 系统模块登录用户解析实现。
 */
@Component
public class SystemAuthUserResolver implements AuthUserResolver {

    @Autowired
    private UserService userService;
    @Autowired
    private PermissionService permissionService;

    @Override
    public LoginUser resolve(Long userId) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);

        UserDO user = userService.getById(userId);
        if (user != null) {
            loginUser.setUsername(user.getUsername());
        }
        loginUser.setPermissions(new LinkedHashSet<>(permissionService.getPermsByUserId(userId)));
        loginUser.setRoles(new LinkedHashSet<>(permissionService.getRoleKeysByUserId(userId)));
        return loginUser;
    }
}
