package com.nz.admin.modules.demo.config;

import com.nz.admin.framework.tenant.core.TenantTableRuleCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LeaveTenantConfiguration {
    @Bean
    public TenantTableRuleCustomizer leaveTenantTables() {
        return tables -> tables.add("demo_leave");
    }
}
