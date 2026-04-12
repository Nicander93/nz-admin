package com.nz.admin.modules.system.service;

import com.nz.admin.modules.system.entity.SysDept;
import com.nz.admin.modules.system.mapper.SysDeptMapper;
import com.nz.admin.modules.system.service.impl.SysDeptServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysDeptServiceImplTest {

    @InjectMocks
    private SysDeptServiceImpl deptService;
    @Mock
    private SysDeptMapper deptMapper;

    @Test
    void testListAll() {
        // given
        SysDept dept1 = new SysDept();
        dept1.setId(1L);
        dept1.setName("总公司");
        SysDept dept2 = new SysDept();
        dept2.setId(2L);
        dept2.setName("技术部");
        when(deptMapper.selectListOrderBySort()).thenReturn(List.of(dept1, dept2));

        // when
        List<SysDept> result = deptService.listAll();

        // then
        assertEquals(2, result.size());
        assertEquals("总公司", result.get(0).getName());
    }

    @Test
    void testGetById() {
        // given
        SysDept dept = new SysDept();
        dept.setId(1L);
        dept.setName("总公司");
        when(deptMapper.selectById(1L)).thenReturn(dept);

        // when
        SysDept result = deptService.getById(1L);

        // then
        assertNotNull(result);
        assertEquals("总公司", result.getName());
    }

    @Test
    void testSave() {
        // given
        SysDept dept = new SysDept();
        dept.setName("新部门");
        when(deptMapper.insert(dept)).thenReturn(1);

        // when
        deptService.save(dept);

        // then
        verify(deptMapper).insert(dept);
    }

    @Test
    void testRemoveById() {
        // when
        deptService.removeById(1L);

        // then
        verify(deptMapper).deleteById(1L);
    }
}
