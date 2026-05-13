package com.nz.admin.framework.monitor.config;

import com.nz.admin.framework.monitor.core.MonitorStatusProvider;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * 监控自动配置。
 */
@AutoConfiguration
public class NzMonitorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MonitorStatusProvider monitorStatusProvider(org.springframework.beans.factory.ObjectProvider<HealthEndpoint> healthEndpointProvider,
                                                       org.springframework.beans.factory.ObjectProvider<DataSource> dataSourceProvider) {
        return new MonitorStatusProvider(healthEndpointProvider.getIfAvailable(), dataSourceProvider.getIfAvailable());
    }
}
