package com.nz.admin.framework.mybatis.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.nz.admin.framework.mybatis.handler.DefaultDbFieldHandler;
import com.nz.admin.framework.mybatis.properties.MybatisFrameworkProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * MyBatis-Plus 自动配置。
 */
@AutoConfiguration
@EnableConfigurationProperties(MybatisFrameworkProperties.class)
public class MybatisPlusAutoConfiguration {

    /**
     * 注册 MyBatis-Plus 分页拦截器。
     *
     * @return MyBatis-Plus 拦截器
     */
    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisFrameworkProperties properties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(properties.getDbType()));
        return interceptor;
    }

    /**
     * 默认时间字段填充，可被业务自定义 {@link MetaObjectHandler} 覆盖。
     */
    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    public MetaObjectHandler metaObjectHandler() {
        return new DefaultDbFieldHandler();
    }
}
