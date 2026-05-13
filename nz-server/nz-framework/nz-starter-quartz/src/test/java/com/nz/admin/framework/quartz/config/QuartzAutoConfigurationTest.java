package com.nz.admin.framework.quartz.config;

import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class QuartzAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(QuartzAutoConfiguration.class));

    @Test
    void shouldCreateSchedulerBean() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Scheduler.class);
            assertThat(context.getBean(Scheduler.class).getSchedulerName()).isNotBlank();
        });
    }
}
