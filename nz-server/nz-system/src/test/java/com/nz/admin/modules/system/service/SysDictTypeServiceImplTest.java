package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.modules.system.entity.SysDictType;
import com.nz.admin.modules.system.mapper.SysDictTypeMapper;
import com.nz.admin.modules.system.query.SysDictTypeQuery;
import com.nz.admin.modules.system.service.impl.SysDictTypeServiceImpl;
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
class SysDictTypeServiceImplTest {

    @InjectMocks
    private SysDictTypeServiceImpl dictTypeService;
    @Mock
    private SysDictTypeMapper dictTypeMapper;

    @Test
    void testListPage() {
        // given
        SysDictTypeQuery query = new SysDictTypeQuery();
        query.setPageNum(1);
        query.setPageSize(10);

        SysDictType dt = new SysDictType();
        dt.setId(1L);
        dt.setName("用户性别");
        dt.setType("sys_user_sex");

        Page<SysDictType> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(List.of(dt));
        when(dictTypeMapper.selectPageByCondition(any(), eq(query))).thenReturn(mockPage);

        // when
        Page<SysDictType> result = dictTypeService.listPage(query);

        // then
        assertEquals(1, result.getTotal());
        assertEquals("sys_user_sex", result.getRecords().get(0).getType());
    }

    @Test
    void testSave() {
        // given
        SysDictType dt = new SysDictType();
        dt.setName("用户性别");
        dt.setType("sys_user_sex");
        when(dictTypeMapper.insert(dt)).thenReturn(1);

        // when
        dictTypeService.save(dt);

        // then
        verify(dictTypeMapper).insert(dt);
    }

    @Test
    void testRemoveById() {
        // when
        dictTypeService.removeById(1L);

        // then
        verify(dictTypeMapper).deleteById(1L);
    }
}
