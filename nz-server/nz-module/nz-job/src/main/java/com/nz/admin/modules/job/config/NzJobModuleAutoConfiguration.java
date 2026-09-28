package com.nz.admin.modules.job.config;

import com.nz.admin.common.job.JobExecuteService;
import com.nz.admin.common.job.JobInvokeLogSink;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ConditionalOnProperty(prefix = "nz.modules.job", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.nz.admin.modules.job")
@MapperScan("com.nz.admin.modules.job.mapper")
public class NzJobModuleAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JobExecuteService jobExecuteService(ApplicationContext applicationContext,
                                               JobInvokeLogSink jobInvokeLogSink) {
        return new JobExecuteService(applicationContext, jobInvokeLogSink);
    }
}