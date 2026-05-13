package com.nz.admin.modules.system.service.permission;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleMenuDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserRoleDO;
import com.nz.admin.modules.system.mapper.menu.MenuMapper;
import com.nz.admin.modules.system.mapper.role.RoleMapper;
import com.nz.admin.modules.system.mapper.role.RoleMenuMapper;
import com.nz.admin.modules.system.mapper.user.UserRoleMapper;
import com.nz.admin.modules.system.service.permission.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限这块的服务实现，主要做用户、角色、菜单之间的关系整理。
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private UserRoleMapper userRoleMapper;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private RoleMenuMapper roleMenuMapper;
    @Autowired
    private MenuMapper menuMapper;

    /**
     * 按用户 id 拿角色标识集合。
     */
    @Override
    public Set<String> getRoleKeysByUserId(Long userId) {
        List<Long> roleIds = getRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) return Collections.emptySet();
        return roleIds.stream()
                .map(roleMapper::selectById)
                .filter(Objects::nonNull)
                .map(RoleDO::getRoleKey)
                .collect(Collectors.toSet());
    }

    /**
     * 按用户 id 拿权限标识集合。
     */
    @Override
    public Set<String> getPermsByUserId(Long userId) {
        List<Long> roleIds = getRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) return Collections.emptySet();

        Set<Long> menuIds = new HashSet<>();
        for (Long roleId : roleIds) {
            roleMenuMapper.selectByRoleId(roleId).stream()
                    .map(RoleMenuDO::getMenuId)
                    .forEach(menuIds::add);
        }
        if (menuIds.isEmpty()) return Collections.emptySet();

        return menuIds.stream()
                .map(menuMapper::selectById)
                .filter(Objects::nonNull)
                .map(MenuDO::getPerm)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * 按用户 id 拿菜单列表。
     */
    @Override
    public List<MenuDO> getMenusByUserId(Long userId) {
        List<Long> roleIds = getRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) return Collections.emptyList();

        Set<Long> menuIds = new HashSet<>();
        for (Long roleId : roleIds) {
            roleMenuMapper.selectByRoleId(roleId).stream()
                    .map(RoleMenuDO::getMenuId)
                    .forEach(menuIds::add);
        }
        if (menuIds.isEmpty()) return Collections.emptyList();

        return menuMapper.selectBatchIds(menuIds).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MenuDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MenuDO::getId))
                .toList();
    }

    /**
     * 按用户 id 拿角色 id 列表。
     */
    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectByUserId(userId).stream()
                .map(UserRoleDO::getRoleId).toList();
    }

    /**
     * 给用户重新分配角色。
     */
    @Override
    @Transactional
    public void assignUserRoles(Long userId, List<Long> roleIds) {
        // 这里走覆盖式分配：先清掉旧关系，再写入新关系。
        userRoleMapper.deleteByUserId(userId);
        for (Long roleId : roleIds) {
            UserRoleDO ur = new UserRoleDO();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }
}
