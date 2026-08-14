package com.nz.admin.framework.mybatis.plugin;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;

/**
 * MyBatis-Plus 内部拦截器扩展点。
 *
 * <p>扩展拦截器会在分页拦截器之前注册，避免分页改写后的 SQL 绕过数据隔离。</p>
 */
@FunctionalInterface
public interface MybatisPlusInterceptorCustomizer {

    void customize(MybatisPlusInterceptor interceptor);
}
