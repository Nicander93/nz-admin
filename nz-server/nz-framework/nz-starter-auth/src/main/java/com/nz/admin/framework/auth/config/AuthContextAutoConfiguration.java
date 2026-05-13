package com.nz.admin.framework.auth.config;

import com.nz.admin.framework.auth.core.AuthUserResolver;
import com.nz.admin.framework.auth.core.LoginUserContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 认证上下文自动配置。
 */
@AutoConfiguration
public class AuthContextAutoConfiguration {

    @Bean
    public LoginUserContext loginUserContext(ObjectProvider<AuthUserResolver> authUserResolverProvider) {
        return new LoginUserContext(authUserResolverProvider);
    }
}
