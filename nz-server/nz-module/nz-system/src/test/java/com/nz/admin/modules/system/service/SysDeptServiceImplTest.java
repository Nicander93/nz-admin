package com.nz.admin.modules.system.service;

import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.SysDept;
import com.nz.admin.modules.system.mapper.SysDeptMapper;
import com.nz.admin.modules.system.service.impl.SysDeptServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(NzSystemTestApplication.class)
class SysDeptServiceImplTest extends BaseDbUnitTest {

    @Resource
    private SysDeptServiceImpl deptService;

    @Resource
    private SysDeptMapper deptMapper;

    @Test
    void testListAll() {
        SysDept dept1 = new SysDept()
                .setName("总公司")
                .setSort(2);
        deptMapper.insert(dept1);

        SysDept dept2 = new SysDept()
                .setName("技术部")
                .setSort(1);
        deptMapper.insert(dept2);

        List<SysDept> result = deptService.listAll();

        assertEquals(2, result.size());
        assertEquals("技术部", result.get(0).getName());
        assertEquals("总公司", result.get(1).getName());
    }

    @Test
    void testGetById() {
        SysDept dept = new SysDept()
                .setName("总公司")
                .setSort(1);
        deptMapper.insert(dept);

        SysDept result = deptService.getById(dept.getId());

        assertNotNull(result);
        assertEquals("总公司", result.getName());
    }

    @Test
    void testSave() {
        SysDept dept = new SysDept()
                .setName("新部门")
                .setSort(10);
        deptService.save(dept);

        assertNotNull(dept.getId());
        SysDept dbDept = deptMapper.selectById(dept.getId());
        assertNotNull(dbDept);
        assertEquals("新部门", dbDept.getName());
    }

    @Test
    void testRemoveById() {
        SysDept dept = new SysDept()
                .setName("待删除部门")
                .setSort(3);
        deptMapper.insert(dept);

        deptService.removeById(dept.getId());

        assertNull(deptMapper.selectById(dept.getId()));
    }
}
