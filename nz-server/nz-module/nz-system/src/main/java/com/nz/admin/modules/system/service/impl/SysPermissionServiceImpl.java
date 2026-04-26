package com.nz.admin.modules.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.modules.system.entity.SysMenu;
import com.nz.admin.modules.system.entity.SysRole;
import com.nz.admin.modules.system.entity.SysRoleMenu;
import com.nz.admin.modules.system.entity.SysUserRole;
import com.nz.admin.modules.system.mapper.SysMenuMapper;
import com.nz.admin.modules.system.mapper.SysRoleMapper;
import com.nz.admin.modules.system.mapper.SysRoleMenuMapper;
import com.nz.admin.modules.system.mapper.SysUserRoleMapper;
import com.nz.admin.modules.system.service.SysPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限这块的服务实现，主要做用户、角色、菜单之间的关系整理。
 */
@Service
public class SysPermissionServiceImpl implements SysPermissionService {

    @Autowired
    private SysUserRoleMapper userRoleMapper;
    @Autowired
    private SysRoleMapper roleMapper;
    @Autowired
    private SysRoleMenuMapper roleMenuMapper;
    @Autowired
    private SysMenuMapper menuMapper;

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
                .map(SysRole::getRoleKey)
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
                    .map(SysRoleMenu::getMenuId)
                    .forEach(menuIds::add);
        }
        if (menuIds.isEmpty()) return Collections.emptySet();

        return menuIds.stream()
                .map(menuMapper::selectById)
                .filter(Objects::nonNull)
                .map(SysMenu::getPerm)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * 按用户 id 拿菜单列表。
     */
    @Override
    public List<SysMenu> getMenusByUserId(Long userId) {
        List<Long> roleIds = getRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) return Collections.emptyList();

        Set<Long> menuIds = new HashSet<>();
        for (Long roleId : roleIds) {
            roleMenuMapper.selectByRoleId(roleId).stream()
                    .map(SysRoleMenu::getMenuId)
                    .forEach(menuIds::add);
        }
        if (menuIds.isEmpty()) return Collections.emptyList();

        return menuMapper.selectBatchIds(menuIds).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SysMenu::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysMenu::getId))
                .toList();
    }

    /**
     * 按用户 id 拿角色 id 列表。
     */
    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        return userRoleMapper.selectByUserId(userId).stream()
                .map(SysUserRole::getRoleId).toList();
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
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }
}
