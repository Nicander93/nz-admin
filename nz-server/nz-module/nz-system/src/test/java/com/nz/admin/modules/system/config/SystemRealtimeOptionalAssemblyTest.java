package com.nz.admin.modules.system.config;

import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.realtime.config.RealtimeAutoConfiguration;
import com.nz.admin.modules.system.controller.realtime.RealtimeController;
import com.nz.admin.modules.system.service.realtime.RealtimeService;
import com.nz.admin.modules.system.service.realtime.RealtimeServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SystemRealtimeOptionalAssemblyTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withPropertyValues("nz.cluster.enabled=false")
            .withConfiguration(AutoConfigurations.of(
                    JacksonAutoConfiguration.class,
                    RealtimeAutoConfiguration.class,
                    SystemRealtimeComponents.class
            ))
            .withBean(LoginUserContext.class, () -> mock(LoginUserContext.class));

    @Test
    void startsSystemRealtimeComponentsDisabled() {
        contextRunner.withPropertyValues("nz.realtime.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(RealtimeService.class);
            assertThat(context).doesNotHaveBean(RealtimeController.class);
        });
    }

    @Test
    void createsSystemRealtimeComponentsWhenEnabled() {
        contextRunner.withPropertyValues("nz.realtime.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(RealtimeService.class);
            assertThat(context).hasSingleBean(RealtimeController.class);
        });
    }

    @AutoConfiguration(after = RealtimeAutoConfiguration.class)
    @ComponentScan(basePackageClasses = {RealtimeController.class, RealtimeServiceImpl.class})
    static class SystemRealtimeComponents {}
}
