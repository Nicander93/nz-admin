package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.po.SysRoleDO;
import com.nz.admin.modules.system.entity.po.SysRoleMenuDO;
import com.nz.admin.modules.system.mapper.SysRoleMapper;
import com.nz.admin.modules.system.mapper.SysRoleMenuMapper;
import com.nz.admin.modules.system.entity.query.SysRoleQuery;
import com.nz.admin.modules.system.service.impl.SysRoleServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.util.List;

import static com.nz.admin.framework.test.core.util.RandomPojoUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(NzSystemTestApplication.class)
class SysRoleServiceImplTest extends BaseDbUnitTest {

    @Resource
    private SysRoleServiceImpl roleService;
    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private SysRoleMenuMapper roleMenuMapper;

    @Test
    void testListPage() {
        SysRoleDO role1 = randomPojo(SysRoleDO.class)
                .setId(null)
                .setName("管理员")
                .setRoleKey("admin_test_key")
                .setSort(1);
        roleMapper.insert(role1);

        SysRoleDO role2 = randomPojo(SysRoleDO.class)
                .setId(null)
                .setName("普通角色")
                .setRoleKey("normal_test_key")
                .setSort(2);
        roleMapper.insert(role2);

        SysRoleQuery query = new SysRoleQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setName("管理员");

        Page<SysRoleDO> result = roleService.listPage(query);

        assertEquals(1, result.getTotal());
        assertEquals("管理员", result.getRecords().get(0).getName());
    }

    @Test
    void testGetById() {
        SysRoleDO role = randomPojo(SysRoleDO.class)
                .setId(null)
                .setName("角色_查询");
        roleMapper.insert(role);

        SysRoleDO result = roleService.getById(role.getId());

        assertNotNull(result);
        assertEquals("角色_查询", result.getName());
    }

    @Test
    void testSave() {
        SysRoleDO role = randomPojo(SysRoleDO.class)
                .setId(null)
                .setName("测试角色");

        roleService.save(role);

        assertNotNull(role.getId());
        assertNotNull(roleMapper.selectById(role.getId()));
    }

    @Test
    void testRemoveById_shouldDeleteRoleAndMenus() {
        SysRoleDO role = randomPojo(SysRoleDO.class)
                .setId(null)
                .setName("待删除角色");
        roleMapper.insert(role);

        SysRoleMenuDO rm = randomPojo(SysRoleMenuDO.class)
                .setRoleId(role.getId())
                .setMenuId(100L);
        roleMenuMapper.insert(rm);

        roleService.removeById(role.getId());

        assertNull(roleMapper.selectById(role.getId()));
        assertTrue(roleMenuMapper.selectByRoleId(role.getId()).isEmpty());
    }

    @Test
    void testGetMenuIdsByRoleId() {
        SysRoleDO role = randomPojo(SysRoleDO.class)
                .setId(null);
        roleMapper.insert(role);

        SysRoleMenuDO rm1 = randomPojo(SysRoleMenuDO.class)
                .setRoleId(role.getId())
                .setMenuId(100L);
        roleMenuMapper.insert(rm1);

        SysRoleMenuDO rm2 = randomPojo(SysRoleMenuDO.class)
                .setRoleId(role.getId())
                .setMenuId(101L);
        roleMenuMapper.insert(rm2);

        List<Long> menuIds = roleService.getMenuIdsByRoleId(role.getId());

        assertEquals(2, menuIds.size());
        assertTrue(menuIds.contains(100L));
        assertTrue(menuIds.contains(101L));
    }

    @Test
    void testAssignMenus() {
        SysRoleDO role = randomPojo(SysRoleDO.class)
                .setId(null);
        roleMapper.insert(role);

        SysRoleMenuDO old = randomPojo(SysRoleMenuDO.class)
                .setRoleId(role.getId())
                .setMenuId(99L);
        roleMenuMapper.insert(old);

        List<Long> menuIds = List.of(100L, 101L, 102L);
        roleService.assignMenus(role.getId(), menuIds);

        List<SysRoleMenuDO> dbRows = roleMenuMapper.selectByRoleId(role.getId());
        assertEquals(3, dbRows.size());
        assertTrue(dbRows.stream().anyMatch(item -> item.getMenuId().equals(100L)));
        assertTrue(dbRows.stream().anyMatch(item -> item.getMenuId().equals(101L)));
        assertTrue(dbRows.stream().anyMatch(item -> item.getMenuId().equals(102L)));
    }
}
