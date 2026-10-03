package com.nz.admin.framework.protection.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nz.admin.framework.protection.aspect.IdempotentAspect;
import com.nz.admin.framework.protection.core.*;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@AutoConfiguration(
        after = {
            NzProtectionAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class
        })
public class IdempotencyAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({JdbcTemplate.class, PlatformTransactionManager.class})
    public IdempotencyExecutor idempotencyExecutor(
            JdbcTemplate jdbc,
            PlatformTransactionManager transactions,
            ObjectProvider<ObjectMapper> mappers) {
        return new IdempotencyExecutor(
                jdbc,
                transactions,
                mappers.getIfAvailable(() -> new ObjectMapper().findAndRegisterModules()));
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotentAspect idempotentAspect(
            ObjectProvider<IdempotencyExecutor> executors, ProtectionKeyResolver keys) {
        return new IdempotentAspect(executors, keys);
    }
}
