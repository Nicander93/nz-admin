package com.nz.admin.modules.job.config;

import com.nz.admin.common.job.JobExecuteService;
import com.nz.admin.framework.quartz.config.JobSchedulerProperties;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import com.nz.admin.common.job.JobInvokeLogSink;
import com.nz.admin.modules.job.service.job.JobExecutionService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@EnableScheduling
@ConditionalOnProperty(prefix = "nz.modules.job", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.nz.admin.modules.job")
@MapperScan("com.nz.admin.modules.job.mapper")
public class NzJobModuleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JobExecuteService jobExecuteService(ApplicationContext applicationContext,
                                               JobInvokeLogSink jobInvokeLogSink, JobExecutionService executions) {
        return new JobExecuteService(applicationContext, jobInvokeLogSink, executions);
    }

    @Bean
    public HealthIndicator jobSchedulerHealthIndicator(Scheduler scheduler, JobSchedulerProperties options) {
        return () -> {
            try {
                var metadata = scheduler.getMetaData();
                boolean running = scheduler.isStarted() && !scheduler.isShutdown() && !scheduler.isInStandbyMode();
                boolean persistenceMatches = !options.isClusterEnabled()
                        || (metadata.isJobStoreSupportsPersistence() && metadata.isJobStoreClustered());
                return (running && persistenceMatches ? Health.up() : Health.down())
                        .withDetail("running", running).withDetail("persistent", metadata.isJobStoreSupportsPersistence())
                        .withDetail("clustered", metadata.isJobStoreClustered()).build();
            } catch (SchedulerException error) { return Health.down(error).build(); }
        };
    }
}
