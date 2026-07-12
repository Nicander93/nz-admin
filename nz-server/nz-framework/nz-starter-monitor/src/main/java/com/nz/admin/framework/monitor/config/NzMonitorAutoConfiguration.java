package com.nz.admin.framework.monitor.config;

import com.nz.admin.framework.monitor.core.MonitorStatusProvider;
import com.nz.admin.framework.monitor.core.RedisMonitorStatusProvider;
import com.nz.admin.framework.monitor.redis.SpringDataRedisMonitorStatusProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;

@AutoConfiguration
public class NzMonitorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MonitorStatusProvider monitorStatusProvider(ObjectProvider<HealthEndpoint> healthEndpointProvider,
                                                       ObjectProvider<DataSource> dataSourceProvider,
                                                       ObjectProvider<RedisMonitorStatusProvider> redisProvider) {
        return new MonitorStatusProvider(healthEndpointProvider.getIfAvailable(), dataSourceProvider.getIfAvailable(),
                redisProvider.getIfAvailable());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RedisConnectionFactory.class)
    static class RedisMonitorConfiguration {

        @Bean
        @ConditionalOnBean(RedisConnectionFactory.class)
        @ConditionalOnMissingBean(RedisMonitorStatusProvider.class)
        RedisMonitorStatusProvider redisMonitorStatusProvider(RedisConnectionFactory connectionFactory) {
            return new SpringDataRedisMonitorStatusProvider(connectionFactory);
        }
    }
}