package com.nz.admin.framework.sms.config;

import com.nz.admin.framework.sms.core.SmsGateway;
import com.nz.admin.framework.sms.provider.LoggingSmsProvider;
import com.nz.admin.framework.sms.provider.WebhookSmsProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

class SmsAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SmsAutoConfiguration.class));

    @Test
    void registersGatewayAndBuiltInProvidersByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(SmsGateway.class);
            assertThat(context).hasSingleBean(LoggingSmsProvider.class);
            assertThat(context).hasSingleBean(WebhookSmsProvider.class);
        });
    }

    @Test
    void canDisableSmsCompletely() {
        runner.withPropertyValues("nz.sms.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(SmsGateway.class));
    }
}
