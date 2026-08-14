package com.nz.admin.framework.social.config;

import com.nz.admin.framework.social.core.DefaultSocialOAuthService;
import com.nz.admin.framework.social.core.InMemorySocialAuthorizationStateStore;
import com.nz.admin.framework.social.core.SocialAuthorizationStateStore;
import com.nz.admin.framework.social.core.SocialOAuthService;
import com.nz.admin.framework.social.properties.SocialProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

import java.security.SecureRandom;
import java.time.Clock;

/** 社交认证自动装配。 */
@AutoConfiguration
@EnableConfigurationProperties(SocialProperties.class)
@ConditionalOnProperty(prefix = "nz.social", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SocialAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public Clock socialClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecureRandom socialSecureRandom() {
        return new SecureRandom();
    }

    @Bean
    @ConditionalOnMissingBean
    public SocialAuthorizationStateStore socialAuthorizationStateStore(Clock clock) {
        return new InMemorySocialAuthorizationStateStore(clock);
    }

    @Bean
    @ConditionalOnMissingBean
    public SocialOAuthService socialOAuthService(SocialProperties properties,
                                                 SocialAuthorizationStateStore stateStore,
                                                 Clock clock,
                                                 SecureRandom secureRandom) {
        return new DefaultSocialOAuthService(
                properties, stateStore, RestClient.builder().build(), clock, secureRandom);
    }
}
