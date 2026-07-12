package com.nz.admin.common.module;

import java.util.List;

/** classpath 模块清单。 */
public record NzModuleDescriptor(String code, String name, String version, String description,
                                 List<String> requiredModules, List<String> requiredStarters,
                                 boolean defaultEnabled, String frontendModule) {
}
