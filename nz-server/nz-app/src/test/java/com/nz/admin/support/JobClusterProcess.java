package com.nz.admin.support;

import com.nz.admin.NzAdminApplication;
import com.nz.admin.common.core.R;
import com.nz.admin.common.job.JobRunContext;
import com.nz.admin.common.job.JobRunContextHolder;
import com.nz.admin.common.job.JobTask;
import com.nz.admin.framework.auth.annotation.SaCheckPermission;
import com.nz.admin.framework.quartz.core.QuartzConstants;
import com.nz.admin.framework.quartz.core.QuartzSchedulerManager;
import com.nz.admin.modules.job.service.job.JobService;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** 双 JVM 集群验收使用的子进程入口和测试探针。 */
public final class JobClusterProcess {
    private JobClusterProcess() {}

    public static void main(String[] args) {
        if (Runtime.version().feature() != 17) {
            throw new IllegalStateException("Job cluster child processes require Java 17");
        }
        SpringApplication.run(new Class<?>[]{NzAdminApplication.class, ProbeConfiguration.class}, args);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Profile("job-cluster-probe")
    public static class ProbeConfiguration {
        @Bean
        ProbeTask probeTask(JdbcTemplate jdbc) {
            return new ProbeTask(jdbc);
        }

        @Bean
        ProbeTriggerController probeTriggerController(Scheduler scheduler, JobService jobs,
                                                      TransactionTemplate transactions) {
            return new ProbeTriggerController(scheduler, jobs, transactions);
        }

        @Bean
        ApplicationListener<ApplicationReadyEvent> processReadiness() {
            return event -> {
                String readyPath = System.getProperty("job.cluster.ready-file");
                if (readyPath == null || readyPath.isBlank()) return;
                try {
                    WebApplicationContext context = (WebApplicationContext) event.getApplicationContext();
                    int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
                    Path readyFile = Path.of(readyPath);
                    Files.createDirectories(readyFile.getParent());
                    Files.writeString(readyFile, Integer.toString(port));
                } catch (Exception error) {
                    throw new IllegalStateException("Could not publish child process readiness", error);
                }
            };
        }
    }

    /** 仅由双 JVM 验收子进程执行的测试任务。 */
    public static class ProbeTask {
        private static final CountDownLatch HOLD_FIRST_RECOVERY_ATTEMPT = new CountDownLatch(1);
        private final JdbcTemplate jdbc;
        private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

        ProbeTask(JdbcTemplate jdbc) {
            this.jdbc = jdbc;
        }

        @JobTask
        public void record() {
            recordLocal(JobRunContextHolder.requireCurrent());
        }

        @JobTask(recoverable = true)
        public void recoverableEffect() {
            JobRunContext context = JobRunContextHolder.requireCurrent();
            callIdempotentProvider(context);
            recordLocal(context);
            writeStartedSignal(context);
            if (context.attempt() != null && context.attempt() == 1) holdUntilProcessDeath();
        }

        private void recordLocal(JobRunContext context) {
            jdbc.update("INSERT INTO job_cluster_probe_marker(execution_key,call_count,node_id,attempt) "
                            + "VALUES(?,1,?,?) ON CONFLICT(execution_key) DO UPDATE SET "
                            + "call_count=job_cluster_probe_marker.call_count+1,node_id=EXCLUDED.node_id,attempt=EXCLUDED.attempt",
                    context.executionKey(), context.nodeId(), context.attempt());
        }

        private void callIdempotentProvider(JobRunContext context) {
            String endpoint = System.getProperty("job.cluster.external-url");
            if (endpoint == null || endpoint.isBlank()) throw new IllegalStateException("Provider URL is missing");
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(5))
                    .header("Idempotency-Key", context.executionKey())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("Idempotent provider returned HTTP " + response.statusCode());
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Provider request was interrupted", error);
            } catch (Exception error) {
                throw new IllegalStateException("Idempotent provider request failed", error);
            }
        }

        private void writeStartedSignal(JobRunContext context) {
            String directory = System.getProperty("job.cluster.signals-dir");
            if (directory == null || directory.isBlank()) throw new IllegalStateException("Signal directory is missing");
            Path root = Path.of(directory);
            Path temporary = root.resolve("started-" + context.executionId() + "-" + context.attempt() + ".tmp");
            Path target = root.resolve("started-" + context.executionId() + "-" + context.attempt() + ".properties");
            Properties properties = new Properties();
            properties.setProperty("executionId", String.valueOf(context.executionId()));
            properties.setProperty("executionKey", context.executionKey());
            properties.setProperty("scheduledFireTime", String.valueOf(context.scheduledFireTime()));
            properties.setProperty("nodeId", context.nodeId());
            properties.setProperty("attempt", String.valueOf(context.attempt()));
            try {
                Files.createDirectories(root);
                try (var output = Files.newOutputStream(temporary)) {
                    properties.store(output, "test probe started");
                }
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (Exception error) {
                throw new IllegalStateException("Could not publish task start signal", error);
            }
        }

        private void holdUntilProcessDeath() {
            try {
                if (!HOLD_FIRST_RECOVERY_ATTEMPT.await(2, TimeUnit.MINUTES)) {
                    throw new IllegalStateException("First recovery attempt was not terminated by the test");
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("First recovery attempt was interrupted", error);
            }
        }
    }

    /** 创建不含手动执行元数据的一次性定时触发器。 */
    @RestController
    @RequestMapping("/api/test/job-cluster")
    @Profile("job-cluster-probe")
    public static class ProbeTriggerController {
        private final Scheduler scheduler;
        private final JobService jobs;
        private final TransactionTemplate transactions;

        ProbeTriggerController(Scheduler scheduler, JobService jobs, TransactionTemplate transactions) {
            this.scheduler = scheduler;
            this.jobs = jobs;
            this.transactions = transactions;
        }

        @SaCheckPermission("system:job:edit")
        @PostMapping("/trigger-once/{jobId}")
        public R<Map<String, Object>> triggerOnce(@PathVariable Long jobId) throws Exception {
            jobs.getById(jobId);
            String triggerName = "probe-once-" + jobId + "-" + UUID.randomUUID();
            long startAt = System.currentTimeMillis() + 1_000;
            transactions.executeWithoutResult(status -> {
                try {
                    Trigger trigger = TriggerBuilder.newTrigger()
                            .withIdentity(new TriggerKey(triggerName, QuartzConstants.OWNED_GROUP))
                            .forJob(QuartzSchedulerManager.jobKey(jobId))
                            .startAt(new Date(startAt))
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule())
                            .build();
                    scheduler.scheduleJob(trigger);
                } catch (Exception error) {
                    throw new IllegalStateException("Could not schedule the one-shot probe trigger", error);
                }
            });
            return R.ok(Map.of("triggerName", triggerName, "startAt", startAt));
        }
    }
}
