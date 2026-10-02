package com.nz.admin.modules.demo.config;

import com.nz.admin.framework.datascope.core.DataScopeRule;
import com.nz.admin.framework.datascope.core.DataScopeRuleCustomizer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DemoDataScopeConfiguration {
    @Bean
    public DataScopeRuleCustomizer demoDataScopeRules() {
        return rules -> rules.add(new DataScopeRule("demo_item", null, "owner_id"));
    }
}
