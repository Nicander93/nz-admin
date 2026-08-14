package com.nz.admin.modules.demo.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * 示例模块自动装配。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "nz.modules.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.nz.admin.modules.demo")
@MapperScan("com.nz.admin.modules.demo.mapper")
public class NzDemoModuleAutoConfiguration {
}
