package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.dataobject.log.OperLogDO;
import com.nz.admin.modules.system.mapper.log.OperLogMapper;
import com.nz.admin.modules.system.entity.query.log.OperLogQuery;
import com.nz.admin.modules.system.service.log.OperLogServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.util.List;

import static com.nz.admin.framework.test.core.util.RandomPojoUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(NzSystemTestApplication.class)
class OperLogServiceImplTest extends BaseDbUnitTest {

    @Resource
    private OperLogServiceImpl operLogService;
    @Resource
    private OperLogMapper operLogMapper;

    @Test
    void testListPage() {
        OperLogDO operLog1 = randomPojo(OperLogDO.class)
                .setId(null)
                .setTitle("用户管理")
                .setOperName("alice")
                .setBusinessType(1)
                .setStatus(0);
        operLogMapper.insert(operLog1);

        OperLogDO operLog2 = randomPojo(OperLogDO.class)
                .setId(null)
                .setTitle("角色管理")
                .setOperName("bob")
                .setBusinessType(2)
                .setStatus(1);
        operLogMapper.insert(operLog2);

        OperLogQuery query = new OperLogQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setTitle("用户管理");
        query.setOperName("alice");
        query.setBusinessType(1);
        query.setStatus(0);

        Page<OperLogDO> result = operLogService.listPage(query);

        assertEquals(1, result.getTotal());
        assertEquals("用户管理", result.getRecords().get(0).getTitle());
    }

    @Test
    void testGetById() {
        OperLogDO operLog = randomPojo(OperLogDO.class)
                .setId(null)
                .setTitle("按ID查询操作日志");
        operLogMapper.insert(operLog);

        OperLogDO result = operLogService.getById(operLog.getId());

        assertNotNull(result);
        assertEquals("按ID查询操作日志", result.getTitle());
    }

    @Test
    void testRemoveByIds() {
        OperLogDO operLog1 = randomPojo(OperLogDO.class)
                .setId(null)
                .setTitle("批量删除1");
        operLogMapper.insert(operLog1);

        OperLogDO operLog2 = randomPojo(OperLogDO.class)
                .setId(null)
                .setTitle("批量删除2");
        operLogMapper.insert(operLog2);

        operLogService.removeByIds(List.of(operLog1.getId(), operLog2.getId()));

        assertNull(operLogMapper.selectById(operLog1.getId()));
        assertNull(operLogMapper.selectById(operLog2.getId()));
    }
}
