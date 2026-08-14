package com.nz.admin.framework.tenant.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.nz.admin.framework.mybatis.config.MybatisPlusAutoConfiguration;
import com.nz.admin.framework.mybatis.plugin.MybatisPlusInterceptorCustomizer;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import com.nz.admin.framework.tenant.task.TenantTaskDecorator;
import com.nz.admin.framework.tenant.web.TenantContextFilter;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.task.TaskDecorator;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 多租户自动装配。
 */
@AutoConfiguration
@AutoConfigureBefore(MybatisPlusAutoConfiguration.class)
@EnableConfigurationProperties(TenantProperties.class)
@ConditionalOnProperty(prefix = "nz.tenant", name = "enabled", havingValue = "true")
public class TenantAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TenantLineHandler tenantLineHandler(TenantProperties properties) {
        Set<String> includedTables = properties.getIncludedTables().stream()
                .map(table -> table.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        return new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContextHolder.getTenantIdOrNull();
                return new LongValue(tenantId == null ? properties.getDefaultTenantId() : tenantId);
            }

            @Override
            public String getTenantIdColumn() {
                return properties.getTenantColumn();
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return tableName == null || !includedTables.contains(tableName.toLowerCase(Locale.ROOT));
            }
        };
    }

    @Bean
    public MybatisPlusInterceptorCustomizer tenantInterceptorCustomizer(TenantLineHandler handler) {
        return interceptor -> interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(handler));
    }

    @Bean
    public FilterRegistrationBean<TenantContextFilter> tenantContextFilter() {
        FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantContextFilter());
        registration.setName("tenantContextFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(TaskDecorator.class)
    public TaskDecorator tenantTaskDecorator() {
        return new TenantTaskDecorator();
    }
}
