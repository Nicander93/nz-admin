package com.nz.admin.modules.system.service;

import com.nz.admin.modules.system.entity.*;
import com.nz.admin.modules.system.mapper.*;
import com.nz.admin.modules.system.service.impl.SysPermissionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysPermissionServiceImplTest {

    @InjectMocks
    private SysPermissionServiceImpl permissionService;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysRoleMenuMapper roleMenuMapper;
    @Mock
    private SysMenuMapper menuMapper;

    @Test
    void testGetRoleKeysByUserId() {
        // given
        SysUserRole ur = new SysUserRole();
        ur.setUserId(1L);
        ur.setRoleId(10L);
        when(userRoleMapper.selectByUserId(1L)).thenReturn(List.of(ur));

        SysRole role = new SysRole();
        role.setId(10L);
        role.setRoleKey("admin");
        when(roleMapper.selectById(10L)).thenReturn(role);

        // when
        Set<String> roleKeys = permissionService.getRoleKeysByUserId(1L);

        // then
        assertEquals(1, roleKeys.size());
        assertTrue(roleKeys.contains("admin"));
    }

    @Test
    void testGetRoleKeysByUserId_noRoles() {
        // given
        when(userRoleMapper.selectByUserId(1L)).thenReturn(Collections.emptyList());

        // when
        Set<String> roleKeys = permissionService.getRoleKeysByUserId(1L);

        // then
        assertTrue(roleKeys.isEmpty());
    }

    @Test
    void testGetPermsByUserId() {
        // given
        SysUserRole ur = new SysUserRole();
        ur.setUserId(1L);
        ur.setRoleId(10L);
        when(userRoleMapper.selectByUserId(1L)).thenReturn(List.of(ur));

        SysRoleMenu rm1 = new SysRoleMenu();
        rm1.setRoleId(10L);
        rm1.setMenuId(100L);
        SysRoleMenu rm2 = new SysRoleMenu();
        rm2.setRoleId(10L);
        rm2.setMenuId(101L);
        when(roleMenuMapper.selectByRoleId(10L)).thenReturn(List.of(rm1, rm2));

        SysMenu menu1 = new SysMenu();
        menu1.setId(100L);
        menu1.setPerm("system:user:list");
        when(menuMapper.selectById(100L)).thenReturn(menu1);

        SysMenu menu2 = new SysMenu();
        menu2.setId(101L);
        menu2.setPerm("system:user:add");
        when(menuMapper.selectById(101L)).thenReturn(menu2);

        // when
        Set<String> perms = permissionService.getPermsByUserId(1L);

        // then
        assertEquals(2, perms.size());
        assertTrue(perms.contains("system:user:list"));
        assertTrue(perms.contains("system:user:add"));
    }

    @Test
    void testGetPermsByUserId_filterBlankPerms() {
        // given
        SysUserRole ur = new SysUserRole();
        ur.setUserId(1L);
        ur.setRoleId(10L);
        when(userRoleMapper.selectByUserId(1L)).thenReturn(List.of(ur));

        SysRoleMenu rm = new SysRoleMenu();
        rm.setRoleId(10L);
        rm.setMenuId(100L);
        when(roleMenuMapper.selectByRoleId(10L)).thenReturn(List.of(rm));

        SysMenu menu = new SysMenu();
        menu.setId(100L);
        menu.setPerm("");
        when(menuMapper.selectById(100L)).thenReturn(menu);

        // when
        Set<String> perms = permissionService.getPermsByUserId(1L);

        // then
        assertTrue(perms.isEmpty());
    }

    @Test
    void testAssignUserRoles() {
        // given
        List<Long> roleIds = List.of(10L, 20L);

        // when
        permissionService.assignUserRoles(1L, roleIds);

        // then
        verify(userRoleMapper).deleteByUserId(1L);
        verify(userRoleMapper, times(2)).insert(any(SysUserRole.class));
    }

    @Test
    void testGetRoleIdsByUserId() {
        // given
        SysUserRole ur1 = new SysUserRole();
        ur1.setUserId(1L);
        ur1.setRoleId(10L);
        SysUserRole ur2 = new SysUserRole();
        ur2.setUserId(1L);
        ur2.setRoleId(20L);
        when(userRoleMapper.selectByUserId(1L)).thenReturn(List.of(ur1, ur2));

        // when
        List<Long> roleIds = permissionService.getRoleIdsByUserId(1L);

        // then
        assertEquals(2, roleIds.size());
        assertTrue(roleIds.contains(10L));
        assertTrue(roleIds.contains(20L));
    }
}
