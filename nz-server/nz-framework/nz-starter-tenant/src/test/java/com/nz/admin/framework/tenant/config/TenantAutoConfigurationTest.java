package com.nz.admin.framework.tenant.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.nz.admin.framework.mybatis.plugin.MybatisPlusInterceptorCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class TenantAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TenantAutoConfiguration.class));

    @Test
    void staysDisabledByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(TenantLineHandler.class));
    }

    @Test
    void registersIsolationComponentsWhenEnabled() {
        contextRunner.withPropertyValues("nz.tenant.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(TenantLineHandler.class);
            assertThat(context).hasSingleBean(MybatisPlusInterceptorCustomizer.class);
            TenantLineHandler handler = context.getBean(TenantLineHandler.class);
            assertThat(handler.ignoreTable("sys_tenant")).isTrue();
            assertThat(handler.ignoreTable("sys_user")).isFalse();
            assertThat(handler.getTenantId().toString()).isEqualTo("1");
        });
    }
}
