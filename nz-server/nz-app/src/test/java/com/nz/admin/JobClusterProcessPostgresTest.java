package com.nz.admin;

import cn.hutool.crypto.digest.BCrypt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nz.admin.support.JobClusterProcess;
import com.nz.admin.support.PostgresTestSchema;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/** 双 JVM PostgreSQL Quartz 集群、派发与崩溃恢复验收。 */
@EnabledIfEnvironmentVariable(named = "NZ_TEST_PG_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "NZ_TEST_REDIS_PORT", matches = "[0-9]+")
class JobClusterProcessPostgresTest {
    private static final PostgresTestSchema DATABASE = new PostgresTestSchema("job_cluster_proc");
    private static final String SUFFIX = UUID.randomUUID().toString().replace("-", "");
    private static final String SCHEDULER_NAME = "job-cluster-" + SUFFIX.substring(0, 20);
    private static final String CACHE_PREFIX = "job-cluster-" + SUFFIX;
    private static final String JOB_PREFIX = "cluster_" + SUFFIX.substring(0, 12) + "_";
    private static final String TENANT_CODE = "jc-" + SUFFIX.substring(0, 12);
    private static final String USERNAME = "jc-user-" + SUFFIX.substring(0, 12);
    private static final String PASSWORD = "cluster-process-test-password";
    private static final long TENANT_ID = 989700L;
    private static final long USER_ID = 989701L;
    private static final long ROLE_ID = 989702L;
    private static final List<String> PERMISSIONS = List.of(
            "system:job:list", "system:job:query", "system:job:add", "system:job:edit");
    private static final Duration START_TIMEOUT = Duration.ofSeconds(120);
    private static final Duration JOB_TIMEOUT = Duration.ofSeconds(90);
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    @Test
    @Timeout(value = 9, unit = TimeUnit.MINUTES)
    void twoJvmClusterSharesWorkAndRecoversAnInterruptedRecoverableExecution() throws Exception {
        Path logDirectory = Path.of("target", "job-process-" + SUFFIX).toAbsolutePath();
        Files.createDirectories(logDirectory);
        ChildProcess first = null;
        ChildProcess second = null;
        ProviderServer provider = null;
        HikariDataSource dataSource = null;
        try {
            String jdbcUrl = DATABASE.url();
            migrateAllVersionsBeforeCreatingProbeTables(jdbcUrl);
            dataSource = dataSource(jdbcUrl);
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            createProbeTables(jdbc);
            seedUser(jdbc);
            provider = new ProviderServer(jdbc);
            provider.start();

            first = startNode("node-a", logDirectory, provider.uri());
            second = startNode("node-b", logDirectory, provider.uri());
            first.awaitReady(START_TIMEOUT);
            second.awaitReady(START_TIMEOUT);

            String token = login(first);
            JsonNode firstScheduler = successful(first, token, "GET", "/api/system/job/scheduler", null);
            JsonNode secondScheduler = successful(second, token, "GET", "/api/system/job/scheduler", null);
            assertThat(firstScheduler.path("persistent").asBoolean()).isTrue();
            assertThat(firstScheduler.path("clustered").asBoolean()).isTrue();
            assertThat(secondScheduler.path("persistent").asBoolean()).isTrue();
            assertThat(secondScheduler.path("clustered").asBoolean()).isTrue();
            assertThat(firstScheduler.path("schedulerName").asText()).isEqualTo(SCHEDULER_NAME);
            assertThat(secondScheduler.path("schedulerName").asText()).isEqualTo(SCHEDULER_NAME);
            assertThat(firstScheduler.path("instanceId").asText())
                    .isNotEqualTo(secondScheduler.path("instanceId").asText());
            first.instanceId = firstScheduler.path("instanceId").asText();
            second.instanceId = secondScheduler.path("instanceId").asText();
            await(() -> count(jdbc, "SELECT COUNT(*) FROM qrtz_scheduler_state WHERE sched_name=?", SCHEDULER_NAME) == 2,
                    START_TIMEOUT, "Quartz did not register exactly two live scheduler instances", first, second);

            verifyCronOwnershipAndPause(jdbc, first, second, token);
            verifyCrossNodeManualIdempotency(jdbc, first, second, token);
            verifyHardCrashRecovery(jdbc, first, second, token, provider, logDirectory);
        } finally {
            // 两个子进程停止后，才允许删除本测试的 schema。
            try {
                stopNode(first);
            } finally {
                stopNode(second);
            }
            if ((first != null && first.isAlive()) || (second != null && second.isAlive())) {
                throw new IllegalStateException("Refusing to drop the isolated schema while a child JVM is alive");
            }
            if (provider != null) provider.close();
            if (dataSource != null) dataSource.close();
            DATABASE.drop();
        }
    }

    private void migrateAllVersionsBeforeCreatingProbeTables(String jdbcUrl) {
        Flyway.configure()
                .dataSource(jdbcUrl, dbUsername(), dbPassword())
                .schemas(DATABASE.name())
                .defaultSchema(DATABASE.name())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .cleanDisabled(true)
                .load()
                .migrate();
    }

    private HikariDataSource dataSource(String jdbcUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUsername());
        config.setPassword(dbPassword());
        config.setMaximumPoolSize(8);
        config.setMinimumIdle(1);
        config.setPoolName("job-cluster-parent-" + SUFFIX.substring(0, 8));
        return new HikariDataSource(config);
    }

    private void createProbeTables(JdbcTemplate jdbc) {
        jdbc.execute("CREATE TABLE job_cluster_probe_marker (execution_key VARCHAR(200) PRIMARY KEY, "
                + "call_count INTEGER NOT NULL, node_id VARCHAR(200) NOT NULL, attempt INTEGER NOT NULL)");
        jdbc.execute("CREATE TABLE job_cluster_probe_request (id BIGSERIAL PRIMARY KEY, "
                + "execution_key VARCHAR(200) NOT NULL, request_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE job_cluster_probe_effect (execution_key VARCHAR(200) PRIMARY KEY, "
                + "created_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP)");
    }

    private void seedUser(JdbcTemplate jdbc) {
        jdbc.update("INSERT INTO sys_tenant(id,tenant_code,tenant_name,package_id,account_count,status) "
                        + "VALUES(?,?,?,1,20,0)", TENANT_ID, TENANT_CODE, TENANT_CODE);
        jdbc.update("INSERT INTO sys_role(id,tenant_id,name,role_key,sort,status,data_scope) "
                        + "VALUES(?,?,?, ?,0,0,1)", ROLE_ID, TENANT_ID, USERNAME + " role", USERNAME + "-role");
        jdbc.update("INSERT INTO sys_user(id,tenant_id,dept_id,username,password,nickname,status) "
                        + "VALUES(?,?,NULL,?,?,?,0)", USER_ID, TENANT_ID, USERNAME, BCrypt.hashpw(PASSWORD), USERNAME);
        jdbc.update("INSERT INTO sys_user_role(tenant_id,user_id,role_id) VALUES(?,?,?)", TENANT_ID, USER_ID, ROLE_ID);
        String placeholders = String.join(",", PERMISSIONS.stream().map(ignored -> "?").toList());
        List<Object> values = new ArrayList<>(List.of(TENANT_ID, ROLE_ID));
        values.addAll(PERMISSIONS);
        jdbc.update("INSERT INTO sys_role_menu(tenant_id,role_id,menu_id) SELECT ?,?,id FROM sys_menu WHERE perm IN ("
                + placeholders + ") ON CONFLICT DO NOTHING", values.toArray());
        Integer permissionCount = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role_menu rm "
                        + "JOIN sys_menu m ON m.id=rm.menu_id WHERE rm.tenant_id=? AND rm.role_id=? "
                        + "AND m.perm IN (" + placeholders + ")", Integer.class,
                concat(List.of(TENANT_ID, ROLE_ID), PERMISSIONS).toArray());
        assertThat(permissionCount).isEqualTo(PERMISSIONS.size());
    }

    private void verifyCronOwnershipAndPause(JdbcTemplate jdbc, ChildProcess first, ChildProcess second,
                                             String token) throws Exception {
        JsonNode receipt = createJob(first, token, JOB_PREFIX + "cron", "probeTask.record", "0/1 * * * * ?", 0);
        long jobId = receipt.path("jobId").asLong();
        awaitSchedule(first, token, jobId, "SYNCED", START_TIMEOUT, second);

        await(() -> count(jdbc, "SELECT COUNT(*) FROM sys_job_execution WHERE scheduler_name=? AND tenant_id=? "
                        + "AND job_id=? AND trigger_source='SCHEDULED' AND state='SUCCEEDED'",
                SCHEDULER_NAME, TENANT_ID, jobId) >= 3,
                JOB_TIMEOUT, "The real cluster did not run three Cron executions", first, second);
        List<Map<String, Object>> markers = jdbc.queryForList("SELECT e.execution_key,m.call_count "
                        + "FROM sys_job_execution e JOIN job_cluster_probe_marker m ON m.execution_key=e.execution_key "
                        + "WHERE e.scheduler_name=? AND e.tenant_id=? AND e.job_id=? AND e.trigger_source='SCHEDULED' "
                        + "AND e.state='SUCCEEDED' ORDER BY e.id",
                SCHEDULER_NAME, TENANT_ID, jobId);
        assertThat(markers).hasSizeGreaterThanOrEqualTo(3)
                .allSatisfy(row -> assertThat(((Number) row.get("call_count")).intValue()).isEqualTo(1));

        JsonNode paused = successful(second, token, "PUT", "/api/system/job/pause/" + jobId, null);
        assertThat(paused.path("scheduleState").asText()).isIn("PENDING", "SYNCED");
        awaitSchedule(second, token, jobId, "SYNCED", START_TIMEOUT, first);
        await(() -> count(jdbc, "SELECT COUNT(*) FROM sys_job_execution WHERE scheduler_name=? AND tenant_id=? "
                        + "AND job_id=? AND trigger_source='SCHEDULED' AND state='RUNNING'",
                SCHEDULER_NAME, TENANT_ID, jobId) == 0,
                Duration.ofSeconds(10), "A Cron execution did not drain after pause converged", first, second);
        int markerCountAtPause = count(jdbc, "SELECT COUNT(*) FROM job_cluster_probe_marker m "
                        + "JOIN sys_job_execution e ON e.execution_key=m.execution_key WHERE e.scheduler_name=? "
                        + "AND e.tenant_id=? AND e.job_id=? AND e.trigger_source='SCHEDULED'",
                SCHEDULER_NAME, TENANT_ID, jobId);
        Instant quietPeriodEnds = Instant.now().plusMillis(2_200);
        await(() -> Instant.now().isAfter(quietPeriodEnds), Duration.ofSeconds(4),
                "Cron quiet period did not elapse", first, second);
        int markerCountAfterQuietPeriod = count(jdbc, "SELECT COUNT(*) FROM job_cluster_probe_marker m "
                        + "JOIN sys_job_execution e ON e.execution_key=m.execution_key WHERE e.scheduler_name=? "
                        + "AND e.tenant_id=? AND e.job_id=? AND e.trigger_source='SCHEDULED'",
                SCHEDULER_NAME, TENANT_ID, jobId);
        assertThat(markerCountAfterQuietPeriod).isEqualTo(markerCountAtPause);
    }

    private void verifyCrossNodeManualIdempotency(JdbcTemplate jdbc, ChildProcess first, ChildProcess second,
                                                   String token) throws Exception {
        JsonNode receipt = createJob(first, token, JOB_PREFIX + "manual", "probeTask.record", "0 0 0 1 1 ? 2099", 0);
        long jobId = receipt.path("jobId").asLong();
        awaitSchedule(first, token, jobId, "SYNCED", START_TIMEOUT, second);
        String idempotencyKey = "cross-node." + SUFFIX.substring(0, 20);

        ExecutorService callers = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<JsonNode> a = callers.submit(() -> concurrentRun(first, token, jobId, idempotencyKey, ready, start));
            Future<JsonNode> b = callers.submit(() -> concurrentRun(second, token, jobId, idempotencyKey, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            JsonNode aReceipt = a.get(15, TimeUnit.SECONDS);
            JsonNode bReceipt = b.get(15, TimeUnit.SECONDS);
            assertThat(aReceipt.path("code").asInt()).isEqualTo(200);
            assertThat(bReceipt.path("code").asInt()).isEqualTo(200);
            long executionId = aReceipt.path("data").path("executionId").asLong();
            String executionKey = aReceipt.path("data").path("executionKey").asText();
            assertThat(executionId).isPositive();
            assertThat(bReceipt.path("data").path("executionId").asLong()).isEqualTo(executionId);
            assertThat(bReceipt.path("data").path("executionKey").asText()).isEqualTo(executionKey);

            await(() -> "SUCCEEDED".equals(executionState(jdbc, executionId)), JOB_TIMEOUT,
                    "Cross-node idempotent execution did not succeed", first, second);
            assertThat(count(jdbc, "SELECT COUNT(*) FROM sys_job_execution WHERE scheduler_name=? AND tenant_id=? "
                            + "AND job_id=? AND execution_key=?", SCHEDULER_NAME, TENANT_ID, jobId, executionKey))
                    .isEqualTo(1);
            assertThat(count(jdbc, "SELECT call_count FROM job_cluster_probe_marker WHERE execution_key=?", executionKey))
                    .isEqualTo(1);
        } finally {
            start.countDown();
            callers.shutdownNow();
        }
    }

    private JsonNode concurrentRun(ChildProcess node, String token, long jobId, String key,
                                   CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent start gate timed out");
            return request(node, token, "PUT", "/api/system/job/run/" + jobId, null,
                    Map.of("Idempotency-Key", key));
        } catch (Exception error) {
            throw new IllegalStateException("Concurrent run request failed", error);
        }
    }

    private void verifyHardCrashRecovery(JdbcTemplate jdbc, ChildProcess first, ChildProcess second,
                                         String token, ProviderServer provider, Path logDirectory) throws Exception {
        JsonNode receipt = createJob(first, token, JOB_PREFIX + "crash", "probeTask.recoverableEffect",
                "0 0 0 1 1 ? 2099", 0);
        long jobId = receipt.path("jobId").asLong();
        awaitSchedule(first, token, jobId, "SYNCED", START_TIMEOUT, second);

        successful(first, token, "POST", "/api/test/job-cluster/trigger-once/" + jobId, null);
        Path signalPath = awaitFirstAttemptSignal(logDirectory, JOB_TIMEOUT, first, second);
        Properties signal = readProperties(signalPath);
        long executionId = Long.parseLong(signal.getProperty("executionId"));
        String executionKey = signal.getProperty("executionKey");
        String originalNodeId = signal.getProperty("nodeId");
        long scheduledFireTime = Long.parseLong(signal.getProperty("scheduledFireTime"));
        assertThat(signal.getProperty("attempt")).isEqualTo("1");
        assertThat(executionKey).startsWith("scheduled:" + TENANT_ID + ":" + jobId + ":");
        assertThat(executionId).isPositive();

        await(() -> "RUNNING".equals(executionState(jdbc, executionId)), Duration.ofSeconds(10),
                "Crash probe never became RUNNING", first, second);
        assertThat(count(jdbc, "SELECT COUNT(*) FROM job_cluster_probe_marker WHERE execution_key=?", executionKey))
                .as("first attempt's local marker must remain uncommitted")
                .isZero();
        assertThat(provider.requestCount(executionKey)).isEqualTo(1);
        assertThat(provider.effectCount(executionKey)).isEqualTo(1);

        ChildProcess owner = first.instanceId().equals(originalNodeId) ? first : second;
        assertThat(owner.instanceId()).isEqualTo(originalNodeId);
        owner.killForcibly();
        ChildProcess survivor = owner == first ? second : first;
        assertThat(survivor.isAlive()).isTrue();

        await(() -> "SUCCEEDED".equals(executionState(jdbc, executionId)), JOB_TIMEOUT,
                "Surviving JVM did not recover the crashed Quartz execution", survivor);
        ExecutionRow recovered = execution(jdbc, executionId);
        assertThat(recovered.executionKey()).isEqualTo(executionKey);
        assertThat(recovered.triggerSource()).isEqualTo("SCHEDULED");
        assertThat(recovered.scheduledFireTime()).isEqualTo(scheduledFireTime);
        assertThat(recovered.attempt()).isGreaterThanOrEqualTo(2);
        assertThat(recovered.recovered()).isTrue();
        assertThat(recovered.nodeId()).isNotEqualTo(originalNodeId);
        assertThat(recovered.nodeId()).isEqualTo(survivor.instanceId());
        assertThat(count(jdbc, "SELECT call_count FROM job_cluster_probe_marker WHERE execution_key=?", executionKey))
                .isEqualTo(1);
        assertThat(provider.requestCount(executionKey)).isGreaterThanOrEqualTo(2);
        assertThat(provider.effectCount(executionKey)).isEqualTo(1);
        List<Map<String, Object>> attemptLogs = jdbc.queryForList(
                "SELECT attempt,status FROM sys_job_log WHERE tenant_id=? AND execution_id=? ORDER BY attempt",
                TENANT_ID, executionId);
        assertThat(attemptLogs).anySatisfy(row -> {
            assertThat(((Number) row.get("attempt")).intValue()).isEqualTo(1);
            assertThat(((Number) row.get("status")).intValue()).isEqualTo(1);
        });
        assertThat(attemptLogs).anySatisfy(row -> {
            assertThat(((Number) row.get("attempt")).intValue()).isGreaterThanOrEqualTo(2);
            assertThat(((Number) row.get("status")).intValue()).isZero();
        });
        assertThat(survivor.isAlive()).isTrue();
    }

    private JsonNode createJob(ChildProcess node, String token, String name, String target,
                               String cron, int status) throws Exception {
        Map<String, Object> body = Map.of(
                "jobName", name,
                "jobGroup", "DEFAULT",
                "invokeTarget", target,
                "cronExpression", cron,
                "status", status,
                "concurrent", 1,
                "misfirePolicy", 3,
                "remark", "private two-JVM cluster probe " + SUFFIX);
        JsonNode response = successful(node, token, "POST", "/api/system/job", body);
        assertThat(response.path("jobId").asLong()).isPositive();
        return response;
    }

    private void awaitSchedule(ChildProcess node, String token, long jobId, String state,
                               Duration timeout, ChildProcess diagnosticNode) {
        await(() -> {
            try {
                JsonNode job = request(node, token, "GET", "/api/system/job/" + jobId, null, Map.of());
                return job.path("code").asInt() == 200
                        && state.equals(job.path("data").path("scheduleState").asText());
            } catch (Exception ignored) {
                return false;
            }
        }, timeout, "Job " + jobId + " did not reach scheduleState=" + state, node, diagnosticNode);
    }

    private String login(ChildProcess node) throws Exception {
        JsonNode response = request(node, null, "POST", "/api/auth/login",
                Map.of("tenantCode", TENANT_CODE, "clientId", "nz-web-account",
                        "username", USERNAME, "password", PASSWORD), Map.of());
        assertThat(response.path("code").asInt()).isEqualTo(200);
        String token = response.path("data").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private JsonNode successful(ChildProcess node, String token, String method, String path, Object body)
            throws Exception {
        JsonNode response = request(node, token, method, path, body, Map.of());
        assertThat(response.path("code").asInt())
                .as(method + " " + path + " response: " + response.path("msg").asText())
                .isEqualTo(200);
        return response.path("data");
    }

    private JsonNode request(ChildProcess node, String token, String method, String path, Object body,
                             Map<String, String> extraHeaders) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(node.baseUri().resolve(path))
                .timeout(Duration.ofSeconds(10));
        if (token != null) request.header("Authorization", token);
        extraHeaders.forEach(request::header);
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json");
            request.method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        }
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.body() == null || response.body().isBlank()) {
            throw new AssertionError(method + " " + path + " returned HTTP " + response.statusCode());
        }
        JsonNode json = mapper.readTree(response.body());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AssertionError(method + " " + path + " HTTP " + response.statusCode() + ": " + json);
        }
        return json;
    }

    private void await(BooleanSupplier condition, Duration timeout, String message, ChildProcess... children) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            for (ChildProcess child : children) {
                if (child != null && child.hasExited()) {
                    throw new AssertionError(message + "; " + child.name() + " exited: " + child.diagnostics());
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new AssertionError(message + "; waiter interrupted", error);
            }
        }
        String diagnostics = java.util.Arrays.stream(children).filter(java.util.Objects::nonNull)
                .map(ChildProcess::diagnostics).reduce("", (a, b) -> a + System.lineSeparator() + b);
        throw new AssertionError(message + diagnostics);
    }

    private Path awaitFirstAttemptSignal(Path logDirectory, Duration timeout, ChildProcess... children) {
        Path signals = logDirectory.resolve("signals");
        AtomicReference<Path> signal = new AtomicReference<>();
        await(() -> {
            try (var files = Files.list(signals)) {
                signal.set(files.filter(path -> path.getFileName().toString().matches("started-\\d+-1\\.properties"))
                        .findFirst().orElse(null));
                return signal.get() != null;
            } catch (IOException error) {
                return false;
            }
        }, timeout, "No attempt-1 signal was written", children);
        return signal.get();
    }

    private Properties readProperties(Path path) {
        Properties properties = new Properties();
        try (var input = Files.newInputStream(path)) {
            properties.load(input);
            return properties;
        } catch (IOException error) {
            throw new IllegalStateException("Could not read task start signal " + path, error);
        }
    }

    private String executionState(JdbcTemplate jdbc, long executionId) {
        List<String> states = jdbc.query("SELECT state FROM sys_job_execution WHERE scheduler_name=? AND id=?",
                (ResultSet rs, int row) -> rs.getString(1), SCHEDULER_NAME, executionId);
        return states.isEmpty() ? null : states.get(0);
    }

    private ExecutionRow execution(JdbcTemplate jdbc, long executionId) {
        return jdbc.queryForObject("SELECT execution_key,trigger_source,scheduled_fire_time,attempt,recovered,node_id "
                        + "FROM sys_job_execution WHERE scheduler_name=? AND id=?",
                (rs, row) -> new ExecutionRow(rs.getString("execution_key"), rs.getString("trigger_source"),
                        rs.getLong("scheduled_fire_time"), rs.getInt("attempt"), rs.getBoolean("recovered"),
                        rs.getString("node_id")), SCHEDULER_NAME, executionId);
    }

    private int count(JdbcTemplate jdbc, String sql, Object... values) {
        Integer count = jdbc.queryForObject(sql, Integer.class, values);
        return count == null ? 0 : count;
    }

    private static List<Object> concat(List<?> first, List<?> second) {
        List<Object> values = new ArrayList<>(first);
        values.addAll(second);
        return values;
    }

    private ChildProcess startNode(String name, Path logDirectory, URI providerUri) throws IOException {
        Path ready = logDirectory.resolve(name + ".ready");
        Path log = logDirectory.resolve(name + ".log");
        Files.deleteIfExists(ready);
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classPath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        if (classPath == null || classPath.isBlank()) throw new IllegalStateException("Surefire test classpath is missing");
        List<String> command = new ArrayList<>(List.of(java,
                "-Xms64m",
                "-Xmx384m",
                "-Djob.cluster.ready-file=" + ready,
                "-Djob.cluster.external-url=" + providerUri,
                "-Djob.cluster.signals-dir=" + logDirectory.resolve("signals"),
                "-cp", classPath, JobClusterProcess.class.getName(),
                "--spring.profiles.active=prod,pg-test,job-cluster-probe",
                "--spring.main.web-application-type=servlet",
                "--server.address=127.0.0.1",
                "--spring.datasource.url=" + DATABASE.url(),
                "--spring.datasource.username=" + dbUsername(),
                "--spring.datasource.password=" + dbPassword(),
                "--spring.datasource.hikari.schema=" + DATABASE.name(),
                "--spring.datasource.hikari.maximum-pool-size=12",
                "--spring.datasource.hikari.minimum-idle=1",
                "--spring.flyway.schemas=" + DATABASE.name(),
                "--spring.flyway.default-schema=" + DATABASE.name(),
                "--spring.flyway.clean-disabled=true",
                "--spring.data.redis.host=127.0.0.1",
                "--spring.data.redis.port=" + System.getenv("NZ_TEST_REDIS_PORT"),
                "--nz.cluster.enabled=true",
                "--nz.redis.enabled=true",
                "--nz.cache.key-prefix=" + CACHE_PREFIX,
                "--nz.job.cluster-enabled=true",
                "--nz.job.scheduler-name=" + SCHEDULER_NAME,
                "--nz.job.thread-count=4",
                "--nz.job.cluster-checkin-interval=1s",
                "--nz.job.execution-lease=8s",
                "--nz.job.reconcile-interval=5s",
                "--nz.job.max-dispatch-attempts=8",
                "--nz.modules.system.initialize-data=false",
                "--nz.modules.demo.enabled=false",
                "--warm-flow.enabled=false",
                "--nz.realtime.enabled=false",
                "--server.port=0"));
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.directory(Path.of("").toAbsolutePath().toFile());
        return new ChildProcess(name, builder.start(), ready, log);
    }

    private void stopNode(ChildProcess child) {
        if (child == null) return;
        child.stop();
    }

    private String dbUsername() { return System.getenv().getOrDefault("NZ_TEST_PG_USERNAME", "nztest"); }
    private String dbPassword() { return System.getenv().getOrDefault("NZ_TEST_PG_PASSWORD", "test"); }

    private final class ChildProcess {
        private final String name;
        private final Process process;
        private final Path readyFile;
        private final Path logFile;
        private URI baseUri;
        private String instanceId;

        private ChildProcess(String name, Process process, Path readyFile, Path logFile) {
            this.name = name;
            this.process = process;
            this.readyFile = readyFile;
            this.logFile = logFile;
        }

        private void awaitReady(Duration timeout) {
            await(() -> {
                if (!process.isAlive()) throw new IllegalStateException(diagnostics());
                try {
                    if (!Files.exists(readyFile)) return false;
                    int port = Integer.parseInt(Files.readString(readyFile).trim());
                    baseUri = URI.create("http://127.0.0.1:" + port);
                    return port > 0;
                } catch (Exception error) {
                    return false;
                }
            }, timeout, name + " failed to publish its HTTP port");
        }

        private String instanceId() { return instanceId; }
        private String name() { return name; }
        private URI baseUri() { return baseUri; }
        private boolean isAlive() { return process.isAlive(); }
        private boolean hasExited() { return !process.isAlive(); }

        private void killForcibly() throws InterruptedException {
            process.destroyForcibly();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                throw new AssertionError(name + " did not exit after destroyForcibly");
            }
        }

        private void stop() {
            if (process.isAlive()) {
                process.destroy();
                try {
                    if (!process.waitFor(5, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                        if (!process.waitFor(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException(name + " did not stop after forced termination");
                        }
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    process.destroyForcibly();
                    throw new IllegalStateException("Interrupted while stopping " + name, error);
                }
            }
        }

        private String diagnostics() {
            String contents;
            try {
                List<String> lines = Files.readAllLines(logFile);
                int start = Math.max(0, lines.size() - 100);
                contents = String.join(System.lineSeparator(), lines.subList(start, lines.size()));
            } catch (IOException error) {
                contents = "<log unavailable: " + error.getMessage() + ">";
            }
            return name + " (alive=" + process.isAlive() + ", exit="
                    + (process.isAlive() ? "running" : process.exitValue()) + ") log tail:\n" + contents;
        }
    }

    private final class ProviderServer implements AutoCloseable {
        private final JdbcTemplate jdbc;
        private final ExecutorService executor = Executors.newFixedThreadPool(2);
        private HttpServer server;

        private ProviderServer(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        private void start() throws IOException {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
            server.setExecutor(executor);
            server.createContext("/effect", exchange -> {
                String key = exchange.getRequestHeaders().getFirst("Idempotency-Key");
                int status = 200;
                if (key == null || key.isBlank() || !"POST".equals(exchange.getRequestMethod())) {
                    status = 400;
                } else {
                    jdbc.update("INSERT INTO job_cluster_probe_request(execution_key) VALUES(?)", key);
                    jdbc.update("INSERT INTO job_cluster_probe_effect(execution_key) VALUES(?) ON CONFLICT DO NOTHING", key);
                }
                byte[] body = mapper.writeValueAsBytes(Map.of("accepted", status == 200));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start();
        }

        private URI uri() {
            if (server == null) throw new IllegalStateException("Provider server is not started");
            return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/effect");
        }

        private int requestCount(String key) {
            return JobClusterProcessPostgresTest.this.count(jdbc,
                    "SELECT COUNT(*) FROM job_cluster_probe_request WHERE execution_key=?", key);
        }

        private int effectCount(String key) {
            return JobClusterProcessPostgresTest.this.count(jdbc,
                    "SELECT COUNT(*) FROM job_cluster_probe_effect WHERE execution_key=?", key);
        }

        @Override
        public void close() {
            if (server != null) server.stop(0);
            executor.shutdownNow();
        }
    }

    private record ExecutionRow(String executionKey, String triggerSource, long scheduledFireTime,
                                int attempt, boolean recovered, String nodeId) {}
}
