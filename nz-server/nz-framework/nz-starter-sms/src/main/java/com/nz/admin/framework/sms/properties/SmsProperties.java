package com.nz.admin.framework.sms.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

/** 短信框架配置。 */
@Data
@ConfigurationProperties(prefix = "nz.sms")
public class SmsProperties {
    private boolean enabled = true;
    private boolean loggingProviderEnabled = true;
    private Duration webhookConnectTimeout = Duration.ofSeconds(3);
    private Duration webhookReadTimeout = Duration.ofSeconds(5);
}
