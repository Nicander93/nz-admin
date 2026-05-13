package com.nz.admin.framework.datascope.config;

import com.nz.admin.framework.datascope.DataScopeAspect;
import com.nz.admin.framework.datascope.DataScopeUserResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * 数据权限自动配置。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class NzDatascopeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DataScopeUserResolver.class)
    public DataScopeAspect dataScopeAspect(DataScopeUserResolver dataScopeUserResolver) {
        return new DataScopeAspect(dataScopeUserResolver);
    }
}
