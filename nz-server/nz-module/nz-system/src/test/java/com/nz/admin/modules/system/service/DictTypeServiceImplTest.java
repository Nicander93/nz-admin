package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.dataobject.dict.DictTypeDO;
import com.nz.admin.modules.system.mapper.dict.DictTypeMapper;
import com.nz.admin.modules.system.entity.query.dict.DictTypeQuery;
import com.nz.admin.modules.system.service.dict.DictTypeServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import static com.nz.admin.framework.test.core.util.RandomPojoUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(NzSystemTestApplication.class)
class DictTypeServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DictTypeServiceImpl dictTypeService;
    @Resource
    private DictTypeMapper dictTypeMapper;

    @Test
    void testListPage() {
        DictTypeDO dictType1 = randomPojo(DictTypeDO.class)
                .setId(null)
                .setName("用户性别")
                .setType("sys_user_sex");
        dictTypeMapper.insert(dictType1);

        DictTypeDO dictType2 = randomPojo(DictTypeDO.class)
                .setId(null)
                .setName("通知类型")
                .setType("sys_notice_type");
        dictTypeMapper.insert(dictType2);

        DictTypeQuery query = new DictTypeQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setType("sys_user_sex");

        Page<DictTypeDO> result = dictTypeService.listPage(query);

        assertEquals(1, result.getTotal());
        assertEquals("sys_user_sex", result.getRecords().get(0).getType());
    }

    @Test
    void testSave() {
        DictTypeDO dictType = randomPojo(DictTypeDO.class)
                .setId(null)
                .setName("用户性别")
                .setType("sys_user_sex");

        dictTypeService.save(dictType);

        assertNotNull(dictType.getId());
        assertNotNull(dictTypeMapper.selectById(dictType.getId()));
    }

    @Test
    void testRemoveById() {
        DictTypeDO dictType = randomPojo(DictTypeDO.class)
                .setId(null)
                .setType("type_for_remove");
        dictTypeMapper.insert(dictType);

        dictTypeService.removeById(dictType.getId());

        assertNull(dictTypeMapper.selectById(dictType.getId()));
    }
}
