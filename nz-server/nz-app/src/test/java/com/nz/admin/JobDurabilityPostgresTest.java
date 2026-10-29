package com.nz.admin;

import cn.hutool.crypto.digest.BCrypt;
import com.fasterxml.jackson.databind.JsonNode;
import com.nz.admin.common.job.JobRunContext;
import com.nz.admin.common.job.JobRunContextHolder;
import com.nz.admin.common.job.JobTask;
import com.nz.admin.framework.test.core.http.ApiTestClient;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.modules.job.entity.dataobject.job.JobDO;
import com.nz.admin.modules.job.service.job.JobDispatchService;
import com.nz.admin.modules.job.service.job.JobService;
import com.nz.admin.framework.quartz.core.QuartzSchedulerManager;
import org.quartz.Scheduler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.core.Ordered;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** PostgreSQL Quartz 持久化、调度 outbox、并发执行幂等与租户隔离回归。 */
@EnabledIfEnvironmentVariable(named = "NZ_TEST_PG_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "NZ_TEST_REDIS_PORT", matches = "[0-9]+")
@SpringBootTest(
        classes = NzAdminApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "nz.cluster.enabled=true",
                "nz.redis.enabled=true",
                "nz.job.cluster-enabled=true",
                "nz.job.execution-lease=5s",
                "nz.job.sync-interval=1000",
                "spring.data.redis.port=${NZ_TEST_REDIS_PORT}"
        })
