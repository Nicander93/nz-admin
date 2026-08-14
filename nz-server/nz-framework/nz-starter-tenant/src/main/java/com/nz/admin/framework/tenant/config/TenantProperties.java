package com.nz.admin.framework.tenant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 租户隔离配置。
 */
@Data
@ConfigurationProperties(prefix = "nz.tenant")
public class TenantProperties {

    private boolean enabled = false;
    private Long defaultTenantId = 1L;
    private String tenantColumn = "tenant_id";
    private Set<String> includedTables = new LinkedHashSet<>(Set.of(
            "sys_user", "sys_role", "sys_dept", "sys_dict_type", "sys_dict_data",
            "sys_user_role", "sys_role_menu", "sys_oper_log", "sys_login_log",
            "sys_post", "sys_user_post", "sys_config", "sys_notice", "sys_job",
            "sys_job_log", "sys_file", "sys_file_config", "demo_item",
            "sys_sms_channel", "sys_sms_template", "sys_sms_send_log", "sys_social", "sys_message",
            "flow_category", "flow_definition", "flow_instance", "flow_instance_event",
            "flow_task", "flow_history_task", "flow_task_copy"
    ));
}
