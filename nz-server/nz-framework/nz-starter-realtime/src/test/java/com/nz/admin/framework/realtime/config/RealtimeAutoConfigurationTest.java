package com.nz.admin.framework.realtime.config;

import com.nz.admin.framework.realtime.core.RealtimeConnectionRegistry;
import com.nz.admin.framework.realtime.core.RealtimePublisher;
import com.nz.admin.framework.realtime.core.RealtimeTicketService;
import com.nz.admin.framework.realtime.core.RealtimePrincipal;
import com.nz.admin.framework.realtime.core.RealtimeTransport;
import com.nz.admin.common.core.identity.IdentityValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RealtimeAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withPropertyValues("nz.cluster.enabled=false")
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
    @Test
    void refusesTicketsWhenNoIdentityValidatorIsConfigured() {
        contextRunner.run(context -> assertThatThrownBy(() -> context.getBean(RealtimeTicketService.class)
                .issue(new RealtimePrincipal(7L, 3L, 0L, 0L), RealtimeTransport.SSE))
                .isInstanceOf(IllegalArgumentException.class));
    }

    @Test
    void usesTheProvidedValidatorForTicketIssuanceAndConsumption() {
        contextRunner.withBean(IdentityValidator.class, () -> identity -> identity.isComplete()).run(context -> {
            var tickets = context.getBean(RealtimeTicketService.class);
            assertThatThrownBy(() -> tickets.issue(new RealtimePrincipal(7L, 3L), RealtimeTransport.SSE))
                    .isInstanceOf(IllegalArgumentException.class);
            var principal = new RealtimePrincipal(7L, 3L, 0L, 0L);
            assertThat(tickets.consume(tickets.issue(principal, RealtimeTransport.SSE), RealtimeTransport.SSE))
                    .contains(principal);
        });
    }
}
