package com.nz.admin.modules.system.service;

import com.nz.admin.modules.system.entity.SysMenu;
import com.nz.admin.modules.system.mapper.SysMenuMapper;
import com.nz.admin.modules.system.service.impl.SysMenuServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysMenuServiceImplTest {

    @InjectMocks
    private SysMenuServiceImpl menuService;
    @Mock
    private SysMenuMapper menuMapper;

    @Test
    void testListAll() {
        // given
        SysMenu menu1 = new SysMenu();
        menu1.setId(1L);
        menu1.setName("系统管理");
        menu1.setType("M");
        SysMenu menu2 = new SysMenu();
        menu2.setId(2L);
        menu2.setName("用户管理");
        menu2.setType("C");
        menu2.setPerm("system:user:list");
        when(menuMapper.selectListOrderBySort()).thenReturn(List.of(menu1, menu2));

        // when
        List<SysMenu> result = menuService.listAll();

        // then
        assertEquals(2, result.size());
        assertEquals("系统管理", result.get(0).getName());
        assertEquals("C", result.get(1).getType());
    }

    @Test
    void testSave() {
        // given
        SysMenu menu = new SysMenu();
        menu.setName("新菜单");
        menu.setType("C");
        when(menuMapper.insert(menu)).thenReturn(1);

        // when
        menuService.save(menu);

        // then
        verify(menuMapper).insert(menu);
    }

    @Test
    void testRemoveById() {
        // when
        menuService.removeById(1L);

        // then
        verify(menuMapper).deleteById(1L);
    }
}
