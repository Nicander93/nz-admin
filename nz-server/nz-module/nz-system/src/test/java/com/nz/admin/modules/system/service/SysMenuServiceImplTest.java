package com.nz.admin.modules.system.service;

import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.po.SysMenuDO;
import com.nz.admin.modules.system.mapper.SysMenuMapper;
import com.nz.admin.modules.system.service.impl.SysMenuServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.util.List;

import static com.nz.admin.framework.test.core.util.RandomPojoUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(NzSystemTestApplication.class)
class SysMenuServiceImplTest extends BaseDbUnitTest {

    @Resource
    private SysMenuServiceImpl menuService;
    @Resource
    private SysMenuMapper menuMapper;

    @Test
    void testListAll() {
        SysMenuDO menu1 = randomPojo(SysMenuDO.class)
                .setId(null)
                .setName("系统管理")
                .setType("M")
                .setSort(2);
        menuMapper.insert(menu1);

        SysMenuDO menu2 = randomPojo(SysMenuDO.class)
                .setId(null)
                .setName("用户管理")
                .setType("C")
                .setPerm("system:user:list")
                .setSort(1);
        menuMapper.insert(menu2);

        List<SysMenuDO> result = menuService.listAll();

        assertEquals(2, result.size());
        assertEquals("用户管理", result.get(0).getName());
        assertEquals("系统管理", result.get(1).getName());
    }

    @Test
    void testSave() {
        SysMenuDO menu = randomPojo(SysMenuDO.class)
                .setId(null)
                .setName("新菜单")
                .setType("C");

        menuService.save(menu);

        assertNotNull(menu.getId());
        assertNotNull(menuMapper.selectById(menu.getId()));
    }

    @Test
    void testRemoveById() {
        SysMenuDO menu = randomPojo(SysMenuDO.class)
                .setId(null)
                .setName("待删除菜单");
        menuMapper.insert(menu);

        menuService.removeById(menu.getId());

        assertNull(menuMapper.selectById(menu.getId()));
    }
}
