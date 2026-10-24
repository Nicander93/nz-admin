package com.nz.admin.modules.system.service.permission;

import cn.hutool.core.util.StrUtil;
import com.nz.admin.framework.tenant.config.TenantProperties;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleMenuDO;
import com.nz.admin.modules.system.entity.dataobject.tenant.TenantDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserRoleDO;
import com.nz.admin.modules.system.mapper.menu.MenuMapper;
import com.nz.admin.modules.system.mapper.role.RoleMapper;
import com.nz.admin.modules.system.mapper.role.RoleMenuMapper;
import com.nz.admin.modules.system.mapper.tenant.TenantMapper;
import com.nz.admin.modules.system.mapper.user.UserRoleMapper;
import com.nz.admin.modules.system.service.permission.PermissionService;
import com.nz.admin.modules.system.service.tenant.TenantPackageService;
import com.nz.admin.modules.system.service.user.UserService;
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
    private UserService userService;
    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private RoleMenuMapper roleMenuMapper;
    @Autowired
    private MenuMapper menuMapper;
    @Autowired
    private TenantMapper tenantMapper;
    @Autowired
    private TenantPackageService tenantPackageService;
    @Autowired
    private TenantProperties tenantProperties;

    /**
     * 按用户 id 拿角色标识集合。
     */
    @Override
    public Set<String> getRoleKeysByUserId(Long userId) {
        return getEnabledRolesByUserId(userId).stream()
                .map(RoleDO::getRoleKey)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * 只从启用角色和启用菜单中解析功能权限；隐藏菜单仍可授权。
     */
    @Override
    public Set<String> getPermsByUserId(Long userId) {
        return getGrantedMenusByUserId(userId).stream()
                .filter(menu -> "C".equals(menu.getType()) || "F".equals(menu.getType()))
                .map(MenuDO::getPerm)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    /**
     * 按用户 id 拿菜单列表。
     */
    @Override
    public List<MenuDO> getMenusByUserId(Long userId) {
        return getGrantedMenusByUserId(userId).stream()
                .sorted(Comparator.comparing(MenuDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MenuDO::getId))
                .toList();
    }

    private List<RoleDO> getEnabledRolesByUserId(Long userId) {
        return getRoleIdsByUserId(userId).stream()
                .map(roleMapper::selectById)
                .filter(Objects::nonNull)
                .filter(role -> Integer.valueOf(0).equals(role.getStatus()))
                .toList();
    }

    private List<MenuDO> getGrantedMenusByUserId(Long userId) {
        Set<Long> menuIds = new HashSet<>();
        for (RoleDO role : getEnabledRolesByUserId(userId)) {
            roleMenuMapper.selectByRoleId(role.getId()).stream()
                    .map(RoleMenuDO::getMenuId).forEach(menuIds::add);
        }
        if (menuIds.isEmpty()) return List.of();
        retainMenusWithinTenantPackage(menuIds);
        if (menuIds.isEmpty()) return List.of();
        return menuMapper.selectBatchIds(menuIds).stream()
                .filter(Objects::nonNull)
                .filter(menu -> Integer.valueOf(0).equals(menu.getStatus()))
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
        if (userService.getById(userId) == null) {
            throw new com.nz.admin.common.core.BusinessException("用户不存在或无权访问");
        }
        if (roleIds == null || roleIds.stream().anyMatch(id -> roleMapper.selectById(id) == null)) {
            throw new com.nz.admin.common.core.BusinessException("角色不存在");
        }
        // 这里走覆盖式分配：先清掉旧关系，再写入新关系。
        userRoleMapper.deleteByUserId(userId);
        for (Long roleId : roleIds) {
            UserRoleDO ur = new UserRoleDO();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    private void retainMenusWithinTenantPackage(Set<Long> menuIds) {
        Long tenantId = TenantContextHolder.getTenantIdOrNull();
        if (tenantId == null || tenantProperties.getDefaultTenantId().equals(tenantId)) {
            return;
        }
        TenantDO tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            menuIds.clear();
            return;
        }
        menuIds.retainAll(tenantPackageService.getMenuIds(tenant.getPackageId()));
    }
}
