package com.nz.admin.modules.system.config;

import com.nz.admin.framework.datascope.core.DataScopeRule;
import com.nz.admin.framework.datascope.core.DataScopeRuleCustomizer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SystemDataScopeConfiguration {
    @Bean
    public DataScopeRuleCustomizer systemDataScopeRules() {
        return rules -> rules.add(new DataScopeRule("sys_user", "dept_id", "id"));
    }
}
