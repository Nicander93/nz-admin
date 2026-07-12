package com.nz.admin.common.module;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 校验已启用模块的必需模块是否存在。
 */
public final class NzModuleDependencyValidator {

    private NzModuleDependencyValidator() {
    }

    public static void validate(Collection<NzModuleDescriptor> descriptors, Set<String> enabledCodes) {
        Set<String> presentCodes = new HashSet<>();
        descriptors.forEach(item -> presentCodes.add(item.code()));
        for (NzModuleDescriptor descriptor : descriptors) {
            if (!enabledCodes.contains(descriptor.code())) {
                continue;
            }
            for (String requiredCode : descriptor.requiredModules()) {
                if (!presentCodes.contains(requiredCode)) {
                    throw new IllegalStateException("module " + descriptor.code()
                            + " requires missing module " + requiredCode);
                }
                if (!enabledCodes.contains(requiredCode)) {
                    throw new IllegalStateException("module " + descriptor.code()
                            + " requires enabled module " + requiredCode);
                }
            }
        }
    }
}