@ActiveProfiles("pg-test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestExecutionListeners(listeners = JobDurabilityPostgresTest.SchemaCleanupListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@Import(JobDurabilityPostgresTest.DurabilityTaskConfiguration.class)
class JobDurabilityPostgresTest {

    private static final String SUFFIX = UUID.randomUUID().toString().replace("-", "");
    private static final String SCHEMA = "job_durability_" + SUFFIX.substring(0, 16);
    private static final String REDIS_PREFIX = "job-durability-" + SUFFIX;
    private static final String SCHEDULER_NAME = "job-durability-" + SUFFIX.substring(0, 16);
    private static final String JOB_PREFIX = "durability_" + SUFFIX.substring(0, 12) + "_";
    private static final String PASSWORD = "durability-test-password";
    private static final long OWNER_TENANT_ID = 980700L;
    private static final long OTHER_TENANT_ID = 980701L;
    private static final long OWNER_USER_ID = 980710L;
    private static final long OTHER_USER_ID = 980711L;
    private static final long OWNER_ROLE_ID = 980720L;
    private static final long OTHER_ROLE_ID = 980721L;
    private static final List<String> JOB_PERMISSIONS = List.of(
            "system:job:list", "system:job:query", "system:job:add", "system:job:edit", "system:job:remove",
            "system:joblog:list", "system:joblog:query", "system:joblog:remove");

    private static volatile boolean schemaCreated;

    @Autowired TestRestTemplate rest;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;
    @Autowired JobService jobs;
    @Autowired JobDispatchService dispatcher;
    @Autowired Scheduler scheduler;

    private String ownerTenantCode;
    private String otherTenantCode;
    private String ownerUsername;
    private String otherUsername;
    private ApiTestClient ownerApi;
    private ApiTestClient otherApi;

    @DynamicPropertySource
    static void isolateDatabaseAndRedis(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", JobDurabilityPostgresTest::isolatedJdbcUrl);
        properties.add("spring.datasource.username", () -> env("NZ_TEST_PG_USERNAME", "postgres"));
        properties.add("spring.datasource.password", () -> env("NZ_TEST_PG_PASSWORD", "postgres"));
        properties.add("spring.datasource.hikari.schema", () -> SCHEMA);
        properties.add("spring.flyway.schemas", () -> SCHEMA);
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("nz.cache.key-prefix", () -> REDIS_PREFIX);
        properties.add("nz.job.scheduler-name", () -> SCHEDULER_NAME);
        properties.add("nz.job.cluster-enabled", () -> true);
    }

    @BeforeAll
    void seedPrivateTenantPrincipals() {
        ownerTenantCode = "jobd-" + SUFFIX.substring(0, 10);
        otherTenantCode = "jobd-x-" + SUFFIX.substring(0, 8);
        ownerUsername = "jobd-owner-" + SUFFIX.substring(0, 10);
        otherUsername = "jobd-other-" + SUFFIX.substring(0, 10);
        createMarkerTable();
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT perm) FROM sys_menu WHERE perm IN "
                + "('system:joblog:list','system:joblog:query','system:joblog:remove')", Integer.class)).isEqualTo(3);
        seedTenant(OWNER_TENANT_ID, ownerTenantCode);
        seedTenant(OTHER_TENANT_ID, otherTenantCode);
        seedPrincipal(OWNER_TENANT_ID, OWNER_USER_ID, OWNER_ROLE_ID, ownerUsername);
        seedPrincipal(OTHER_TENANT_ID, OTHER_USER_ID, OTHER_ROLE_ID, otherUsername);
        ownerApi = login(ownerTenantCode, ownerUsername);
        otherApi = login(otherTenantCode, otherUsername);
    }

    @AfterAll
    void logoutFixtures() {
        if (ownerApi != null) ownerApi.ok(HttpMethod.POST, "/api/auth/logout", null);
        if (otherApi != null) otherApi.ok(HttpMethod.POST, "/api/auth/logout", null);
    }

    @Test
    void reportsPostgresPersistentClusteredQuartzMetadata() {
        JsonNode metadata = ownerApi.ok(HttpMethod.GET, "/api/system/job/scheduler", null);

        assertThat(metadata.path("persistent").asBoolean()).isTrue();
        assertThat(metadata.path("clustered").asBoolean()).isTrue();
        assertThat(metadata.path("schedulerName").asText()).isEqualTo(SCHEDULER_NAME);
        assertThat(metadata.path("instanceId").asText()).isNotBlank();
        assertThat(metadata.path("running").asBoolean()).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name='qrtz_job_details'",
                Integer.class, SCHEMA)).isEqualTo(1);

        long jobId = createJob(JOB_PREFIX + "persistent_schedule", "durabilityTask.record");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM qrtz_job_details WHERE sched_name=? AND job_name=?",
                Integer.class, SCHEDULER_NAME, String.valueOf(jobId))).isEqualTo(1);
    }

    @Test
    void rollsBackTheJobDefinitionAndItsScheduleOutboxTogether() {
        String name = JOB_PREFIX + "rollback";
        JobDO input = newJob(name, "durabilityTask.record");
        Long[] generatedId = new Long[1];

        assertThatThrownBy(() -> transactions.executeWithoutResult(status ->
                TenantContextHolder.runWithTenantId(OWNER_TENANT_ID, () -> {
                    jobs.save(input);
                    generatedId[0] = jdbc.queryForObject("SELECT id FROM sys_job WHERE job_name=?", Long.class, name);
                    throw new ExpectedRollback();
                }))).isInstanceOf(ExpectedRollback.class);

        assertThat(generatedId[0]).isPositive();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_job WHERE id=?", Integer.class, generatedId[0]))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_job_schedule_outbox WHERE job_id=?", Integer.class,
                generatedId[0])).isZero();
    }

    @Test
    void savesPendingDefinitionDuringRealQuartzStoreFailureAndRetriesOutboxAfterRecovery() throws Exception {
        String name = JOB_PREFIX + "store_failure";
        String renamedTable = "qrtz_job_details_disabled_" + SUFFIX.substring(0, 8);
        boolean tableRenamed = false;
        try {
            jdbc.execute("ALTER TABLE qrtz_job_details RENAME TO " + renamedTable);
            tableRenamed = true;

            JsonNode receipt = ownerApi.ok(HttpMethod.POST, "/api/system/job", writeRequest(
                    name, "durabilityTask.record"));
            long jobId = receipt.path("jobId").asLong();
            long revision = receipt.path("revision").asLong();
            assertThat(jobId).isPositive();
            assertThat(revision).isPositive();
            assertThat(receipt.path("scheduleState").asText()).isIn("PENDING", "ERROR");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_job WHERE id=? AND job_name=?", Integer.class,
                    jobId, name)).isEqualTo(1);

            Outbox outbox = awaitOutbox(jobId, state -> "PENDING".equals(state) || "ERROR".equals(state));
            assertThat(outbox.state()).isIn("PENDING", "ERROR");
            assertThat(currentJob(jobId).path("scheduleState").asText()).isIn("PENDING", "ERROR");

            jdbc.execute("ALTER TABLE " + renamedTable + " RENAME TO qrtz_job_details");
            tableRenamed = false;
            ownerApi.ok(HttpMethod.PUT, "/api/system/job/schedule-outbox/" + outbox.id() + "/retry", null);
            dispatcher.synchronizePending();

            awaitJobState(jobId, revision, "SYNCED");
            assertThat(awaitOutbox(jobId, "SYNCED"::equals).state()).isEqualTo("SYNCED");
        } finally {
            if (tableRenamed) jdbc.execute("ALTER TABLE " + renamedTable + " RENAME TO qrtz_job_details");
        }
    }

    @Test
    void coalescesConcurrentManualRequestsAndCommitsOneMarkerLedgerAndSuccessLog() throws Exception {
        String name = JOB_PREFIX + "concurrent_success";
        long jobId = createJob(name, "durabilityTask.record");
        String requestKey = "durability." + SUFFIX.substring(0, 16);
        String path = "/api/system/job/run/" + jobId;
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);

        try {
            Future<JsonNode> first = workers.submit(() -> manualRun(path, requestKey, ready, start));
            Future<JsonNode> second = workers.submit(() -> manualRun(path, requestKey, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            JsonNode firstReceipt = first.get(10, TimeUnit.SECONDS);
            JsonNode secondReceipt = second.get(10, TimeUnit.SECONDS);
            assertThat(firstReceipt.path("code").asInt()).isEqualTo(200);
            assertThat(secondReceipt.path("code").asInt()).isEqualTo(200);

            JsonNode firstData = firstReceipt.path("data");
            JsonNode secondData = secondReceipt.path("data");
            long executionId = firstData.path("executionId").asLong();
            String executionKey = firstData.path("executionKey").asText();
            assertThat(executionId).isPositive();
            assertThat(executionKey).isNotBlank();
            assertThat(secondData.path("executionId").asLong()).isEqualTo(executionId);
            assertThat(secondData.path("executionKey").asText()).isEqualTo(executionKey);

            JsonNode completed = awaitExecution(executionId, "SUCCEEDED");
            assertThat(completed.path("executionKey").asText()).isEqualTo(executionKey);
            assertThat(completed.path("jobId").asLong()).isEqualTo(jobId);
            dispatcher.synchronizePending();

            assertThat(jdbc.queryForObject("SELECT call_count FROM test_job_marker WHERE execution_key=?", Integer.class,
                    executionKey)).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM sys_job_execution WHERE id=? AND tenant_id=? AND job_id=? AND state='SUCCEEDED' AND execution_key=?",
                    Integer.class, executionId, OWNER_TENANT_ID, jobId, executionKey)).isEqualTo(1);
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM sys_job_log WHERE execution_id=? AND execution_key=? AND tenant_id=? AND status=0",
                    Integer.class, executionId, executionKey, OWNER_TENANT_ID)).isEqualTo(1);

            JsonNode executionPage = ownerApi.ok(HttpMethod.GET,
                    "/api/system/job/execution/page?pageNum=1&pageSize=50&jobId=" + jobId
                            + "&executionKey=" + executionKey, null);
            assertThat(executionPage.path("records")).hasSize(1);
            JsonNode logsPage = ownerApi.ok(HttpMethod.GET,
                    "/api/system/job/log/page?pageNum=1&pageSize=50&jobId=" + jobId, null);
            long logId = findLog(logsPage, executionId, executionKey).path("id").asLong();

            JsonNode foreignExecutions = otherApi.ok(HttpMethod.GET,
                    "/api/system/job/execution/page?pageNum=1&pageSize=50&jobId=" + jobId
                            + "&executionKey=" + executionKey, null);
            JsonNode foreignLogs = otherApi.ok(HttpMethod.GET,
                    "/api/system/job/log/page?pageNum=1&pageSize=50&jobId=" + jobId, null);
            assertThat(foreignExecutions.path("records")).isEmpty();
            assertThat(foreignLogs.path("records")).isEmpty();
            assertDenied(otherApi.exchange(HttpMethod.GET, "/api/system/job/execution/" + executionId, null));
            assertDenied(otherApi.exchange(HttpMethod.GET, "/api/system/job/log/" + logId, null));
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void rollsBackFailingBusinessMarkerButPersistsFailedLedgerAndFailureLog() {
        String name = JOB_PREFIX + "recoverable_failure";
        long jobId = createJob(name, "durabilityTask.recordThenFail");
        String requestKey = "durability.fail." + SUFFIX.substring(0, 12);
        JsonNode receipt = ownerApi.exchange(HttpMethod.PUT, "/api/system/job/run/" + jobId, null,
                Map.of("Idempotency-Key", requestKey));
        assertThat(receipt.path("code").asInt()).isEqualTo(200);
        long executionId = receipt.path("data").path("executionId").asLong();
        String executionKey = receipt.path("data").path("executionKey").asText();

        JsonNode failed = awaitExecution(executionId, "FAILED");
        assertThat(failed.path("lastError").asText()).contains("intentional durability failure");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM test_job_marker WHERE execution_key=?", Integer.class,
                executionKey)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_job_execution WHERE id=? AND tenant_id=? AND job_id=? AND recoverable=TRUE AND state='FAILED'",
                Integer.class, executionId, OWNER_TENANT_ID, jobId)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_job_log WHERE execution_id=? AND execution_key=? AND tenant_id=? AND status=1 AND exception_info LIKE '%intentional durability failure%'",
                Integer.class, executionId, executionKey, OWNER_TENANT_ID)).isEqualTo(1);
    }

    @Test
    void retriesRecoverableFailureWithOriginalKeyAndCommitsOnlyTheSuccessfulAttempt() {
        long jobId = createJob(JOB_PREFIX + "retry_success", "durabilityTask.recordAndFailFirstAttempt");
        JsonNode failed = runUntilFailure(jobId, "retry.success." + SUFFIX.substring(0, 12));
        long id = failed.path("id").asLong();
        String key = failed.path("executionKey").asText();
        assertRetryEligibility(id, jobId, true);

        JsonNode receipt = ownerApi.ok(HttpMethod.PUT, "/api/system/job/execution/" + id + "/retry", null);
        assertThat(receipt.path("executionId").asLong()).isEqualTo(id);
        assertThat(receipt.path("executionKey").asText()).isEqualTo(key);
        JsonNode succeeded = awaitExecution(id, "SUCCEEDED");
        assertThat(succeeded.path("attempt").asInt()).isEqualTo(2);
        assertThat(succeeded.path("recovered").asBoolean()).isTrue();
        assertRetryEligibility(id, jobId, false);
        assertThat(jdbc.queryForObject("SELECT call_count FROM test_job_marker WHERE execution_key=?", Integer.class, key)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT status FROM sys_job_log WHERE execution_id=? ORDER BY attempt", Integer.class, id))
                .containsExactly(1, 0);
    }

    @Test
    void hidesAndRejectsRetriesForChangedOrDeletedDefinitions() {
        String name = JOB_PREFIX + "retry_old_revision";
        long jobId = createJob(name, "durabilityTask.recordThenFail");
        JsonNode old = runUntilFailure(jobId, "retry.old." + SUFFIX.substring(0, 12));
        long oldId = old.path("id").asLong();
        assertRetryEligibility(oldId, jobId, true);

        Map<String, Object> update = new java.util.HashMap<>(writeRequest(name, "durabilityTask.recordThenFail"));
        update.put("id", jobId);
        update.put("remark", "new definition revision");
        JsonNode mutation = ownerApi.ok(HttpMethod.PUT, "/api/system/job", update);
        awaitJobState(jobId, mutation.path("revision").asLong(), "SYNCED");
        assertRetryEligibility(oldId, jobId, false);
        assertDenied(ownerApi.exchange(HttpMethod.PUT, "/api/system/job/execution/" + oldId + "/retry", null));
        assertThat(awaitExecution(oldId, "FAILED").path("attempt").asInt()).isEqualTo(1);

        JsonNode current = runUntilFailure(jobId, "retry.deleted." + SUFFIX.substring(0, 12));
        long currentId = current.path("id").asLong();
        assertRetryEligibility(currentId, jobId, true);
        ownerApi.ok(HttpMethod.DELETE, "/api/system/job/" + jobId, null);
        assertRetryEligibility(currentId, jobId, false);
        assertDenied(ownerApi.exchange(HttpMethod.PUT, "/api/system/job/execution/" + currentId + "/retry", null));
        assertThat(awaitExecution(currentId, "FAILED").path("attempt").asInt()).isEqualTo(1);
    }

    @Test
    void retriesAnUnstartedFailureButRefusesUnsafeWorkAfterItsFirstAttempt() throws Exception {
        long jobId = createJob(JOB_PREFIX + "retry_unsafe", "durabilityTask.recordUnsafeThenFail");
        long id;
        scheduler.standby();
        try {
            JsonNode receipt = ownerApi.ok(HttpMethod.PUT, "/api/system/job/run/" + jobId, null);
            id = receipt.path("executionId").asLong();
            // 单独安排派发前失败状态；此处验证资格与重试命令，不冒充真实存储故障。
            assertThat(jdbc.update("UPDATE sys_job_execution SET state='FAILED',last_error='test fixture: failed before business' "
                    + "WHERE id=? AND attempt=0", id)).isEqualTo(1);
            assertRetryEligibility(id, jobId, true);
            JsonNode retry = ownerApi.ok(HttpMethod.PUT, "/api/system/job/execution/" + id + "/retry", null);
            assertThat(retry.path("executionId").asLong()).isEqualTo(id);
        } finally {
            scheduler.start();
        }
        JsonNode failed = awaitExecution(id, "FAILED");
        assertThat(failed.path("attempt").asInt()).isEqualTo(1);
        assertThat(failed.path("recoverable").asBoolean()).isFalse();
        assertRetryEligibility(id, jobId, false);
        assertDenied(ownerApi.exchange(HttpMethod.PUT, "/api/system/job/execution/" + id + "/retry", null));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM test_job_marker WHERE execution_key=?", Integer.class,
                failed.path("executionKey").asText())).isZero();
    }

    @Test
    void pauseDoesNotCancelRunningAttemptAndRejectsLaterScheduledTrigger() throws Exception {
        String name = JOB_PREFIX + "pause_running";
        long jobId = createJob(name, "durabilityTask.recordAndBlock");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        DurabilityTask.armPauseGate(entered, release);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            JsonNode receipt = ownerApi.exchange(HttpMethod.PUT, "/api/system/job/run/" + jobId, null,
                    Map.of("Idempotency-Key", "durability.pause." + SUFFIX.substring(0, 12)));
            assertThat(receipt.path("code").asInt()).isEqualTo(200);
            long executionId = receipt.path("data").path("executionId").asLong();
            String executionKey = receipt.path("data").path("executionKey").asText();
            assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(ownerApi.ok(HttpMethod.GET, "/api/system/job/execution/" + executionId, null)
                    .path("state").asText()).isEqualTo("RUNNING");

            Future<JsonNode> pause = worker.submit(() -> ownerApi.ok(HttpMethod.PUT,
                    "/api/system/job/pause/" + jobId, null));
            JsonNode pauseReceipt = pause.get(3, TimeUnit.SECONDS);
            assertThat(pauseReceipt.path("scheduleState").asText()).isIn("PENDING", "SYNCED");
            awaitJobState(jobId, pauseReceipt.path("revision").asLong(), "SYNCED");

            await(() -> Boolean.TRUE.equals(jdbc.queryForObject(
                            "SELECT lease_until<CURRENT_TIMESTAMP FROM sys_job_execution WHERE id=? AND state='RUNNING'",
                            Boolean.class, executionId)), Duration.ofSeconds(8),
                    "运行中执行的租约未自然过期");
            long scanWindowEnds = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(2_200);
            await(() -> System.nanoTime() >= scanWindowEnds, Duration.ofMillis(2_500),
                    "等待真实后台扫描至少越过两个轮询周期");
            assertThat(jdbc.queryForObject("SELECT state FROM sys_job_execution WHERE id=?", String.class, executionId))
                    .isEqualTo("RUNNING");
            assertThat(jdbc.queryForObject("SELECT attempt FROM sys_job_execution WHERE id=?", Integer.class, executionId))
                    .isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_job_execution WHERE scheduler_name=? "
                            + "AND tenant_id=? AND job_id=? AND execution_key=?",
                    Integer.class, SCHEDULER_NAME, OWNER_TENANT_ID, jobId, executionKey)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM test_job_marker WHERE execution_key=?",
                    Integer.class, executionKey)).isZero();

            release.countDown();
            assertThat(awaitExecution(executionId, "SUCCEEDED").path("executionKey").asText())
                    .isEqualTo(executionKey);
            assertThat(jdbc.queryForObject("SELECT call_count FROM test_job_marker WHERE execution_key=?",
                    Integer.class, executionKey)).isEqualTo(1);

            scheduler.triggerJob(QuartzSchedulerManager.jobKey(jobId));
            await(() -> jdbc.queryForObject("SELECT COUNT(*) FROM sys_job_execution WHERE job_id=? "
                            + "AND tenant_id=? AND trigger_source='SCHEDULED' AND state='SKIPPED'",
                    Integer.class, jobId, OWNER_TENANT_ID) > 0, Duration.ofSeconds(10),
                    "暂停后的定时触发未进入 SKIPPED");
            JsonNode page = ownerApi.ok(HttpMethod.GET,
                    "/api/system/job/execution/page?pageNum=1&pageSize=100&jobId=" + jobId, null);
            JsonNode skipped = null;
            for (JsonNode row : page.path("records")) {
                if ("SCHEDULED".equals(row.path("triggerSource").asText())
                        && "SKIPPED".equals(row.path("state").asText())) {
                    skipped = row;
                    break;
                }
            }
            assertThat(skipped).isNotNull();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM test_job_marker WHERE execution_key=?",
                    Integer.class, skipped.path("executionKey").asText())).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM test_job_marker WHERE tenant_id=? AND execution_id IN "
                            + "(SELECT id FROM sys_job_execution WHERE job_id=? AND trigger_source='SCHEDULED')",
                    Integer.class, OWNER_TENANT_ID, jobId)).isZero();
        } finally {
            release.countDown();
            DurabilityTask.clearPauseGate();
            worker.shutdownNow();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DurabilityTaskConfiguration {
        @Bean
        DurabilityTask durabilityTask(JdbcTemplate jdbc) {
            return new DurabilityTask(jdbc);
        }
    }

    static class DurabilityTask {
        private static volatile CountDownLatch pauseEntered;
        private static volatile CountDownLatch pauseRelease;
        private final JdbcTemplate jdbc;

        DurabilityTask(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @JobTask(recoverable = true)
        public void record() {
            recordMarker();
        }

        @JobTask(recoverable = true)
        public void recordThenFail() {
            recordMarker();
            throw new IllegalStateException("intentional durability failure");
        }

        @JobTask(recoverable = true)
        public void recordAndFailFirstAttempt() {
            recordMarker();
            if (JobRunContextHolder.requireCurrent().attempt() == 1)
                throw new IllegalStateException("intentional first attempt failure");
        }

        @JobTask
        public void recordUnsafeThenFail() {
            recordMarker();
            throw new IllegalStateException("intentional unsafe failure");
        }

        @JobTask(recoverable = true)
        public void recordAndBlock() {
            recordMarker();
            CountDownLatch entered = pauseEntered;
            CountDownLatch release = pauseRelease;
            if (entered == null || release == null) throw new IllegalStateException("pause gate is not armed");
            entered.countDown();
            try {
                if (!release.await(20, TimeUnit.SECONDS)) throw new IllegalStateException("pause gate timed out");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("pause gate interrupted", error);
            }
        }

        static void armPauseGate(CountDownLatch entered, CountDownLatch release) {
            pauseEntered = entered;
            pauseRelease = release;
        }

        static void clearPauseGate() {
            pauseEntered = null;
            pauseRelease = null;
        }

        private void recordMarker() {
            JobRunContext context = JobRunContextHolder.requireCurrent();
            jdbc.update("INSERT INTO test_job_marker(tenant_id,execution_key,node_id,execution_id,call_count) "
                            + "VALUES(?,?,?,?,1) ON CONFLICT(execution_key) DO UPDATE "
                            + "SET call_count=test_job_marker.call_count+1",
                    context.tenantId(), context.executionKey(), context.nodeId(), context.executionId());
        }
    }

    private static String isolatedJdbcUrl() {
        String url = env("NZ_TEST_PG_URL", "");
        if (url.isBlank()) throw new IllegalStateException("NZ_TEST_PG_URL is required");
        createPrivateSchema(url);
        String parameter = "currentSchema=" + URLEncoder.encode(SCHEMA, StandardCharsets.UTF_8);
        return url + (url.contains("?") ? "&" : "?") + parameter;
    }

    private static synchronized void createPrivateSchema(String baseUrl) {
        if (schemaCreated) return;
        try (var connection = DriverManager.getConnection(baseUrl, env("NZ_TEST_PG_USERNAME", "postgres"),
                env("NZ_TEST_PG_PASSWORD", "postgres")); Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + SCHEMA);
            schemaCreated = true;
        } catch (SQLException error) {
            throw new IllegalStateException("无法创建任务持久化测试私有 schema", error);
        }
    }

    private static void dropPrivateSchema() throws SQLException {
        if (!schemaCreated) return;
        try (var connection = DriverManager.getConnection(env("NZ_TEST_PG_URL", ""),
                env("NZ_TEST_PG_USERNAME", "postgres"), env("NZ_TEST_PG_PASSWORD", "postgres"));
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
            schemaCreated = false;
        }
    }

    static final class SchemaCleanupListener extends AbstractTestExecutionListener implements Ordered {
        @Override public int getOrder() { return 0; }

        @Override
        public void afterTestClass(TestContext testContext) throws Exception {
            dropPrivateSchema();
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private void createMarkerTable() {
        jdbc.execute("CREATE TABLE test_job_marker (tenant_id BIGINT NOT NULL, "
                + "execution_key VARCHAR(200) PRIMARY KEY, node_id VARCHAR(200), execution_id BIGINT, "
                + "call_count INTEGER NOT NULL)");
    }

    private void seedTenant(long tenantId, String tenantCode) {
        jdbc.update("INSERT INTO sys_tenant(id,tenant_code,tenant_name,package_id,account_count,status) "
                        + "VALUES(?,?,?,1,20,0) ON CONFLICT(id) DO UPDATE SET tenant_code=EXCLUDED.tenant_code, "
                        + "tenant_name=EXCLUDED.tenant_name,package_id=1,status=0,expire_time=NULL",
                tenantId, tenantCode, tenantCode);
    }

    private void seedPrincipal(long tenantId, long userId, long roleId, String username) {
        jdbc.update("INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope) "
                        + "VALUES(?,?,?,?,0,0,1) ON CONFLICT(id) DO UPDATE SET tenant_id=EXCLUDED.tenant_id, "
                        + "name=EXCLUDED.name,role_key=EXCLUDED.role_key,status=0,data_scope=1",
                roleId, tenantId, username + " role", username + "-role");
        jdbc.update("INSERT INTO sys_user(id,tenant_id,dept_id,username,password,nickname,status) "
                        + "VALUES(?,?,NULL,?,?,?,0) ON CONFLICT(id) DO UPDATE SET tenant_id=EXCLUDED.tenant_id, "
                        + "username=EXCLUDED.username,password=EXCLUDED.password,nickname=EXCLUDED.nickname,status=0",
                userId, tenantId, username, BCrypt.hashpw(PASSWORD), username);
        jdbc.update("INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES(?,?,?) ON CONFLICT DO NOTHING",
                tenantId, userId, roleId);
        String placeholders = String.join(",", JOB_PERMISSIONS.stream().map(ignored -> "?").toList());
        List<Object> arguments = new ArrayList<>();
        arguments.add(tenantId);
        arguments.add(roleId);
        arguments.addAll(JOB_PERMISSIONS);
        jdbc.update("INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) "
                        + "SELECT ?, ?, id FROM sys_menu WHERE perm IN (" + placeholders + ") ON CONFLICT DO NOTHING",
                arguments.toArray());
    }

    private ApiTestClient login(String tenantCode, String username) {
        return new ApiTestClient(rest).login(tenantCode, username, PASSWORD);
    }

    private JobDO newJob(String name, String target) {
        return new JobDO().setJobName(name).setJobGroup("DEFAULT").setInvokeTarget(target)
                .setCronExpression("0 0 0 1 1 ? 2099").setStatus(0).setConcurrent(1).setMisfirePolicy(3);
    }

    private Map<String, Object> writeRequest(String name, String target) {
        return Map.of("jobName", name, "jobGroup", "DEFAULT", "invokeTarget", target,
                "cronExpression", "0 0 0 1 1 ? 2099", "status", 0, "concurrent", 1, "misfirePolicy", 3,
                "remark", "private PostgreSQL durability test");
    }

    private long createJob(String name, String target) {
        JsonNode receipt = ownerApi.ok(HttpMethod.POST, "/api/system/job", writeRequest(name, target));
        long id = receipt.path("jobId").asLong();
        assertThat(id).isPositive();
        awaitJobState(id, receipt.path("revision").asLong(), "SYNCED");
        return id;
    }

    private JsonNode manualRun(String path, String key, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("parallel run gate timed out");
        return ownerApi.exchange(HttpMethod.PUT, path, null, Map.of("Idempotency-Key", key));
    }

    private JsonNode currentJob(long id) {
        return ownerApi.ok(HttpMethod.GET, "/api/system/job/" + id, null);
    }

    private JsonNode awaitJobState(long id, long revision, String expected) {
        await(() -> {
            dispatcher.synchronizePending();
            JsonNode job = currentJob(id);
            return job.path("revision").asLong() == revision
                    && expected.equals(job.path("scheduleState").asText());
        }, Duration.ofSeconds(30), "任务没有到达 scheduleState=" + expected);
        return currentJob(id);
    }

    private Outbox awaitOutbox(long jobId, java.util.function.Predicate<String> state) {
        final Outbox[] found = new Outbox[1];
        await(() -> {
            dispatcher.synchronizePending();
            JsonNode page = ownerApi.ok(HttpMethod.GET,
                    "/api/system/job/schedule-outbox/page?pageNum=1&pageSize=100&jobId=" + jobId, null);
            for (JsonNode row : page.path("records")) {
                if (row.path("jobId").asLong() == jobId && state.test(row.path("state").asText())) {
                    found[0] = new Outbox(row.path("id").asLong(), row.path("state").asText());
                    return true;
                }
            }
            return false;
        }, Duration.ofSeconds(20), "任务 outbox 状态未收敛");
        return found[0];
    }

    private JsonNode runUntilFailure(long jobId, String requestKey) {
        JsonNode receipt = ownerApi.exchange(HttpMethod.PUT, "/api/system/job/run/" + jobId, null,
                Map.of("Idempotency-Key", requestKey));
        assertThat(receipt.path("code").asInt()).isEqualTo(200);
        return awaitExecution(receipt.path("data").path("executionId").asLong(), "FAILED");
    }

    private void assertRetryEligibility(long id, long jobId, boolean expected) {
        JsonNode detail = ownerApi.ok(HttpMethod.GET, "/api/system/job/execution/" + id, null);
        assertThat(detail.path("retryable").isBoolean()).isTrue();
        assertThat(detail.path("retryable").asBoolean()).isEqualTo(expected);
        JsonNode page = ownerApi.ok(HttpMethod.GET,
                "/api/system/job/execution/page?pageNum=1&pageSize=100&jobId=" + jobId, null);
        JsonNode record = null;
        for (JsonNode row : page.path("records")) if (row.path("id").asLong() == id) record = row;
        assertThat(record).isNotNull();
        assertThat(record.path("retryable").isBoolean()).isTrue();
        assertThat(record.path("retryable").asBoolean()).isEqualTo(expected);
    }

    private JsonNode awaitExecution(long id, String state) {
        final JsonNode[] latest = new JsonNode[1];
        await(() -> {
            dispatcher.synchronizePending();
            latest[0] = ownerApi.ok(HttpMethod.GET, "/api/system/job/execution/" + id, null);
            return state.equals(latest[0].path("state").asText());
        }, Duration.ofSeconds(30), "执行记录没有到达 state=" + state);
        return latest[0];
    }

    private JsonNode findLog(JsonNode page, long executionId, String executionKey) {
        for (JsonNode row : page.path("records")) {
            if (row.path("executionId").asLong() == executionId
                    && executionKey.equals(row.path("executionKey").asText())) return row;
        }
        throw new AssertionError("执行日志未与 executionId/executionKey 关联");
    }

    private void assertDenied(JsonNode response) {
        assertThat(response.path("code").asInt()).isNotEqualTo(200);
    }

    private void await(BooleanSupplier condition, Duration timeout, String message) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            try {
                Thread.sleep(50);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new AssertionError("等待任务状态时被中断", error);
            }
        }
        throw new AssertionError(message);
    }

    private record Outbox(long id, String state) {}

    private static final class ExpectedRollback extends RuntimeException {}
}
