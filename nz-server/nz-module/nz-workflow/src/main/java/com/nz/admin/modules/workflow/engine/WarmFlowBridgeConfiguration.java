package com.nz.admin.modules.workflow.engine;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashSet;
import java.util.List;

/** 引擎使用静态上下文，初始化前必须先绑定本次 Spring 容器。 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "warm-flow.enabled", havingValue = "true")
public class WarmFlowBridgeConfiguration {
    /** Warm-Flow 的 tenant_id 是字符串，需要独立的 SQL 租户拦截器。 */
    @Bean
    @org.springframework.core.annotation.Order(1)
    public com.nz.admin.framework.mybatis.plugin.MybatisPlusInterceptorCustomizer
            warmFlowTenantInterceptor() {
        var tables =
                java.util.Set.of(
                        "flow_definition",
                        "flow_node",
                        "flow_skip",
                        "flow_instance",
                        "flow_task",
                        "flow_his_task",
                        "flow_user", "nz_workflow_business", "nz_workflow_event");
        return interceptor ->
                interceptor.addInnerInterceptor(
                        new com.baomidou.mybatisplus.extension.plugins.inner
                                .TenantLineInnerInterceptor(
                                new com.baomidou.mybatisplus.extension.plugins.handler
                                        .TenantLineHandler() {
                                    public net.sf.jsqlparser.expression.Expression getTenantId() {
                                        Long tenant =
                                                com.nz.admin.framework.tenant.core
                                                        .TenantContextHolder.getTenantIdOrNull();
                                        if (tenant == null)
                                            throw new com.nz.admin.common.core.BusinessException(
                                                    "新引擎操作需要可信租户身份");
                                        return new net.sf.jsqlparser.expression.StringValue(
                                                tenant.toString());
                                    }

                                    public boolean ignoreTable(String table) {
                                        return !tables.contains(
                                                table.toLowerCase(java.util.Locale.ROOT));
                                    }
                                }));
    }

    @Bean
    public static BeanFactoryPostProcessor warmFlowContextDependency() {
        return factory -> {
            // initFlow 返回同一个配置对象，明确选择配置属性 Bean，避免按类型查找出现两个候选。
            String properties =
                    "warm-flow-org.dromara.warm.plugin.modes.sb.config.WarmFlowProperties";
            if (factory.containsBeanDefinition(properties))
                factory.getBeanDefinition(properties).setPrimary(true);
            if (!factory.containsBeanDefinition("initFlow")) return;
            var definition = factory.getBeanDefinition("initFlow");
            var dependencies = new LinkedHashSet<String>();
            if (definition.getDependsOn() != null)
                dependencies.addAll(List.of(definition.getDependsOn()));
            dependencies.add("warmFlowSpringUtil");
            definition.setDependsOn(dependencies.toArray(String[]::new));
        };
    }
}
