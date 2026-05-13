package com.nz.admin.modules.system.config;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.framework.datascope.DataScopeUserResolver;
import com.nz.admin.modules.system.service.permission.PermissionService;
import com.nz.admin.modules.system.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * system 模块数据权限用户信息解析实现。
 */
@Component
public class DataScopeUserResolverImpl implements DataScopeUserResolver {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserService userService;

    @Override
    public boolean isAdmin(Long userId) {
        Set<String> roleKeys = permissionService.getRoleKeysByUserId(userId);
        if (roleKeys == null || roleKeys.isEmpty()) {
            return false;
        }
        return roleKeys.stream()
                .filter(StrUtil::isNotBlank)
                .anyMatch("admin"::equalsIgnoreCase);
    }

    @Override
    public Long getDeptId(Long userId) {
        var user = userService.getById(userId);
        if (user == null) {
            return null;
        }
        return user.getDeptId();
    }
}
