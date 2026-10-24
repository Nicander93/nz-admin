package com.nz.admin;

import cn.hutool.crypto.digest.BCrypt;
import com.nz.admin.common.job.JobTask;
import com.nz.admin.framework.test.core.http.ApiTestClient;
import com.nz.admin.modules.job.service.job.JobSchedulerInitializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** PostgreSQL HTTP 与真实 Quartz 调度回归；NZ_TEST_PG_URL 必须指向隔离测试库。 */
@EnabledIfEnvironmentVariable(named = "NZ_TEST_PG_URL", matches = ".+")
@SpringBootTest(classes = NzAdminApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("pg-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(JobReadinessPostgresTest.ReadinessTaskConfiguration.class)
class JobReadinessPostgresTest {
    private static final String PREFIX = "job-readiness-";
    private static final String CACHE_KEY_PREFIX = "job-readiness-test-" + UUID.randomUUID();
    private static final long OWNER_TENANT_ID = 990001L;
    private static final long OTHER_TENANT_ID = 990002L;
    private static final long STALE_TENANT_ID = 990003L;
    private static final long OWNER_USER_ID = 991001L;
    private static final long OTHER_USER_ID = 991002L;
    private static final long STALE_USER_ID = 991003L;
    private static final long OWNER_ROLE_ID = 992001L;
    private static final long OTHER_ROLE_ID = 992002L;
    private static final long STALE_ROLE_ID = 992003L;
    private static final long OTHER_JOB_ID = 990022L;
    private static final long RECOVERY_JOB_ID = 990011L;
    private static final long EXPIRED_TENANT_ID = 990011L;
    private static final long EXPIRED_JOB_ID = 990012L;

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired Scheduler scheduler;
    @Autowired JobSchedulerInitializer schedulerInitializer;

    private ApiTestClient ownerApi;
    private ApiTestClient otherApi;
    private ApiTestClient staleApi;

    @DynamicPropertySource
    static void isolateRedisKeys(DynamicPropertyRegistry registry) {
        registry.add("nz.cache.key-prefix", () -> CACHE_KEY_PREFIX);
    }

    @BeforeAll
    void createIsolatedPrincipalsAndLogin() {
        cleanupFixtures();
        seedTenant(OWNER_TENANT_ID, "job-readiness-owner");
        seedTenant(OTHER_TENANT_ID, "job-readiness-other");
        seedTenant(STALE_TENANT_ID, "job-readiness-stale");
        seedPrincipal(OWNER_TENANT_ID, OWNER_USER_ID, OWNER_ROLE_ID, "job-readiness-owner");
        seedPrincipal(OTHER_TENANT_ID, OTHER_USER_ID, OTHER_ROLE_ID, "job-readiness-other");
        seedPrincipal(STALE_TENANT_ID, STALE_USER_ID, STALE_ROLE_ID, "job-readiness-stale");
        ownerApi = login("job-readiness-owner");
        otherApi = login("job-readiness-other");
        staleApi = login("job-readiness-stale");
    }

    @BeforeEach
    void setUp() throws Exception {
        clearScheduler();
        cleanupJobs();
    }

    @AfterEach
    void cleanUp() throws Exception {
        clearScheduler();
        cleanupJobs();
    }

    @AfterAll
    void removeIsolatedPrincipals() { cleanupFixtures(); }

    @Test
    @Order(1)
    void createAndUpdateIgnoreForgedTenantAndRunOnceTriggersTask() throws Exception {
        Map<String, Object> body = request("job-readiness-trigger", "demoTask.run");
        body.put("tenantId", OTHER_TENANT_ID);
        ownerApi.ok(HttpMethod.POST, "/api/system/job", body);
        Long id = jobId("job-readiness-trigger");
        assertTenant(id, OWNER_TENANT_ID);

        Map<String, Object> update = request("job-readiness-updated", "demoTask.run");
        update.put("id", id);
        update.put("tenantId", OTHER_TENANT_ID);
        ownerApi.ok(HttpMethod.PUT, "/api/system/job", update);
        assertTenant(id, OWNER_TENANT_ID);
        assertThat(jdbc.queryForObject("SELECT job_name FROM sys_job WHERE id=?", String.class, id)).isEqualTo("job-readiness-updated");

        ownerApi.ok(HttpMethod.PUT, "/api/system/job/run/" + id, null);
        awaitSuccessfulLog(id);
        assertThat(jdbc.queryForObject("SELECT job_id FROM sys_job_log WHERE job_id=? AND status=0 ORDER BY id DESC LIMIT 1", Long.class, id)).isEqualTo(id);
        assertThat(jdbc.queryForObject("SELECT tenant_id FROM sys_job_log WHERE job_id=? AND status=0 ORDER BY id DESC LIMIT 1", Long.class, id)).isEqualTo(OWNER_TENANT_ID);
        assertThat(jdbc.queryForObject("SELECT job_name FROM sys_job_log WHERE job_id=? AND status=0 ORDER BY id DESC LIMIT 1", String.class, id)).isEqualTo("job-readiness-updated");
    }

    @Test
    @Order(2)
    void productionStartupRunnerRecoversPersistedJobsAndSkipsExpiredTenant() throws Exception {
        jdbc.update("INSERT INTO sys_job(id,tenant_id,job_name,job_group,invoke_target,cron_expression,status,concurrent,misfire_policy) VALUES(?,1,?,?,?,'0 0/5 * * * ?',0,1,3)", RECOVERY_JOB_ID, PREFIX + "recovery", "DEFAULT", "demoTask.run");
        jdbc.update("INSERT INTO sys_tenant(id,tenant_code,tenant_name,package_id,account_count,status,expire_time) VALUES(?,'job-readiness-expired','Job expired',1,10,0,CURRENT_TIMESTAMP - INTERVAL '1 day') ON CONFLICT(id) DO NOTHING", EXPIRED_TENANT_ID);
        jdbc.update("INSERT INTO sys_job(id,tenant_id,job_name,job_group,invoke_target,cron_expression,status,concurrent,misfire_policy) VALUES(?,?,?,?,'demoTask.run','0 0/5 * * * ?',0,1,3)", EXPIRED_JOB_ID, EXPIRED_TENANT_ID, PREFIX + "expired", "DEFAULT");
        scheduler.clear();
        schedulerInitializer.run(new DefaultApplicationArguments(new String[0]));
        assertThat(scheduler.checkExists(new JobKey(String.valueOf(RECOVERY_JOB_ID)))).isTrue();
        assertThat(scheduler.checkExists(new JobKey(String.valueOf(EXPIRED_JOB_ID)))).isFalse();
        scheduler.clear();
        schedulerInitializer.run(new DefaultApplicationArguments(new String[0]));
        assertThat(scheduler.checkExists(new JobKey(String.valueOf(RECOVERY_JOB_ID)))).isTrue();
    }

    @Test
    @Order(3)
    void rejectsCrossTenantReadExecutionUpdatePauseResumeAndDelete() {
        jdbc.update("INSERT INTO sys_job(id,tenant_id,job_name,job_group,invoke_target,cron_expression,status,concurrent,misfire_policy) VALUES(?,?,?,'DEFAULT','demoTask.run','0 0/5 * * * ?',1,1,3)", OTHER_JOB_ID, OTHER_TENANT_ID, PREFIX + "foreign");
        assertDenied(ownerApi.exchange(HttpMethod.GET, "/api/system/job/" + OTHER_JOB_ID, null));
        assertDenied(ownerApi.exchange(HttpMethod.PUT, "/api/system/job/run/" + OTHER_JOB_ID, null));
        assertDenied(ownerApi.exchange(HttpMethod.DELETE, "/api/system/job/" + OTHER_JOB_ID, null));

        ownerApi.ok(HttpMethod.POST, "/api/system/job", request(PREFIX + "owner-only", "demoTask.run"));
        Long ownerJobId = jobId(PREFIX + "owner-only");
        Map<String, Object> forgedUpdate = request(PREFIX + "forged-update", "demoTask.run");
        forgedUpdate.put("id", ownerJobId);
        forgedUpdate.put("tenantId", OTHER_TENANT_ID);
        assertDenied(otherApi.exchange(HttpMethod.PUT, "/api/system/job", forgedUpdate));
        assertDenied(otherApi.exchange(HttpMethod.PUT, "/api/system/job/pause/" + ownerJobId, null));
        assertDenied(otherApi.exchange(HttpMethod.PUT, "/api/system/job/resume/" + ownerJobId, null));
        assertThat(jdbc.queryForObject("SELECT tenant_id FROM sys_job WHERE id=?", Long.class, ownerJobId)).isEqualTo(OWNER_TENANT_ID);
        assertThat(jdbc.queryForObject("SELECT job_name FROM sys_job WHERE id=?", String.class, ownerJobId)).isEqualTo(PREFIX + "owner-only");
        assertThat(jdbc.queryForObject("SELECT status FROM sys_job WHERE id=?", Integer.class, ownerJobId)).isEqualTo(1);
    }

    @Test
    @Order(4)
    void rejectsInvalidUnmarkedAndEmptyRequests() {
        assertDenied(ownerApi.exchange(HttpMethod.POST, "/api/system/job", request(PREFIX + "invalid", "demoTask.run() ")));
        assertDenied(ownerApi.exchange(HttpMethod.POST, "/api/system/job", request(PREFIX + "unmarked", "readinessFailTask.notMarked")));
        assertDenied(ownerApi.exchange(HttpMethod.POST, "/api/system/job", Map.of()));
        assertDenied(ownerApi.exchange(HttpMethod.PUT, "/api/system/job", request(PREFIX + "missing-id", "demoTask.run")));
    }

    @Test
    @Order(5)
    void failedExecutionPersistsCorrelatedFailureLog() {
        String name = PREFIX + "failure";
        ownerApi.ok(HttpMethod.POST, "/api/system/job", request(name, "readinessFailTask.fail"));
        Long id = jobId(name);
        assertDenied(ownerApi.exchange(HttpMethod.PUT, "/api/system/job/run/" + id, null));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_job_log WHERE job_id=? AND tenant_id=? AND job_name=? AND job_group='DEFAULT' AND invoke_target='readinessFailTask.fail' AND status=1 AND exception_info LIKE '%intentional failure%'", Integer.class, id, OWNER_TENANT_ID, name)).isEqualTo(1);
    }

    @Test
    @Order(6)
    void oldSessionCannotRunOrWriteAfterTenantIsDisabledOrExpired() {
        String name = PREFIX + "stale-session";
        staleApi.ok(HttpMethod.POST, "/api/system/job", request(name, "demoTask.run"));
        Long id = jobId(name);
        assertThat(countLogs(id)).isZero();

        jdbc.update("UPDATE sys_tenant SET status=1 WHERE id=?", STALE_TENANT_ID);
        assertDenied(staleApi.exchange(HttpMethod.PUT, "/api/system/job/run/" + id, null));
        assertDenied(staleApi.exchange(HttpMethod.POST, "/api/system/job", request(PREFIX + "disabled-write", "demoTask.run")));
        assertThat(countLogs(id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_job WHERE job_name=?", Integer.class, PREFIX + "disabled-write")).isZero();

        jdbc.update("UPDATE sys_tenant SET status=0,expire_time=CURRENT_TIMESTAMP - INTERVAL '1 day' WHERE id=?", STALE_TENANT_ID);
        assertDenied(staleApi.exchange(HttpMethod.PUT, "/api/system/job/run/" + id, null));
        assertDenied(staleApi.exchange(HttpMethod.POST, "/api/system/job", request(PREFIX + "expired-write", "demoTask.run")));
        assertThat(countLogs(id)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_job WHERE job_name=?", Integer.class, PREFIX + "expired-write")).isZero();
    }

    @Test
    @Order(99)
    void schedulerFailureReturnsHttpErrorAndRollsBackTheJobInsert() throws Exception {
        scheduler.shutdown();
        Map<String, Object> request = request(PREFIX + "scheduler-failure", "demoTask.run");
        request.put("status", 0);
        assertDenied(ownerApi.exchange(HttpMethod.POST, "/api/system/job", request));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_job WHERE job_name=?", Integer.class, PREFIX + "scheduler-failure")).isZero();
    }

    @TestConfiguration
    static class ReadinessTaskConfiguration {
        @Bean("readinessFailTask")
        ReadinessFailTask readinessFailTask() { return new ReadinessFailTask(); }
    }

    static class ReadinessFailTask {
        @JobTask
        public void fail() { throw new IllegalStateException("intentional failure " + "x".repeat(1200)); }
        public void notMarked() { }
    }

    private ApiTestClient login(String tenantCode) {
        return new ApiTestClient(rest).login(tenantCode, tenantCode, "job-readiness-password");
    }

    private void seedTenant(long id, String tenantCode) {
        jdbc.update("INSERT INTO sys_tenant(id,tenant_code,tenant_name,package_id,account_count,status) VALUES(?,?,?,1,10,0) ON CONFLICT(id) DO UPDATE SET tenant_code=EXCLUDED.tenant_code,tenant_name=EXCLUDED.tenant_name,package_id=EXCLUDED.package_id,account_count=EXCLUDED.account_count,status=0,expire_time=NULL",
                id, tenantCode, tenantCode);
    }

    private void seedPrincipal(long tenantId, long userId, long roleId, String username) {
        jdbc.update("INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope) VALUES(?,?,?, ?,0,0,1) ON CONFLICT(id) DO UPDATE SET tenant_id=EXCLUDED.tenant_id,name=EXCLUDED.name,role_key=EXCLUDED.role_key,status=0",
                roleId, tenantId, username + " role", username + "-role");
        jdbc.update("INSERT INTO sys_user(id,tenant_id,dept_id,username,password,nickname,status) VALUES(?,?,NULL,?,?,?,0) ON CONFLICT(id) DO UPDATE SET tenant_id=EXCLUDED.tenant_id,username=EXCLUDED.username,password=EXCLUDED.password,nickname=EXCLUDED.nickname,status=0",
                userId, tenantId, username, BCrypt.hashpw("job-readiness-password"), username);
        jdbc.update("INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES(?,?,?) ON CONFLICT DO NOTHING", tenantId, userId, roleId);
        jdbc.update("INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) SELECT ?, ?, id FROM sys_menu WHERE perm IN ('system:job:list','system:job:query','system:job:add','system:job:edit','system:job:remove') ON CONFLICT DO NOTHING",
                tenantId, roleId);
    }

    private Map<String, Object> request(String name, String target) {
        return new HashMap<>(Map.of("jobName", name, "jobGroup", "DEFAULT", "invokeTarget", target,
                "cronExpression", "0 0/5 * * * ?", "status", 1, "concurrent", 1, "misfirePolicy", 3));
    }

    private Long jobId(String name) {
        return jdbc.queryForObject("SELECT id FROM sys_job WHERE job_name=?", Long.class, name);
    }

    private void assertTenant(long jobId, long expectedTenantId) {
        assertThat(jdbc.queryForObject("SELECT tenant_id FROM sys_job WHERE id=?", Long.class, jobId)).isEqualTo(expectedTenantId);
    }

    private int countLogs(long jobId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sys_job_log WHERE job_id=?", Integer.class, jobId);
    }

    private void awaitSuccessfulLog(long jobId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline && countLogs(jobId) == 0) Thread.sleep(50);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_job_log WHERE job_id=? AND status=0", Integer.class, jobId)).isGreaterThan(0);
    }

    private void assertDenied(com.fasterxml.jackson.databind.JsonNode response) {
        assertThat(response.path("code").asInt()).isNotEqualTo(200);
    }

    private void clearScheduler() throws org.quartz.SchedulerException {
        if (!scheduler.isShutdown()) scheduler.clear();
    }

    private void cleanupJobs() {
        jdbc.update("DELETE FROM sys_job_log WHERE job_id IN (SELECT id FROM sys_job WHERE job_name LIKE ?) OR job_id IN (?,?,?)",
                PREFIX + "%", OTHER_JOB_ID, RECOVERY_JOB_ID, EXPIRED_JOB_ID);
        jdbc.update("DELETE FROM sys_job WHERE job_name LIKE ? OR id IN (?,?,?)",
                PREFIX + "%", OTHER_JOB_ID, RECOVERY_JOB_ID, EXPIRED_JOB_ID);
        jdbc.update("DELETE FROM sys_tenant WHERE id=?", EXPIRED_TENANT_ID);
        jdbc.update("UPDATE sys_tenant SET status=0,expire_time=NULL WHERE id=?", STALE_TENANT_ID);
    }

    private void cleanupFixtures() {
        cleanupJobs();
        jdbc.update("DELETE FROM sys_role_menu WHERE role_id IN (?,?,?)", OWNER_ROLE_ID, OTHER_ROLE_ID, STALE_ROLE_ID);
        jdbc.update("DELETE FROM sys_user_role WHERE user_id IN (?,?,?) OR tenant_id IN (?,?,?)",
                OWNER_USER_ID, OTHER_USER_ID, STALE_USER_ID, OWNER_TENANT_ID, OTHER_TENANT_ID, STALE_TENANT_ID);
        jdbc.update("DELETE FROM sys_user WHERE id IN (?,?,?)", OWNER_USER_ID, OTHER_USER_ID, STALE_USER_ID);
        jdbc.update("DELETE FROM sys_role WHERE id IN (?,?,?)", OWNER_ROLE_ID, OTHER_ROLE_ID, STALE_ROLE_ID);
        jdbc.update("DELETE FROM sys_tenant WHERE id IN (?,?,?) OR tenant_code IN ('job-readiness-owner','job-readiness-other','job-readiness-stale')",
                OWNER_TENANT_ID, OTHER_TENANT_ID, STALE_TENANT_ID);
    }
}
