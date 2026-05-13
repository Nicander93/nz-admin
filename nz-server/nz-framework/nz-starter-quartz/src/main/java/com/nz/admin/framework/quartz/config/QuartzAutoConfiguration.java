package com.nz.admin.framework.quartz.config;

import com.nz.admin.framework.quartz.core.QuartzSchedulerManager;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * Quartz 自动配置：使用 Spring 管理 Job 实例以便构造器注入。
 */
@AutoConfiguration
public class QuartzAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    public Scheduler scheduler(ApplicationContext applicationContext) throws Exception {
        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        scheduler.setJobFactory(jobFactory);
        return scheduler;
    }

    @Bean
    public QuartzSchedulerManager quartzSchedulerManager(Scheduler scheduler) {
        return new QuartzSchedulerManager(scheduler);
    }
}
