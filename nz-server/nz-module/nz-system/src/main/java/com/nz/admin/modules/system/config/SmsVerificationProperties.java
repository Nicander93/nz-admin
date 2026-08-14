package com.nz.admin.modules.system.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/** 短信验证码生命周期配置。 */
@Data
@Validated
@ConfigurationProperties(prefix = "nz.sms.verification")
public class SmsVerificationProperties {

    private boolean enabled = true;
    @NotBlank
    private String templateCode = "verification-code";
    @Min(4)
    @Max(8)
    private int codeLength = 6;
    @Pattern(regexp = "^$|^\\d{4,8}$")
    private String fixedCode = "";
    @Min(1)
    @Max(10)
    private int maxAttempts = 5;
    private Duration ttl = Duration.ofMinutes(5);
    private Duration resendInterval = Duration.ofSeconds(60);
}
