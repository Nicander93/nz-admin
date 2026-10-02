package com.nz.admin.framework.datascope.config;

import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.nz.admin.framework.datascope.core.DataScopeResolver;
import com.nz.admin.framework.datascope.core.DataScopeRule;
import com.nz.admin.framework.datascope.core.DataScopeRuleCustomizer;
import com.nz.admin.framework.datascope.support.DataScopeHandler;
import com.nz.admin.framework.mybatis.config.MybatisPlusAutoConfiguration;
import com.nz.admin.framework.mybatis.plugin.MybatisPlusInterceptorCustomizer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@AutoConfigureBefore(MybatisPlusAutoConfiguration.class)
public class NzDatascopeAutoConfiguration {
    @Bean
    public DataScopeHandler dataScopeHandler(
            ObjectProvider<DataScopeRuleCustomizer> customizers,
            ObjectProvider<DataScopeResolver> resolver) {
        List<DataScopeRule> rules = new ArrayList<>();
        customizers.orderedStream().forEach(customizer -> customizer.customize(rules));
        return new DataScopeHandler(
                rules,
                () -> {
                    DataScopeResolver value = resolver.getIfAvailable();
                    if (value == null) {
                        throw new IllegalStateException("受保护表缺少 DataScopeResolver");
                    }
                    return value.resolve();
                });
    }

    @Bean
    public MybatisPlusInterceptorCustomizer dataScopeInterceptorCustomizer(
            DataScopeHandler handler) {
        return new OrderedCustomizer(handler);
    }

    // 租户插件先运行，数据范围随后执行，分页由 MyBatis starter 最后加入。
    private record OrderedCustomizer(DataScopeHandler handler)
            implements MybatisPlusInterceptorCustomizer, Ordered {
        @Override
        public int getOrder() {
            return 100;
        }

        @Override
        public void customize(
                com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor interceptor) {
            interceptor.addInnerInterceptor(new DataPermissionInterceptor(handler));
        }
    }
}
