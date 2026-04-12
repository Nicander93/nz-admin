package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysUser;
import com.nz.admin.modules.system.mapper.SysUserMapper;
import com.nz.admin.modules.system.query.SysUserQuery;
import com.nz.admin.modules.system.service.impl.SysUserServiceImpl;
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
class SysUserServiceImplTest {

    @InjectMocks
    private SysUserServiceImpl userService;
    @Mock
    private SysUserMapper userMapper;

    @Test
    void testListPage() {
        // given
        SysUserQuery query = new SysUserQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setUsername("admin");

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setNickname("管理员");

        Page<SysUser> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(List.of(user));
        when(userMapper.selectPageByCondition(any(), eq(query))).thenReturn(mockPage);

        // when
        Page<SysUser> result = userService.listPage(query);

        // then
        assertEquals(1, result.getTotal());
        assertEquals("admin", result.getRecords().get(0).getUsername());
    }

    @Test
    void testGetById() {
        // given
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        when(userMapper.selectById(1L)).thenReturn(user);

        // when
        SysUser result = userService.getById(1L);

        // then
        assertNotNull(result);
        assertEquals("admin", result.getUsername());
    }

    @Test
    void testGetByUsername() {
        // given
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        when(userMapper.selectByUsername("admin")).thenReturn(user);

        // when
        SysUser result = userService.getByUsername("admin");

        // then
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetByUsername_notFound() {
        // given
        when(userMapper.selectByUsername("nonexistent")).thenReturn(null);

        // when
        SysUser result = userService.getByUsername("nonexistent");

        // then
        assertNull(result);
    }

    @Test
    void testSave() {
        // given
        SysUser user = new SysUser();
        user.setUsername("newuser");
        when(userMapper.insert(user)).thenReturn(1);

        // when
        userService.save(user);

        // then
        verify(userMapper).insert(user);
    }

    @Test
    void testUpdateById() {
        // given
        SysUser user = new SysUser();
        user.setId(1L);
        user.setNickname("新昵称");
        when(userMapper.updateById(user)).thenReturn(1);

        // when
        userService.updateById(user);

        // then
        verify(userMapper).updateById(user);
    }

    @Test
    void testRemoveById() {
        // when
        userService.removeById(1L);

        // then
        verify(userMapper).deleteById(1L);
    }

    @Test
    void testCount() {
        // given
        when(userMapper.selectCount(null)).thenReturn(5L);

        // when
        long count = userService.count();

        // then
        assertEquals(5L, count);
    }
}
