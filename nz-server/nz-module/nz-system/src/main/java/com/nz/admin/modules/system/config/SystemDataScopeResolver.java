package com.nz.admin.modules.system.config;

import com.nz.admin.common.core.BusinessException;
import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.datascope.core.DataScopeResolver;
import com.nz.admin.framework.datascope.core.DataScopeResult;
import com.nz.admin.modules.system.mapper.dept.DeptMapper;
import com.nz.admin.modules.system.mapper.role.RoleDeptMapper;
import com.nz.admin.modules.system.mapper.role.RoleMapper;
import com.nz.admin.modules.system.mapper.user.UserMapper;
import com.nz.admin.modules.system.mapper.user.UserRoleMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SystemDataScopeResolver implements DataScopeResolver {
    private final LoginUserContext loginUserContext;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RoleDeptMapper roleDeptMapper;
    private final DeptMapper deptMapper;
    private final com.nz.admin.modules.system.mapper.role.RoleMenuMapper roleMenuMapper;
    private final com.nz.admin.modules.system.mapper.menu.MenuMapper menuMapper;

    @Override
    public DataScopeResult resolve() {
        Long userId = loginUserContext.getLoginUserIdOrNull();
        if (userId == null) {
            throw new BusinessException("受保护数据操作需要登录身份");
        }
        var user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getStatus(), 0)) {
            throw new BusinessException("当前用户不可用");
        }
        Set<Long> departments = new LinkedHashSet<>();
        boolean self = false;
        for (var relation : userRoleMapper.selectByUserId(userId)) {
            var role = roleMapper.selectById(relation.getRoleId());
            if (role == null || !Objects.equals(role.getStatus(), 0)) {
                continue;
            }
            var required = com.nz.admin.framework.auth.core.PermissionContext.get();
            if (!required.isEmpty() && roleMenuMapper.selectByRoleId(role.getId()).stream()
                    .map(relationMenu -> menuMapper.selectById(relationMenu.getMenuId()))
                    .filter(Objects::nonNull)
                    .filter(menu -> menu.getPerm() != null && Objects.equals(menu.getStatus(), 0))
                    .noneMatch(menu -> required.contains(menu.getPerm()))) {
                continue;
            }
            if (Objects.equals(role.getDataScope(), 1)) {
                return new DataScopeResult(true, Set.of(), false, userId);
            }
            int scope = role.getDataScope() == null ? 5 : role.getDataScope();
            switch (scope) {
                case 2 ->
                        roleDeptMapper
                                .selectByRoleId(role.getId())
                                .forEach(row -> departments.add(row.getDeptId()));
                case 3 -> {
                    if (user.getDeptId() != null) departments.add(user.getDeptId());
                }
                case 4 -> addDepartmentTree(departments, user.getDeptId());
                case 5 -> self = true;
                default -> throw new BusinessException("角色数据范围不合法");
            }
        }
        return new DataScopeResult(false, departments, self, userId);
    }

    private void addDepartmentTree(Set<Long> result, Long rootId) {
        if (rootId == null) {
            return;
        }
        Set<Long> tree = new LinkedHashSet<>();
        tree.add(rootId);
        var departments = deptMapper.selectList(null);
        boolean changed;
        do {
            changed = false;
            for (var dept : departments) {
                if (tree.contains(dept.getParentId()) && tree.add(dept.getId())) changed = true;
            }
        } while (changed);
        result.addAll(tree);
    }
}
