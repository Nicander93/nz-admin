package com.nz.admin.modules.generator.template;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.tools.ToolProvider;

/** 编译最终生成的 Java，防止模板断言通过但用户下载后无法编译。 */
class GeneratedCompilationTest {
    @TempDir Path directory;

    @Test
    void generatedTenantAndOwnedCrudCompilesWithoutClientOwnershipFields() throws Exception {
        var columns = new ArrayList<>(GeneratorTemplateRendererTest.columns());
        for (String name : new String[] {"tenant_id", "dept_id", "owner_id"}) {
            var column = new com.nz.admin.modules.generator.model.GeneratorColumn();
            column.setColumnName(name);
            column.setOrdinalPosition(columns.size() + 1);
            column.setDataType("bigint");
            column.setUdtName("int8");
            column.setNullable(false);
            columns.add(column);
        }
        var files =
                new GeneratorTemplateRenderer()
                        .render(GeneratorTemplateRendererTest.request(), columns);
        var sources = new ArrayList<String>();
        for (var entry : files.entrySet()) {
            if (!entry.getKey().endsWith(".java")) continue;
            Path file = directory.resolve(entry.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, entry.getValue());
            sources.add(file.toString());
            if (file.getFileName().toString().endsWith("CreateRequest.java"))
                assertThat(entry.getValue()).doesNotContain("tenantId", "ownerId", "deptId");
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("验证需要 JDK 而不是 JRE").isNotNull();
        var arguments = new ArrayList<String>();
        arguments.addAll(
                java.util.List.of(
                        "-classpath",
                        System.getProperty("java.class.path"),
                        "-d",
                        directory.toString()));
        arguments.addAll(sources);
        assertThat(compiler.run(null, null, null, arguments.toArray(String[]::new))).isZero();
        assertThat(files.values()).anyMatch(value -> value.contains("TenantTableRuleCustomizer"));
        assertThat(files.values()).anyMatch(value -> value.contains("DataScopeRuleCustomizer"));
    }
}
