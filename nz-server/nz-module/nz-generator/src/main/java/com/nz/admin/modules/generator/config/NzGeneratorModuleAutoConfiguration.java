package com.nz.admin.modules.generator.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * 代码生成模块自动装配。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "nz.modules.generator", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.nz.admin.modules.generator")
public class NzGeneratorModuleAutoConfiguration {
}
