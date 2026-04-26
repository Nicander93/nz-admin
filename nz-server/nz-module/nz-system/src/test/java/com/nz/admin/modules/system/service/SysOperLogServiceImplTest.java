package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.SysOperLog;
import com.nz.admin.modules.system.mapper.SysOperLogMapper;
import com.nz.admin.modules.system.query.SysOperLogQuery;
import com.nz.admin.modules.system.service.impl.SysOperLogServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.util.List;

import static com.nz.admin.framework.test.core.util.RandomPojoUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Import(NzSystemTestApplication.class)
class SysOperLogServiceImplTest extends BaseDbUnitTest {

    @Resource
    private SysOperLogServiceImpl operLogService;
    @Resource
    private SysOperLogMapper operLogMapper;

    @Test
    void testListPage() {
        SysOperLog operLog1 = randomPojo(SysOperLog.class)
                .setId(null)
                .setTitle("用户管理")
                .setOperName("alice")
                .setBusinessType(1)
                .setStatus(0);
        operLogMapper.insert(operLog1);

        SysOperLog operLog2 = randomPojo(SysOperLog.class)
                .setId(null)
                .setTitle("角色管理")
                .setOperName("bob")
                .setBusinessType(2)
                .setStatus(1);
        operLogMapper.insert(operLog2);

        SysOperLogQuery query = new SysOperLogQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setTitle("用户管理");
        query.setOperName("alice");
        query.setBusinessType(1);
        query.setStatus(0);

        Page<SysOperLog> result = operLogService.listPage(query);

        assertEquals(1, result.getTotal());
        assertEquals("用户管理", result.getRecords().get(0).getTitle());
    }

    @Test
    void testGetById() {
        SysOperLog operLog = randomPojo(SysOperLog.class)
                .setId(null)
                .setTitle("按ID查询操作日志");
        operLogMapper.insert(operLog);

        SysOperLog result = operLogService.getById(operLog.getId());

        assertNotNull(result);
        assertEquals("按ID查询操作日志", result.getTitle());
    }

    @Test
    void testRemoveByIds() {
        SysOperLog operLog1 = randomPojo(SysOperLog.class)
                .setId(null)
                .setTitle("批量删除1");
        operLogMapper.insert(operLog1);

        SysOperLog operLog2 = randomPojo(SysOperLog.class)
                .setId(null)
                .setTitle("批量删除2");
        operLogMapper.insert(operLog2);

        operLogService.removeByIds(List.of(operLog1.getId(), operLog2.getId()));

        assertNull(operLogMapper.selectById(operLog1.getId()));
        assertNull(operLogMapper.selectById(operLog2.getId()));
    }
}
