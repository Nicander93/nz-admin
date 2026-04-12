package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysRole;
import com.nz.admin.modules.system.entity.SysRoleMenu;
import com.nz.admin.modules.system.mapper.SysRoleMapper;
import com.nz.admin.modules.system.mapper.SysRoleMenuMapper;
import com.nz.admin.modules.system.query.SysRoleQuery;
import com.nz.admin.modules.system.service.impl.SysRoleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysRoleServiceImplTest {

    @InjectMocks
    private SysRoleServiceImpl roleService;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysRoleMenuMapper roleMenuMapper;

    @Test
    void testListPage() {
        // given
        SysRoleQuery query = new SysRoleQuery();
        query.setPageNum(1);
        query.setPageSize(10);

        SysRole role = new SysRole();
        role.setId(1L);
        role.setName("管理员");
        role.setRoleKey("admin");

        Page<SysRole> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(List.of(role));
        when(roleMapper.selectPageByCondition(any(), eq(query))).thenReturn(mockPage);

        // when
        Page<SysRole> result = roleService.listPage(query);

        // then
        assertEquals(1, result.getTotal());
        assertEquals("管理员", result.getRecords().get(0).getName());
        verify(roleMapper).selectPageByCondition(any(), eq(query));
    }

    @Test
    void testGetById() {
        // given
        SysRole role = new SysRole();
        role.setId(1L);
        role.setName("管理员");
        when(roleMapper.selectById(1L)).thenReturn(role);

        // when
        SysRole result = roleService.getById(1L);

        // then
        assertNotNull(result);
        assertEquals("管理员", result.getName());
    }

    @Test
    void testSave() {
        // given
        SysRole role = new SysRole();
        role.setName("测试角色");
        when(roleMapper.insert(role)).thenReturn(1);

        // when
        roleService.save(role);

        // then
        verify(roleMapper).insert(role);
    }

    @Test
    void testRemoveById_shouldDeleteRoleAndMenus() {
        // when
        roleService.removeById(1L);

        // then
        verify(roleMapper).deleteById(1L);
        verify(roleMenuMapper).deleteByRoleId(1L);
    }

    @Test
    void testGetMenuIdsByRoleId() {
        // given
        SysRoleMenu rm1 = new SysRoleMenu();
        rm1.setRoleId(1L);
        rm1.setMenuId(100L);
        SysRoleMenu rm2 = new SysRoleMenu();
        rm2.setRoleId(1L);
        rm2.setMenuId(101L);
        when(roleMenuMapper.selectByRoleId(1L)).thenReturn(List.of(rm1, rm2));

        // when
        List<Long> menuIds = roleService.getMenuIdsByRoleId(1L);

        // then
        assertEquals(2, menuIds.size());
        assertTrue(menuIds.contains(100L));
        assertTrue(menuIds.contains(101L));
    }

    @Test
    void testAssignMenus() {
        // given
        List<Long> menuIds = List.of(100L, 101L, 102L);

        // when
        roleService.assignMenus(1L, menuIds);

        // then
        verify(roleMenuMapper).deleteByRoleId(1L);
        verify(roleMenuMapper, times(3)).insert(any(SysRoleMenu.class));
    }
}
