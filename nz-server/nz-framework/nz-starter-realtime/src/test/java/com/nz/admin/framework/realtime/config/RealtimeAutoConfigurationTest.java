package com.nz.admin.framework.realtime.config;

import com.nz.admin.framework.realtime.core.RealtimeConnectionRegistry;
import com.nz.admin.framework.realtime.core.RealtimePublisher;
import com.nz.admin.framework.realtime.core.RealtimeTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimeAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(
                            JacksonAutoConfiguration.class,
                            RealtimeAutoConfiguration.class
                    ));

    @Test
    void createsRealtimeInfrastructureByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RealtimeTicketService.class);
            assertThat(context).hasSingleBean(RealtimeConnectionRegistry.class);
            assertThat(context).hasSingleBean(RealtimePublisher.class);
            assertThat(context).hasBean("realtimeWebSocketConfiguration");
            assertThat(context).hasBean("realtimeSseController");
        });
    }

    @Test
    void canDisableRealtimeInfrastructure() {
        contextRunner
                .withPropertyValues("nz.realtime.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RealtimeTicketService.class);
                    assertThat(context).doesNotHaveBean(RealtimePublisher.class);
                });
    }
}
