package com.nz.admin.modules.workflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 使用官方同版本资源，接口仍由本模块进行权限和租户校验。 */
@Configuration
@org.springframework.scheduling.annotation.EnableScheduling
public class WorkflowDesignerConfiguration implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/workflow/designer/ui/**")
                .addResourceLocations("classpath:/META-INF/resources/warm-flow-ui/");
    }
}
