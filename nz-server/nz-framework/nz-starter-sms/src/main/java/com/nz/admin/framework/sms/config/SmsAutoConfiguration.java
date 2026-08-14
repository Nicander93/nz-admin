package com.nz.admin.framework.sms.config;

import com.nz.admin.framework.sms.core.DefaultSmsGateway;
import com.nz.admin.framework.sms.core.SmsGateway;
import com.nz.admin.framework.sms.core.SmsProvider;
import com.nz.admin.framework.sms.properties.SmsProperties;
import com.nz.admin.framework.sms.provider.LoggingSmsProvider;
import com.nz.admin.framework.sms.provider.WebhookSmsProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import java.util.List;

/** 短信框架自动装配。 */
@AutoConfiguration
@EnableConfigurationProperties(SmsProperties.class)
@ConditionalOnProperty(prefix = "nz.sms", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SmsAutoConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "nz.sms", name = "logging-provider-enabled",
            havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(LoggingSmsProvider.class)
    public LoggingSmsProvider loggingSmsProvider() {
        return new LoggingSmsProvider();
    }

    @Bean
    @ConditionalOnMissingBean(WebhookSmsProvider.class)
    public WebhookSmsProvider webhookSmsProvider(SmsProperties properties) {
        return new WebhookSmsProvider(properties.getWebhookConnectTimeout(), properties.getWebhookReadTimeout());
    }

    @Bean
    @ConditionalOnMissingBean(SmsGateway.class)
    public SmsGateway smsGateway(List<SmsProvider> providers) {
        return new DefaultSmsGateway(providers);
    }
}
