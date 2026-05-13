package com.nz.admin.framework.monitor.config;

import com.nz.admin.framework.monitor.core.MonitorStatus;
import com.nz.admin.framework.monitor.core.MonitorStatusProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NzMonitorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NzMonitorAutoConfiguration.class));

    @Test
    void shouldProvideMonitorStatus() throws Exception {
        HealthEndpoint healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.health()).thenReturn(Health.up().build());

        contextRunner.withBean(HealthEndpoint.class, () -> healthEndpoint)
                .run(context -> {
                    assertThat(context).hasSingleBean(MonitorStatusProvider.class);
                    MonitorStatus status = context.getBean(MonitorStatusProvider.class).getStatus();
                    assertThat(status.getHealthStatus()).isEqualTo("UP");
                    assertThat(status.getHeapMaxBytes()).isGreaterThanOrEqualTo(0L);
                });
    }
}
