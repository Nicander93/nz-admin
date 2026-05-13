package com.nz.admin.framework.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisPlusAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MybatisPlusAutoConfiguration.class));

    @Test
    void shouldRegisterConfiguredPaginationInterceptor() {
        contextRunner.withPropertyValues("nz.mybatis.db-type=MYSQL").run(context -> {
            assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
            assertThat(context).hasSingleBean(MetaObjectHandler.class);

            MybatisPlusInterceptor interceptor = context.getBean(MybatisPlusInterceptor.class);

            assertThat(interceptor.getInterceptors()).hasSize(1);
            assertThat(interceptor.getInterceptors().get(0)).isInstanceOf(PaginationInnerInterceptor.class);

            PaginationInnerInterceptor innerInterceptor =
                    (PaginationInnerInterceptor) interceptor.getInterceptors().get(0);
            assertThat(ReflectionTestUtils.getField(innerInterceptor, "dbType")).isEqualTo(DbType.MYSQL);
        });
    }

    @Test
    void shouldUsePostgresByDefault() {
        contextRunner.run(context -> {
            MybatisPlusInterceptor interceptor = context.getBean(MybatisPlusInterceptor.class);
            PaginationInnerInterceptor innerInterceptor =
                    (PaginationInnerInterceptor) interceptor.getInterceptors().get(0);
            assertThat(ReflectionTestUtils.getField(innerInterceptor, "dbType")).isEqualTo(DbType.POSTGRE_SQL);
        });
    }

    @Test
    void shouldBackOffWhenCustomInterceptorExists() {
        MybatisPlusInterceptor customInterceptor = new MybatisPlusInterceptor();

        contextRunner.withBean(MybatisPlusInterceptor.class, () -> customInterceptor)
                .run(context -> {
                    assertThat(context).hasSingleBean(MybatisPlusInterceptor.class);
                    assertThat(context.getBean(MybatisPlusInterceptor.class)).isSameAs(customInterceptor);
                });
    }
}
