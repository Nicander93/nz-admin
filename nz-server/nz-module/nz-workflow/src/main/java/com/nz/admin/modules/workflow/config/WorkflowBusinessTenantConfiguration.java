package com.nz.admin.modules.workflow.config;

import com.nz.admin.framework.tenant.core.TenantTableRuleCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** JDBC 服务也显式限定租户；此注册保证后续 Mapper 不会遗漏隔离。 */
@Configuration
public class WorkflowBusinessTenantConfiguration {
    @Bean
    public TenantTableRuleCustomizer workflowBusinessTables(com.nz.admin.framework.tenant.config.TenantProperties properties) {
        var names = java.util.Set.of("nz_workflow_business", "nz_workflow_event");
        return tables -> {
            // 与 Warm-Flow 表相同，VARCHAR 租户由字符串拦截器管理。
            tables.removeAll(names);
            properties.getExternallyManagedTables().addAll(names);
        };
    }
}
