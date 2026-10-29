package com.nz.admin;

import org.junit.jupiter.api.Test;
import com.nz.admin.support.PostgresTestSchema;
import com.nz.admin.framework.quartz.core.QuartzSchedulerManager;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;

/** Boots the production application twice against isolated PostgreSQL with demo initialization disabled. */
@EnabledIfEnvironmentVariable(named = "NZ_TEST_PG_URL", matches = ".+")
class JobRestartPostgresTest {
    private static final PostgresTestSchema DATABASE = new PostgresTestSchema("job_restart");
    private static final long JOB_ID = 990021L;

    @Test
    void persistedActiveJobIsRecoveredAndRunsAfterApplicationRestart() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(DATABASE.url(),
                System.getenv().getOrDefault("NZ_TEST_PG_USERNAME", "nztest"),
                System.getenv().getOrDefault("NZ_TEST_PG_PASSWORD", "test"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        ConfigurableApplicationContext context = null;
        try {
            context = startApplication();
            cleanup(jdbc);
            jdbc.update("INSERT INTO sys_job(id,tenant_id,job_name,job_group,invoke_target,cron_expression,status,concurrent,misfire_policy) VALUES(?,1,'restart-readiness','DEFAULT','demoTask.run','0 0 0 1 1 ? 2099',0,1,3)", JOB_ID);
            context.close();
            context = null;

            context = startApplication();
            Scheduler scheduler = context.getBean(Scheduler.class);
            assertThat(scheduler.checkExists(QuartzSchedulerManager.jobKey(JOB_ID))).isTrue();
            scheduler.triggerJob(QuartzSchedulerManager.jobKey(JOB_ID));
            awaitLogCount(jdbc, 1);
            context.close();
            context = null;

            context = startApplication();
            scheduler = context.getBean(Scheduler.class);
            assertThat(scheduler.checkExists(QuartzSchedulerManager.jobKey(JOB_ID))).isTrue();
            scheduler.triggerJob(QuartzSchedulerManager.jobKey(JOB_ID));
            awaitLogCount(jdbc, 2);
        } finally {
            if (context != null) context.close();
            DATABASE.drop();
        }
    }

    private void cleanup(JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM sys_job_log WHERE job_id=?", JOB_ID);
        jdbc.update("DELETE FROM sys_job WHERE id=? OR job_name='restart-readiness'", JOB_ID);
    }

    private ConfigurableApplicationContext startApplication() {
        return new SpringApplicationBuilder(NzAdminApplication.class).run(
                "--spring.profiles.active=pg-test",
                "--spring.main.web-application-type=servlet",
                "--spring.datasource.url=" + DATABASE.url(),
                "--spring.flyway.schemas=" + DATABASE.name(),
                "--spring.flyway.default-schema=" + DATABASE.name(),
                "--nz.job.scheduler-name=" + DATABASE.name(),
                "--nz.job.cluster-enabled=true",
                "--spring.datasource.username=" + System.getenv().getOrDefault("NZ_TEST_PG_USERNAME", "nztest"),
                "--spring.datasource.password=" + System.getenv().getOrDefault("NZ_TEST_PG_PASSWORD", "test"),
                "--spring.data.redis.host=127.0.0.1",
                "--spring.data.redis.port=" + System.getenv().getOrDefault("NZ_TEST_REDIS_PORT", "56379"),
                "--warm-flow.enabled=false",
                "--nz.modules.system.initialize-data=false",
                "--nz.modules.demo.enabled=false",
                "--server.port=0");
    }

    private void awaitLogCount(JdbcTemplate jdbc, int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_job_log WHERE job_id=? AND status=0", Integer.class, JOB_ID);
            if (count != null && count >= expected) return;
            Thread.sleep(50);
        }
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_job_log WHERE job_id=? AND status=0", Integer.class, JOB_ID);
        assertThat(count).isGreaterThanOrEqualTo(expected);
    }
}
