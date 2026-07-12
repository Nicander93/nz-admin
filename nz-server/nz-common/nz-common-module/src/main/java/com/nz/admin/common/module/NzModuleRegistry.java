package com.nz.admin.common.module;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 读取 classpath 中 META-INF/nz/module.yaml 的只读注册表。
 *
 * <p>清单格式刻意保持简单，避免模块协议层引入 YAML/Spring 依赖。</p>
 */
public final class NzModuleRegistry {

    private final Map<String, NzModuleDescriptor> modules;

    private NzModuleRegistry(Map<String, NzModuleDescriptor> modules) {
        this.modules = Map.copyOf(modules);
    }

    public static NzModuleRegistry load(ClassLoader classLoader) {
        Map<String, NzModuleDescriptor> result = new LinkedHashMap<>();
        try {
            Enumeration<java.net.URL> resources = classLoader.getResources("META-INF/nz/module.yaml");
            while (resources.hasMoreElements()) {
                NzModuleDescriptor descriptor = parse(resources.nextElement().openStream());
                if (result.putIfAbsent(descriptor.code(), descriptor) != null) {
                    throw new IllegalStateException("duplicate module code: " + descriptor.code());
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("failed to load module manifests", e);
        }
        return new NzModuleRegistry(result);
    }

    public Collection<NzModuleDescriptor> all() {
        return modules.values();
    }

    public Optional<NzModuleDescriptor> find(String code) {
        return Optional.ofNullable(modules.get(code));
    }

    private static NzModuleDescriptor parse(InputStream input) throws Exception {
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, List<String>> lists = new LinkedHashMap<>();
        String currentList = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("-")) {
                    if (currentList == null) {
                        throw new IllegalArgumentException("list item without list key");
                    }
                    lists.computeIfAbsent(currentList, ignored -> new ArrayList<>())
                            .add(trimmed.substring(1).trim());
                    continue;
                }
                int index = trimmed.indexOf(':');
                if (index <= 0) {
                    throw new IllegalArgumentException("invalid module manifest line: " + trimmed);
                }
                String key = trimmed.substring(0, index).trim();
                String value = trimmed.substring(index + 1).trim();
                currentList = value.isEmpty() ? key : null;
                if (value.isEmpty()) {
                    lists.putIfAbsent(key, new ArrayList<>());
                } else {
                    values.put(key, value);
                }
            }
        }
        String code = values.get("code");
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("module code is required");
        }
        return new NzModuleDescriptor(code, values.getOrDefault("name", code), values.getOrDefault("version", ""),
                values.getOrDefault("description", ""), List.copyOf(lists.getOrDefault("requiredModules", List.of())),
                List.copyOf(lists.getOrDefault("requiredStarters", List.of())),
                Boolean.parseBoolean(values.getOrDefault("defaultEnabled", "true")),
                values.getOrDefault("frontendModule", code));
    }
}
