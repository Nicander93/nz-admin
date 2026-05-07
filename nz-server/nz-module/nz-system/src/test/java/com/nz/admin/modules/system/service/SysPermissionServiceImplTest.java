package com.nz.admin.modules.system.service;

import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.po.SysMenuDO;
import com.nz.admin.modules.system.entity.po.SysRoleDO;
import com.nz.admin.modules.system.entity.po.SysRoleMenuDO;
import com.nz.admin.modules.system.entity.po.SysUserRoleDO;
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
        SysRoleDO role = randomPojo(SysRoleDO.class)
                .setId(null)
                .setRoleKey("admin");
        roleMapper.insert(role);

        SysUserRoleDO userRole = randomPojo(SysUserRoleDO.class)
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
        SysRoleDO role = randomPojo(SysRoleDO.class)
                .setId(null);
        roleMapper.insert(role);

        SysUserRoleDO userRole = randomPojo(SysUserRoleDO.class)
                .setUserId(1L)
                .setRoleId(role.getId());
        userRoleMapper.insert(userRole);

        SysMenuDO menu1 = randomPojo(SysMenuDO.class)
                .setId(null)
                .setPerm("system:user:list");
        menuMapper.insert(menu1);

        SysMenuDO menu2 = randomPojo(SysMenuDO.class)
                .setId(null)
                .setPerm("system:user:add");
        menuMapper.insert(menu2);

        SysRoleMenuDO roleMenu1 = randomPojo(SysRoleMenuDO.class)
                .setRoleId(role.getId())
                .setMenuId(menu1.getId());
        roleMenuMapper.insert(roleMenu1);

        SysRoleMenuDO roleMenu2 = randomPojo(SysRoleMenuDO.class)
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
        SysRoleDO role = randomPojo(SysRoleDO.class)
                .setId(null);
        roleMapper.insert(role);

        SysUserRoleDO userRole = randomPojo(SysUserRoleDO.class)
                .setUserId(1L)
                .setRoleId(role.getId());
        userRoleMapper.insert(userRole);

        SysMenuDO menu = randomPojo(SysMenuDO.class)
                .setId(null)
                .setPerm("");
        menuMapper.insert(menu);

        SysRoleMenuDO roleMenu = randomPojo(SysRoleMenuDO.class)
                .setRoleId(role.getId())
                .setMenuId(menu.getId());
        roleMenuMapper.insert(roleMenu);

        Set<String> perms = permissionService.getPermsByUserId(1L);

        assertTrue(perms.isEmpty());
    }

    @Test
    void testAssignUserRoles() {
        SysUserRoleDO old = randomPojo(SysUserRoleDO.class)
                .setUserId(1L)
                .setRoleId(999L);
        userRoleMapper.insert(old);

        List<Long> roleIds = List.of(10L, 20L);
        permissionService.assignUserRoles(1L, roleIds);

        List<SysUserRoleDO> dbRows = userRoleMapper.selectByUserId(1L);
        assertEquals(2, dbRows.size());
        assertTrue(dbRows.stream().anyMatch(item -> item.getRoleId().equals(10L)));
        assertTrue(dbRows.stream().anyMatch(item -> item.getRoleId().equals(20L)));
    }

    @Test
    void testGetRoleIdsByUserId() {
        SysUserRoleDO userRole1 = randomPojo(SysUserRoleDO.class)
                .setUserId(1L)
                .setRoleId(10L);
        userRoleMapper.insert(userRole1);

        SysUserRoleDO userRole2 = randomPojo(SysUserRoleDO.class)
                .setUserId(1L)
                .setRoleId(20L);
        userRoleMapper.insert(userRole2);

        List<Long> roleIds = permissionService.getRoleIdsByUserId(1L);

        assertEquals(2, roleIds.size());
        assertTrue(roleIds.contains(10L));
        assertTrue(roleIds.contains(20L));
    }
}
