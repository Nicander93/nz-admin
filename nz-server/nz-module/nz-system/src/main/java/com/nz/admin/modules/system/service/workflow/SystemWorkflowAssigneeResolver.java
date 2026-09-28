package com.nz.admin.modules.system.service.workflow;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.nz.admin.common.module.NzWorkflowAssigneeResolver;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserRoleDO;
import com.nz.admin.modules.system.mapper.role.RoleMapper;
import com.nz.admin.modules.system.mapper.user.UserMapper;
import com.nz.admin.modules.system.mapper.user.UserRoleMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 使用系统用户和角色数据解析工作流办理人。
 */
@Component
public class SystemWorkflowAssigneeResolver implements NzWorkflowAssigneeResolver {

    private static final String USER_PREFIX = "user:";
    private static final String ROLE_PREFIX = "role:";

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public SystemWorkflowAssigneeResolver(UserMapper userMapper,
                                          RoleMapper roleMapper,
                                          UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public List<String> resolveUserIds(List<String> assigneeExpressions) {
        if (assigneeExpressions == null || assigneeExpressions.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = new LinkedHashSet<>();
        Set<String> roleKeys = new LinkedHashSet<>();
        for (String expression : assigneeExpressions) {
            String value = StrUtil.trim(expression);
            if (NumberUtil.isLong(value)) {
                addPositiveUserId(userIds, value);
            } else if (StrUtil.startWith(value, USER_PREFIX)) {
                addPositiveUserId(userIds, StrUtil.removePrefix(value, USER_PREFIX));
            } else if (StrUtil.startWith(value, ROLE_PREFIX)) {
                String roleKey = StrUtil.removePrefix(value, ROLE_PREFIX);
                if (StrUtil.isNotBlank(roleKey)) {
                    roleKeys.add(roleKey);
                }
            }
        }
        addRoleUsers(userIds, roleKeys);
        if (userIds.isEmpty()) {
            return List.of();
        }

        Set<Long> activeUserIds = userMapper.selectActiveByIds(userIds).stream()
                .map(UserDO::getId)
                .collect(Collectors.toSet());
        return userIds.stream()
                .filter(activeUserIds::contains)
                .map(String::valueOf)
                .toList();
    }

    private void addPositiveUserId(Set<Long> userIds, String value) {
        if (!NumberUtil.isLong(value)) {
            return;
        }
        long userId = NumberUtil.parseLong(value);
        if (userId > 0) {
            userIds.add(userId);
        }
    }

    private void addRoleUsers(Set<Long> userIds, Set<String> roleKeys) {
        if (roleKeys.isEmpty()) {
            return;
        }
        List<Long> roleIds = roleMapper.selectActiveByKeys(roleKeys).stream()
                .map(RoleDO::getId)
                .toList();
        if (roleIds.isEmpty()) {
            return;
        }
        userRoleMapper.selectByRoleIds(roleIds).stream()
                .map(UserRoleDO::getUserId)
                .forEach(userIds::add);
    }
}
