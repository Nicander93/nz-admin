package com.nz.admin.modules.system.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;

/**
 * system 业务模块的条件化自动装配入口。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "nz.modules.system", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.nz.admin.modules.system")
@MapperScan("com.nz.admin.modules.system.mapper")
public class NzSystemModuleAutoConfiguration {
}
