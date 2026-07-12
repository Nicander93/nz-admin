package com.nz.admin.config;

import com.nz.admin.common.module.NzModuleRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 将 classpath 模块清单注册为应用只读注册表。
 */
@Configuration
public class ModuleRegistryConfiguration {

    @Bean
    public NzModuleRegistry nzModuleRegistry() {
        return NzModuleRegistry.load(Thread.currentThread().getContextClassLoader());
    }
}
