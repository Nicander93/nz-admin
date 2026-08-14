package com.nz.admin.modules.workflow.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/** 工作流模块自动装配。 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "nz.modules.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.nz.admin.modules.workflow")
@MapperScan("com.nz.admin.modules.workflow.mapper")
public class NzWorkflowModuleAutoConfiguration {
}
