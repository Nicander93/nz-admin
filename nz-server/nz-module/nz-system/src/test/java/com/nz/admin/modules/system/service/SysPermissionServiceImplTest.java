package com.nz.admin.modules.system.service;

import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.SysMenu;
import com.nz.admin.modules.system.entity.SysRole;
import com.nz.admin.modules.system.entity.SysRoleMenu;
import com.nz.admin.modules.system.entity.SysUserRole;
import com.nz.admin.modules.system.mapper.SysMenuMapper;
import com.nz.admin.modules.system.mapper.SysRoleMapper;
import com.nz.admin.modules.system.mapper.SysRoleMenuMapper;
import com.nz.admin.modules.system.mapper.SysUserRoleMapper;
import com.nz.admin.modules.system.service.impl.SysPermissionServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Set;

import static com.nz.admin.framework.test.core.util.RandomPojoUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(NzSystemTestApplication.class)
class SysPermissionServiceImplTest extends BaseDbUnitTest {

    @Resource
    private SysPermissionServiceImpl permissionService;
    @Resource
    private SysUserRoleMapper userRoleMapper;
    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private SysRoleMenuMapper roleMenuMapper;
    @Resource
    private SysMenuMapper menuMapper;

    @Test
    void testGetRoleKeysByUserId() {
        SysRole role = randomPojo(SysRole.class)
                .setId(null)
                .setRoleKey("admin");
        roleMapper.insert(role);

        SysUserRole userRole = randomPojo(SysUserRole.class)
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
        SysRole role = randomPojo(SysRole.class)
                .setId(null);
        roleMapper.insert(role);

        SysUserRole userRole = randomPojo(SysUserRole.class)
                .setUserId(1L)
                .setRoleId(role.getId());
        userRoleMapper.insert(userRole);

        SysMenu menu1 = randomPojo(SysMenu.class)
                .setId(null)
                .setPerm("system:user:list");
        menuMapper.insert(menu1);

        SysMenu menu2 = randomPojo(SysMenu.class)
                .setId(null)
                .setPerm("system:user:add");
        menuMapper.insert(menu2);

        SysRoleMenu roleMenu1 = randomPojo(SysRoleMenu.class)
                .setRoleId(role.getId())
                .setMenuId(menu1.getId());
        roleMenuMapper.insert(roleMenu1);

        SysRoleMenu roleMenu2 = randomPojo(SysRoleMenu.class)
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
        SysRole role = randomPojo(SysRole.class)
                .setId(null);
        roleMapper.insert(role);

        SysUserRole userRole = randomPojo(SysUserRole.class)
                .setUserId(1L)
                .setRoleId(role.getId());
        userRoleMapper.insert(userRole);

        SysMenu menu = randomPojo(SysMenu.class)
                .setId(null)
                .setPerm("");
        menuMapper.insert(menu);

        SysRoleMenu roleMenu = randomPojo(SysRoleMenu.class)
                .setRoleId(role.getId())
                .setMenuId(menu.getId());
        roleMenuMapper.insert(roleMenu);

        Set<String> perms = permissionService.getPermsByUserId(1L);

        assertTrue(perms.isEmpty());
    }

    @Test
    void testAssignUserRoles() {
        SysUserRole old = randomPojo(SysUserRole.class)
                .setUserId(1L)
                .setRoleId(999L);
        userRoleMapper.insert(old);

        List<Long> roleIds = List.of(10L, 20L);
        permissionService.assignUserRoles(1L, roleIds);

        List<SysUserRole> dbRows = userRoleMapper.selectByUserId(1L);
        assertEquals(2, dbRows.size());
        assertTrue(dbRows.stream().anyMatch(item -> item.getRoleId().equals(10L)));
        assertTrue(dbRows.stream().anyMatch(item -> item.getRoleId().equals(20L)));
    }

    @Test
    void testGetRoleIdsByUserId() {
        SysUserRole userRole1 = randomPojo(SysUserRole.class)
                .setUserId(1L)
                .setRoleId(10L);
        userRoleMapper.insert(userRole1);

        SysUserRole userRole2 = randomPojo(SysUserRole.class)
                .setUserId(1L)
                .setRoleId(20L);
        userRoleMapper.insert(userRole2);

        List<Long> roleIds = permissionService.getRoleIdsByUserId(1L);

        assertEquals(2, roleIds.size());
        assertTrue(roleIds.contains(10L));
        assertTrue(roleIds.contains(20L));
    }
}
