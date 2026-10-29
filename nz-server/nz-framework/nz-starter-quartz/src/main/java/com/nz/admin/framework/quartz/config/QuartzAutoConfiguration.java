package com.nz.admin.framework.quartz.config;

import com.nz.admin.framework.quartz.core.QuartzSchedulerManager;
import org.quartz.Scheduler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.quartz.JobStoreType;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

/** 由 Spring 管理工厂、数据源和关闭生命周期，迁移完成后才访问调度表。 */
@AutoConfiguration(afterName = {"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"})
@EnableConfigurationProperties({JobSchedulerProperties.class, QuartzProperties.class})
public class QuartzAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(Scheduler.class)
    @DependsOnDatabaseInitialization
    public SchedulerFactoryBean quartzScheduler(ApplicationContext context, ObjectProvider<DataSource> sources,
                                                  JobSchedulerProperties options, QuartzProperties quartz) throws Exception {
        Assert.hasText(options.getSchedulerName(), "Quartz scheduler name is required");
        Assert.isTrue(options.getSchedulerName().length() <= 120, "Quartz scheduler name is too long");
        Assert.isTrue(options.getThreadCount() > 0 && options.getThreadCount() <= 64, "Quartz thread count must be 1..64");
        Assert.isTrue(options.getClusterCheckinInterval().toMillis() >= 1000, "Quartz cluster check-in must be at least 1s");
        var factory = new SchedulerFactoryBean();
        var properties = new Properties();
        properties.putAll(quartz.getProperties());
        // JDBC 变更可能在唤醒调度线程之后才提交；也定期读取其他节点写入的触发器。
        properties.putIfAbsent("org.quartz.scheduler.idleWaitTime", "1000");
        properties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        properties.setProperty("org.quartz.threadPool.threadCount", String.valueOf(options.getThreadCount()));
        boolean persistent = options.isClusterEnabled() || quartz.getJobStoreType() == JobStoreType.JDBC;
        if (persistent) {
            DataSource dataSource = sources.getIfAvailable();
            Assert.notNull(dataSource, "JDBC Quartz requires the application DataSource");
            try (Connection connection = dataSource.getConnection()) {
                Assert.isTrue("PostgreSQL".equals(connection.getMetaData().getDatabaseProductName()),
                        "Persistent Quartz currently requires PostgreSQL");
            }
            factory.setDataSource(dataSource);
            factory.setNonTransactionalDataSource(dataSource);
            properties.setProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
            properties.setProperty("org.quartz.jobStore.isClustered", String.valueOf(options.isClusterEnabled()));
            properties.setProperty("org.quartz.jobStore.clusterCheckinInterval", String.valueOf(options.getClusterCheckinInterval().toMillis()));
            properties.setProperty("org.quartz.jobStore.useProperties", "true");
            properties.setProperty("org.quartz.jobStore.tablePrefix", "QRTZ_");
        } else {
            properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        }
        var jobs = new SpringBeanJobFactory();
        jobs.setApplicationContext(context);
        factory.setJobFactory(jobs);
        factory.setSchedulerName(options.getSchedulerName());
        factory.setQuartzProperties(properties);
        factory.setAutoStartup(false);
        factory.setExposeSchedulerInRepository(false);
        factory.setWaitForJobsToCompleteOnShutdown(quartz.isWaitForJobsToCompleteOnShutdown());
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public QuartzSchedulerManager quartzSchedulerManager(Scheduler scheduler) {
        return new QuartzSchedulerManager(scheduler);
    }
}
