package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.po.SysDictTypeDO;
import com.nz.admin.modules.system.mapper.SysDictTypeMapper;
import com.nz.admin.modules.system.entity.query.SysDictTypeQuery;
import com.nz.admin.modules.system.service.impl.SysDictTypeServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static com.nz.admin.framework.test.core.util.RandomPojoUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(NzSystemTestApplication.class)
class SysDictTypeServiceImplTest extends BaseDbUnitTest {

    @Resource
    private SysDictTypeServiceImpl dictTypeService;
    @Resource
    private SysDictTypeMapper dictTypeMapper;

    @Test
    void testListPage() {
        SysDictTypeDO dictType1 = randomPojo(SysDictTypeDO.class)
                .setId(null)
                .setName("用户性别")
                .setType("sys_user_sex");
        dictTypeMapper.insert(dictType1);

        SysDictTypeDO dictType2 = randomPojo(SysDictTypeDO.class)
                .setId(null)
                .setName("通知类型")
                .setType("sys_notice_type");
        dictTypeMapper.insert(dictType2);

        SysDictTypeQuery query = new SysDictTypeQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setType("sys_user_sex");

        Page<SysDictTypeDO> result = dictTypeService.listPage(query);

        assertEquals(1, result.getTotal());
        assertEquals("sys_user_sex", result.getRecords().get(0).getType());
    }

    @Test
    void testSave() {
        SysDictTypeDO dictType = randomPojo(SysDictTypeDO.class)
                .setId(null)
                .setName("用户性别")
                .setType("sys_user_sex");

        dictTypeService.save(dictType);

        assertNotNull(dictType.getId());
        assertNotNull(dictTypeMapper.selectById(dictType.getId()));
    }

    @Test
    void testRemoveById() {
        SysDictTypeDO dictType = randomPojo(SysDictTypeDO.class)
                .setId(null)
                .setType("type_for_remove");
        dictTypeMapper.insert(dictType);

        dictTypeService.removeById(dictType.getId());

        assertNull(dictTypeMapper.selectById(dictType.getId()));
    }
}
