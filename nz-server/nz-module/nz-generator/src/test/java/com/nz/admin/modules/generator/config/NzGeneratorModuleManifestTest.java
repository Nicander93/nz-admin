package com.nz.admin.modules.generator.config;

import com.nz.admin.common.module.NzModuleDescriptor;
import com.nz.admin.common.module.NzModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 代码生成模块清单测试。
 */
class NzGeneratorModuleManifestTest {

    @Test
    void exposesACompleteModuleManifest() {
        NzModuleDescriptor descriptor = NzModuleRegistry.load(getClass().getClassLoader())
                .find("generator")
                .orElseThrow();

        assertThat(descriptor.name()).isEqualTo("Code Generator");
        assertThat(descriptor.frontendModule()).isEqualTo("generator");
        assertThat(descriptor.defaultEnabled()).isTrue();
        assertThat(descriptor.requiredModules()).isEmpty();
        assertThat(descriptor.requiredStarters()).containsExactly("web", "auth");
    }
}
