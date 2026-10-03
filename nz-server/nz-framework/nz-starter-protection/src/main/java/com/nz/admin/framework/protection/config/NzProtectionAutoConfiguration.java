package com.nz.admin.framework.protection.config;

import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.protection.aspect.RateLimitAspect;
import com.nz.admin.framework.protection.aspect.RepeatSubmitAspect;
import com.nz.admin.framework.protection.core.InMemoryProtectionStore;
import com.nz.admin.framework.protection.core.ProtectionStore;
import com.nz.admin.framework.protection.core.ProtectionKeyResolver;
import com.nz.admin.framework.protection.support.XssCleaner;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 保护能力自动配置。
 */
@AutoConfiguration(after = com.nz.admin.framework.cache.config.AtomicStateAutoConfiguration.class)
public class NzProtectionAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ProtectionStore.class)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="nz.cluster.enabled", havingValue="true")
    public ProtectionStore sharedProtectionStore(com.nz.admin.framework.cache.core.AtomicStateStore states) {
        return new com.nz.admin.framework.protection.core.SharedProtectionStore(states);
    }


    @Bean
    @ConditionalOnMissingBean(ProtectionStore.class)
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="nz.cluster.enabled",havingValue="false",matchIfMissing=true)
    public InMemoryProtectionStore inMemoryProtectionStore() {
        return new InMemoryProtectionStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProtectionKeyResolver protectionKeyResolver(ObjectProvider<LoginUserContext> loginUserContextProvider) {
        return new ProtectionKeyResolver(loginUserContextProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean
    public RepeatSubmitAspect repeatSubmitAspect(ProtectionStore protectionStore,
                                                 ProtectionKeyResolver protectionKeyResolver) {
        return new RepeatSubmitAspect(protectionStore, protectionKeyResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitAspect rateLimitAspect(ProtectionStore protectionStore,
                                           ProtectionKeyResolver protectionKeyResolver) {
        return new RateLimitAspect(protectionStore, protectionKeyResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public XssCleaner xssCleaner() {
        return new XssCleaner();
    }
}
