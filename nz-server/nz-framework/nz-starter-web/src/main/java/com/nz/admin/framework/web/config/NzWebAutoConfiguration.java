package com.nz.admin.framework.web.config;

import com.nz.admin.framework.web.properties.WebFrameworkProperties;
import com.nz.admin.framework.web.support.RequestTraceFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 通用自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(WebFrameworkProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class NzWebAutoConfiguration {

    /**
     * 配置全局跨域。
     *
     * @return WebMvcConfigurer
     */
    @Bean
    @ConditionalOnProperty(prefix = "nz.web.cors", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WebMvcConfigurer nzCorsWebMvcConfigurer(WebFrameworkProperties properties) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                WebFrameworkProperties.Cors cors = properties.getCors();
                registry.addMapping("/**")
                        .allowedOriginPatterns(cors.getAllowedOriginPatterns().toArray(String[]::new))
                        .allowedMethods(cors.getAllowedMethods().toArray(String[]::new))
                        .allowedHeaders(cors.getAllowedHeaders().toArray(String[]::new))
                        .allowCredentials(cors.isAllowCredentials())
                        .maxAge(cors.getMaxAge());
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "nz.web.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RequestTraceFilter requestTraceFilter(WebFrameworkProperties properties) {
        return new RequestTraceFilter(properties.getTrace());
    }
}
