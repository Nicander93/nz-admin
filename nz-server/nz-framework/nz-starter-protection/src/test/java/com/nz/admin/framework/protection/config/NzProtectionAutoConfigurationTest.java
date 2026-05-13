package com.nz.admin.framework.protection.config;

import com.nz.admin.framework.protection.core.InMemoryProtectionStore;
import com.nz.admin.framework.protection.core.ProtectionKeyResolver;
import com.nz.admin.framework.protection.support.XssCleaner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class NzProtectionAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NzProtectionAutoConfiguration.class));

    @Test
    void shouldRegisterProtectionBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(InMemoryProtectionStore.class);
            assertThat(context).hasSingleBean(ProtectionKeyResolver.class);
            assertThat(context).hasSingleBean(XssCleaner.class);

            XssCleaner xssCleaner = context.getBean(XssCleaner.class);
            assertThat(xssCleaner.clean("<script>alert(1)</script>", false)).doesNotContain("<script>");
        });
    }
}
