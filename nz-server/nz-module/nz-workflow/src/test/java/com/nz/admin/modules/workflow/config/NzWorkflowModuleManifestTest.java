package com.nz.admin.modules.workflow.config;

import com.nz.admin.common.module.NzModuleDescriptor;
import com.nz.admin.common.module.NzModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工作流模块清单测试。
 */
class NzWorkflowModuleManifestTest {

    @Test
    void exposesACompleteModuleManifest() {
        NzModuleDescriptor descriptor = NzModuleRegistry.load(getClass().getClassLoader())
                .find("workflow")
                .orElseThrow();

        assertThat(descriptor.name()).isEqualTo("Workflow Module");
        assertThat(descriptor.frontendModule()).isEqualTo("workflow");
        assertThat(descriptor.defaultEnabled()).isTrue();
        assertThat(descriptor.requiredModules()).isEmpty();
        assertThat(descriptor.requiredStarters()).containsExactly(
                "web", "mybatis", "tenant", "auth", "log", "protection", "excel");
    }
}
