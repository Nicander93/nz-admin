package com.nz.admin.controller;

import com.nz.admin.common.core.R;
import com.nz.admin.common.module.NzModuleDescriptor;
import com.nz.admin.common.module.NzModuleRegistry;
import com.nz.admin.common.module.NzModuleState;
import com.nz.admin.config.ModuleProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 已装配模块的只读状态接口。
 */
@RestController
@RequestMapping("/api/system/modules")
@EnableConfigurationProperties(ModuleProperties.class)
public class ModuleController {

    private final NzModuleRegistry registry;
    private final ModuleProperties properties;

    public ModuleController(NzModuleRegistry registry, ModuleProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    @GetMapping
    public R<List<ModuleItem>> list() {
        List<ModuleItem> result = registry.all().stream().map(this::toItem).toList();
        return R.ok(result);
    }

    private ModuleItem toItem(NzModuleDescriptor descriptor) {
        Boolean configured = properties.getEnabled(descriptor.code());
        boolean enabled = configured == null ? descriptor.defaultEnabled() : configured;
        return new ModuleItem(descriptor.code(), descriptor.name(), descriptor.version(),
                descriptor.description(), descriptor.frontendModule(),
                enabled ? NzModuleState.ENABLED : NzModuleState.DISABLED);
    }

    public record ModuleItem(String code, String name, String version, String description,
                             String frontendModule, NzModuleState state) {
    }
}
