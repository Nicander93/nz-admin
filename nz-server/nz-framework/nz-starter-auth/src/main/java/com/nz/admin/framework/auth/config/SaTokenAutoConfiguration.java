package com.nz.admin.framework.auth.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.nz.admin.framework.auth.properties.AuthFrameworkProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(AuthFrameworkProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SaTokenAutoConfiguration implements WebMvcConfigurer {

    private final AuthFrameworkProperties properties;

    public SaTokenAutoConfiguration(AuthFrameworkProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    SaRouter.match(properties.getIncludePaths().toArray(String[]::new))
                            .notMatch(properties.getExcludePaths().toArray(String[]::new))
                            .check(r -> StpUtil.checkLogin());
                }))
                .addPathPatterns(properties.getIncludePaths().toArray(String[]::new));
    }
}
