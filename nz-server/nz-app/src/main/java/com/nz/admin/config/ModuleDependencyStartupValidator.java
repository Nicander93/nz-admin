package com.nz.admin.config;

import com.nz.admin.common.module.NzModuleDependencyValidator;
import com.nz.admin.common.module.NzModuleDescriptor;
import com.nz.admin.common.module.NzModuleRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在应用启动时校验部署期模块开关与 manifest 依赖。
 */
@Component
public class ModuleDependencyStartupValidator {

    private final NzModuleRegistry registry;
    private final ModuleProperties properties;

    public ModuleDependencyStartupValidator(NzModuleRegistry registry, ModuleProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    @PostConstruct
    public void validate() {
        Set<String> enabled = registry.all().stream()
                .filter(this::isEnabled)
                .map(NzModuleDescriptor::code)
                .collect(Collectors.toSet());
        NzModuleDependencyValidator.validate(registry.all(), enabled);
    }

    private boolean isEnabled(NzModuleDescriptor descriptor) {
        Boolean configured = properties.getEnabled(descriptor.code());
        return configured == null ? descriptor.defaultEnabled() : configured;
    }
}
