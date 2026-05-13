package com.nz.admin.modules.system.service;

import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.dataobject.menu.MenuDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleDO;
import com.nz.admin.modules.system.entity.dataobject.role.RoleMenuDO;
import com.nz.admin.modules.system.entity.dataobject.user.UserRoleDO;
import com.nz.admin.modules.system.mapper.menu.MenuMapper;
import com.nz.admin.modules.system.mapper.role.RoleMapper;
import com.nz.admin.modules.system.mapper.role.RoleMenuMapper;
import com.nz.admin.modules.system.mapper.user.UserRoleMapper;
import com.nz.admin.modules.system.service.permission.PermissionServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;

import static com.nz.admin.framework.test.core.util.RandomPojoUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(NzSystemTestApplication.class)
class PermissionServiceImplTest extends BaseDbUnitTest {

    @Resource
    private PermissionServiceImpl permissionService;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private MenuMapper menuMapper;

    @Test
    void testGetRoleKeysByUserId() {
        RoleDO role = randomPojo(RoleDO.class)
                .setId(null)
                .setRoleKey("admin");
        roleMapper.insert(role);

        UserRoleDO userRole = randomPojo(UserRoleDO.class)
                .setUserId(1L)
                .setRoleId(role.getId());
        userRoleMapper.insert(userRole);

        Set<String> roleKeys = permissionService.getRoleKeysByUserId(1L);

        assertEquals(1, roleKeys.size());
        assertTrue(roleKeys.contains("admin"));
    }

    @Test
    void testGetRoleKeysByUserId_noRoles() {
        Set<String> roleKeys = permissionService.getRoleKeysByUserId(1L);
        assertTrue(roleKeys.isEmpty());
    }

    @Test
    void testGetPermsByUserId() {
        RoleDO role = randomPojo(RoleDO.class)
                .setId(null);
        roleMapper.insert(role);

        UserRoleDO userRole = randomPojo(UserRoleDO.class)
                .setUserId(1L)
                .setRoleId(role.getId());
        userRoleMapper.insert(userRole);

        MenuDO menu1 = randomPojo(MenuDO.class)
                .setId(null)
                .setPerm("system:user:list");
        menuMapper.insert(menu1);

        MenuDO menu2 = randomPojo(MenuDO.class)
                .setId(null)
                .setPerm("system:user:add");
        menuMapper.insert(menu2);

        RoleMenuDO roleMenu1 = randomPojo(RoleMenuDO.class)
                .setRoleId(role.getId())
                .setMenuId(menu1.getId());
        roleMenuMapper.insert(roleMenu1);

        RoleMenuDO roleMenu2 = randomPojo(RoleMenuDO.class)
                .setRoleId(role.getId())
                .setMenuId(menu2.getId());
        roleMenuMapper.insert(roleMenu2);

        Set<String> perms = permissionService.getPermsByUserId(1L);

        assertEquals(2, perms.size());
        assertTrue(perms.contains("system:user:list"));
        assertTrue(perms.contains("system:user:add"));
    }

    @Test
    void testGetPermsByUserId_filterBlankPerms() {
        RoleDO role = randomPojo(RoleDO.class)
                .setId(null);
        roleMapper.insert(role);

        UserRoleDO userRole = randomPojo(UserRoleDO.class)
                .setUserId(1L)
                .setRoleId(role.getId());
        userRoleMapper.insert(userRole);

        MenuDO menu = randomPojo(MenuDO.class)
                .setId(null)
                .setPerm("");
        menuMapper.insert(menu);

        RoleMenuDO roleMenu = randomPojo(RoleMenuDO.class)
                .setRoleId(role.getId())
                .setMenuId(menu.getId());
        roleMenuMapper.insert(roleMenu);

        Set<String> perms = permissionService.getPermsByUserId(1L);

        assertTrue(perms.isEmpty());
    }

    @Test
    void testAssignUserRoles() {
        UserRoleDO old = randomPojo(UserRoleDO.class)
                .setUserId(1L)
                .setRoleId(999L);
        userRoleMapper.insert(old);

        List<Long> roleIds = List.of(10L, 20L);
        permissionService.assignUserRoles(1L, roleIds);

        List<UserRoleDO> dbRows = userRoleMapper.selectByUserId(1L);
        assertEquals(2, dbRows.size());
        assertTrue(dbRows.stream().anyMatch(item -> item.getRoleId().equals(10L)));
        assertTrue(dbRows.stream().anyMatch(item -> item.getRoleId().equals(20L)));
    }

    @Test
    void testGetRoleIdsByUserId() {
        UserRoleDO userRole1 = randomPojo(UserRoleDO.class)
                .setUserId(1L)
                .setRoleId(10L);
        userRoleMapper.insert(userRole1);

        UserRoleDO userRole2 = randomPojo(UserRoleDO.class)
                .setUserId(1L)
                .setRoleId(20L);
        userRoleMapper.insert(userRole2);

        List<Long> roleIds = permissionService.getRoleIdsByUserId(1L);

        assertEquals(2, roleIds.size());
        assertTrue(roleIds.contains(10L));
        assertTrue(roleIds.contains(20L));
    }
}
