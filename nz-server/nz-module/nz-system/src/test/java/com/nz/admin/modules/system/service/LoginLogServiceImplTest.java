package com.nz.admin.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nz.admin.NzSystemTestApplication;
import com.nz.admin.framework.test.core.ut.BaseDbUnitTest;
import com.nz.admin.modules.system.entity.dataobject.log.LoginLogDO;
import com.nz.admin.modules.system.mapper.log.LoginLogMapper;
import com.nz.admin.modules.system.entity.query.log.LoginLogQuery;
import com.nz.admin.modules.system.service.log.LoginLogServiceImpl;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;
import java.time.Duration;

import static com.nz.admin.framework.test.core.util.RandomPojoUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(NzSystemTestApplication.class)
class LoginLogServiceImplTest extends BaseDbUnitTest {

    @Resource
    private LoginLogServiceImpl loginLogService;
    @Resource
    private LoginLogMapper loginLogMapper;

    @Test
    void testListPage() {
        LoginLogDO loginLog1 = randomPojo(LoginLogDO.class)
                .setId(null)
                .setUsername("admin_login_page")
                .setIp("192.168.10.1")
                .setStatus(0);
        loginLogMapper.insert(loginLog1);

        LoginLogDO loginLog2 = randomPojo(LoginLogDO.class)
                .setId(null)
                .setUsername("guest_login_page")
                .setIp("10.0.0.1")
                .setStatus(1);
        loginLogMapper.insert(loginLog2);

        LoginLogQuery query = new LoginLogQuery();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setUsername("admin_login_page");
        query.setIp("192.168.10.1");
        query.setStatus(0);

        Page<LoginLogDO> result = loginLogService.listPage(query);

        assertEquals(1, result.getTotal());
        assertEquals("admin_login_page", result.getRecords().get(0).getUsername());
    }

    @Test
    void testGetById() {
        LoginLogDO loginLog = randomPojo(LoginLogDO.class)
                .setId(null)
                .setUsername("admin_get_login_log");
        loginLogMapper.insert(loginLog);

        LoginLogDO result = loginLogService.getById(loginLog.getId());

        assertNotNull(result);
        assertEquals("admin_get_login_log", result.getUsername());
    }

    @Test
    void testSaveAsync() {
        LoginLogDO loginLog = randomPojo(LoginLogDO.class)
                .setId(null)
                .setUsername("admin_save_async");

        loginLogService.saveAsync(loginLog);

        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    assertNotNull(loginLog.getId());
                    assertNotNull(loginLogMapper.selectById(loginLog.getId()));
                });
    }
}
