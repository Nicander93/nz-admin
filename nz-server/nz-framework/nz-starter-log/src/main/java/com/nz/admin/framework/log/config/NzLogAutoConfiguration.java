package com.nz.admin.framework.log.config;

import com.nz.admin.framework.log.aspect.OperationLogAspect;
import com.nz.admin.framework.log.properties.OperationLogProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 操作日志自动配置入口。
 */
@AutoConfiguration
@EnableConfigurationProperties(OperationLogProperties.class)
public class NzLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OperationLogAspect operationLogAspect(org.springframework.beans.factory.ObjectProvider<com.nz.admin.framework.log.core.OperationLogRecorder> operationLogRecorderProvider,
                                                 org.springframework.beans.factory.ObjectProvider<com.nz.admin.framework.auth.core.LoginUserContext> loginUserContextProvider,
                                                 OperationLogProperties properties) {
        return new OperationLogAspect(operationLogRecorderProvider, loginUserContextProvider, properties);
    }
}
