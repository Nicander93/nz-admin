package com.nz.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模块开关配置绑定测试。
 */
class ModulePropertiesBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "nz.modules.system.enabled=true",
                    "nz.modules.job.enabled=true",
                    "nz.modules.optional.enabled=false"
            );

    @Test
    void bindsModuleSwitchesFromNzModulesTree() {
        contextRunner.run(context -> {
            ModuleProperties properties = context.getBean(ModuleProperties.class);
            assertThat(properties.getEnabled("system")).isTrue();
            assertThat(properties.getEnabled("job")).isTrue();
            assertThat(properties.getEnabled("optional")).isFalse();
            assertThat(properties.getEnabled("missing")).isNull();
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ModuleProperties.class)
    static class TestConfiguration {
    }
}
