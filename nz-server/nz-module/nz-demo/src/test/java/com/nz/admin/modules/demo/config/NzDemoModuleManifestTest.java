package com.nz.admin.modules.demo.config;

import com.nz.admin.common.module.NzModuleDescriptor;
import com.nz.admin.common.module.NzModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 示例模块清单测试。
 */
class NzDemoModuleManifestTest {

    @Test
    void exposesACompleteModuleManifest() {
        NzModuleDescriptor descriptor = NzModuleRegistry.load(getClass().getClassLoader())
                .find("demo")
                .orElseThrow();

        assertThat(descriptor.name()).isEqualTo("Demo Module");
        assertThat(descriptor.frontendModule()).isEqualTo("demo");
        assertThat(descriptor.defaultEnabled()).isTrue();
        assertThat(descriptor.requiredModules()).isEmpty();
        assertThat(descriptor.requiredStarters()).containsExactly("web", "mybatis", "auth", "log");
    }
}
