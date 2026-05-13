package com.nz.admin.modules.system.service;

import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.dataobject.dept.DeptDO;
import com.nz.admin.modules.system.mapper.dept.DeptMapper;
import com.nz.admin.modules.system.service.dept.DeptServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(NzSystemTestApplication.class)
class DeptServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DeptServiceImpl deptService;

    @Resource
    private DeptMapper deptMapper;

    @Test
    void testListAll() {
        DeptDO dept1 = new DeptDO()
                .setName("总公司")
                .setSort(2);
        deptMapper.insert(dept1);

        DeptDO dept2 = new DeptDO()
                .setName("技术部")
                .setSort(1);
        deptMapper.insert(dept2);

        List<DeptDO> result = deptService.listAll();

        assertEquals(2, result.size());
        assertEquals("技术部", result.get(0).getName());
        assertEquals("总公司", result.get(1).getName());
    }

    @Test
    void testGetById() {
        DeptDO dept = new DeptDO()
                .setName("总公司")
                .setSort(1);
        deptMapper.insert(dept);

        DeptDO result = deptService.getById(dept.getId());

        assertNotNull(result);
        assertEquals("总公司", result.getName());
    }

    @Test
    void testSave() {
        DeptDO dept = new DeptDO()
                .setName("新部门")
                .setSort(10);
        deptService.save(dept);

        assertNotNull(dept.getId());
        DeptDO dbDept = deptMapper.selectById(dept.getId());
        assertNotNull(dbDept);
        assertEquals("新部门", dbDept.getName());
    }

    @Test
    void testRemoveById() {
        DeptDO dept = new DeptDO()
                .setName("待删除部门")
                .setSort(3);
        deptMapper.insert(dept);

        deptService.removeById(dept.getId());

        assertNull(deptMapper.selectById(dept.getId()));
    }
}
