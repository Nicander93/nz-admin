package com.nz.admin.modules.system.config;

import com.nz.admin.modules.system.service.auth.InMemorySmsVerificationCodeStore;
import com.nz.admin.modules.system.service.auth.SmsVerificationCodeStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 短信验证码业务配置。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SmsVerificationProperties.class)
public class SmsVerificationConfiguration {

    @Bean
    @ConditionalOnMissingBean(SmsVerificationCodeStore.class)
    public SmsVerificationCodeStore smsVerificationCodeStore() {
        return new InMemorySmsVerificationCodeStore();
    }
}
