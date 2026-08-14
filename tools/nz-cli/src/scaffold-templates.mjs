function lines(values) {
  return `${values.join('\n')}\n`
}

export function namesFor(code) {
  const words = code.split('-')
  const pascal = words.map((word) => word[0].toUpperCase() + word.slice(1)).join('')
  return {
    code,
    artifactId: `nz-${code}`,
    packageSegment: words.join(''),
    pascal,
  }
}

export function backendPom({ artifactId, title }) {
  return lines([
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<project xmlns="http://maven.apache.org/POM/4.0.0"',
    '         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"',
    '         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">',
    '    <modelVersion>4.0.0</modelVersion>',
    '',
    '    <parent>',
    '        <groupId>com.nz</groupId>',
    '        <artifactId>nz-module</artifactId>',
    '        <version>0.0.1-SNAPSHOT</version>',
    '        <relativePath>../pom.xml</relativePath>',
    '    </parent>',
    '',
    `    <artifactId>${artifactId}</artifactId>`,
    `    <name>${artifactId}</name>`,
    `    <description>${escapeXml(title)}业务模块。</description>`,
    '',
    '    <dependencies>',
    '        <dependency>',
    '            <groupId>com.nz</groupId>',
    '            <artifactId>nz-common-core</artifactId>',
    '        </dependency>',
    '        <dependency>',
    '            <groupId>com.nz</groupId>',
    '            <artifactId>nz-starter-web</artifactId>',
    '        </dependency>',
    '        <dependency>',
    '            <groupId>com.nz</groupId>',
    '            <artifactId>nz-starter-mybatis</artifactId>',
    '        </dependency>',
    '        <dependency>',
    '            <groupId>com.nz</groupId>',
    '            <artifactId>nz-starter-auth</artifactId>',
    '        </dependency>',
    '        <dependency>',
    '            <groupId>com.nz</groupId>',
    '            <artifactId>nz-common-module</artifactId>',
    '            <scope>test</scope>',
    '        </dependency>',
    '        <dependency>',
    '            <groupId>com.nz</groupId>',
    '            <artifactId>nz-starter-test</artifactId>',
    '            <scope>test</scope>',
    '        </dependency>',
    '    </dependencies>',
    '</project>',
  ])
}

export function autoConfiguration({ packageSegment, pascal, code }) {
  return lines([
    `package com.nz.admin.modules.${packageSegment}.config;`,
    '',
    'import org.mybatis.spring.annotation.MapperScan;',
    'import org.springframework.boot.autoconfigure.AutoConfiguration;',
    'import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;',
    'import org.springframework.context.annotation.ComponentScan;',
    '',
    '/**',
    ` * ${pascal} 模块自动装配。`,
    ' */',
    '@AutoConfiguration',
    `@ConditionalOnProperty(prefix = "nz.modules.${code}", name = "enabled", havingValue = "true", matchIfMissing = true)`,
    `@ComponentScan(basePackages = "com.nz.admin.modules.${packageSegment}")`,
    `@MapperScan("com.nz.admin.modules.${packageSegment}.mapper")`,
    `public class Nz${pascal}ModuleAutoConfiguration {`,
    '}',
  ])
}

export function infoController({ packageSegment, code, title }) {
  return lines([
    `package com.nz.admin.modules.${packageSegment}.controller;`,
    '',
    'import com.nz.admin.common.core.R;',
    'import com.nz.admin.framework.auth.annotation.SaCheckPermission;',
    'import org.springframework.web.bind.annotation.GetMapping;',
    'import org.springframework.web.bind.annotation.RequestMapping;',
    'import org.springframework.web.bind.annotation.RestController;',
    '',
    'import java.util.Map;',
    '',
    '/**',
    ` * ${escapeJava(title)}模块入口。`,
    ' */',
    '@RestController',
    `@RequestMapping("/api/${code}")`,
    'public class ModuleInfoController {',
    '',
    `    @SaCheckPermission("${code}:module:view")`,
    '    @GetMapping("/info")',
    '    public R<Map<String, String>> info() {',
    `        return R.ok(Map.of("code", "${code}", "name", "${escapeJava(title)}"));`,
    '    }',
    '}',
  ])
}

export function packageInfo({ packageSegment, title }) {
  return lines([
    `/** ${escapeJava(title)}业务模块。 */`,
    `package com.nz.admin.modules.${packageSegment};`,
  ])
}

export function moduleManifest({ code, title, description }) {
  return lines([
    `code: ${code}`,
    `name: ${escapeYaml(title)}`,
    'version: 0.0.1-SNAPSHOT',
    `description: ${escapeYaml(description)}`,
    'requiredModules: []',
    'requiredStarters:',
    '  - web',
    '  - mybatis',
    '  - auth',
    'defaultEnabled: true',
    `frontendModule: ${code}`,
  ])
}

export function autoConfigurationImport({ packageSegment, pascal }) {
  return `com.nz.admin.modules.${packageSegment}.config.Nz${pascal}ModuleAutoConfiguration\n`
}

export function manifestTest({ packageSegment, pascal, code }) {
  return lines([
    `package com.nz.admin.modules.${packageSegment};`,
    '',
    'import com.nz.admin.common.module.NzModuleRegistry;',
    'import org.junit.jupiter.api.Test;',
    '',
    'import static org.assertj.core.api.Assertions.assertThat;',
    '',
    `class Nz${pascal}ModuleManifestTest {`,
    '',
    '    @Test',
    '    void exposesModuleManifest() {',
    '        var descriptor = NzModuleRegistry.load(getClass().getClassLoader())',
    `                .find("${code}")`,
    '                .orElseThrow();',
    '',
    `        assertThat(descriptor.frontendModule()).isEqualTo("${code}");`,
    '        assertThat(descriptor.defaultEnabled()).isTrue();',
    '    }',
    '}',
  ])
}

export function frontendManifest({ code, title }) {
  return lines([
    "import type { FrontendModuleManifest } from '@/core/modules/types'",
    '',
    'const manifest: FrontendModuleManifest = {',
    `  code: '${code}',`,
    `  title: '${escapeTs(title)}',`,
    `  componentPrefix: '${code}',`,
    '}',
    '',
    'export default manifest',
  ])
}

export function frontendApi({ code }) {
  return lines([
    "import request from '@/api/request'",
    '',
    'export interface ModuleInfo {',
    '  code: string',
    '  name: string',
    '}',
    '',
    'export function getModuleInfo() {',
    `  return request.get<ModuleInfo>('/api/${code}/info')`,
    '}',
  ])
}

export function frontendPage({ code, title }) {
  return lines([
    '<template>',
    '  <div class="page-container">',
    '    <el-card v-loading="loading" shadow="never">',
    `      <template #header>${escapeHtml(title)}</template>`,
    '      <el-descriptions v-if="info" :column="1" border>',
    '        <el-descriptions-item label="模块编码">{{ info.code }}</el-descriptions-item>',
    '        <el-descriptions-item label="模块名称">{{ info.name }}</el-descriptions-item>',
    '      </el-descriptions>',
    '    </el-card>',
    '  </div>',
    '</template>',
    '',
    '<script setup lang="ts">',
    "import { onMounted, ref } from 'vue'",
    `import { getModuleInfo, type ModuleInfo } from '@/api/${code}'`,
    '',
    'const loading = ref(false)',
    'const info = ref<ModuleInfo>()',
    '',
    'onMounted(async () => {',
    '  loading.value = true',
    '  try {',
    '    const response = await getModuleInfo()',
    '    info.value = response.data',
    '  } finally {',
    '    loading.value = false',
    '  }',
    '})',
    '</script>',
  ])
}

export function frontendManifestTest({ code }) {
  return lines([
    "import { describe, expect, it } from 'vitest'",
    "import { getFrontendModuleManifests } from '@/core/modules/registry'",
    '',
    `describe('${code} module manifest', () => {`,
    "  it('is discovered by the frontend registry', () => {",
    `    expect(getFrontendModuleManifests().some((manifest) => manifest.code === '${code}')).toBe(true)`,
    '  })',
    '})',
  ])
}

export function menuMigration({ code, title, parentMenuId, menuId }) {
  const sqlTitle = escapeSql(title)
  return lines([
    'INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, type, perm, visible, status)',
    `VALUES (${menuId}, ${parentMenuId}, '${sqlTitle}', '${code}', '${code}/index', 'Menu', 1, 'C',`,
    `        '${code}:module:view', 0, 0)`,
    'ON CONFLICT (id) DO UPDATE SET',
    '    parent_id = EXCLUDED.parent_id, name = EXCLUDED.name, path = EXCLUDED.path,',
    '    component = EXCLUDED.component, icon = EXCLUDED.icon, sort = EXCLUDED.sort,',
    '    perm = EXCLUDED.perm, visible = EXCLUDED.visible, status = EXCLUDED.status;',
    '',
    'INSERT INTO sys_role_menu (role_id, menu_id)',
    'SELECT role.id, menu.id',
    'FROM sys_role role',
    `JOIN sys_menu menu ON menu.id = ${menuId}`,
    "WHERE role.role_key = 'admin'",
    'ON CONFLICT DO NOTHING;',
  ])
}

function escapeXml(value) {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
}

function escapeJava(value) {
  return value.replaceAll('\\', '\\\\').replaceAll('"', '\\"').replaceAll('\r', '\\r').replaceAll('\n', '\\n')
}

function escapeTs(value) {
  return value.replaceAll('\\', '\\\\').replaceAll("'", "\\'").replaceAll('\r', '\\r').replaceAll('\n', '\\n')
}

function escapeHtml(value) {
  return escapeXml(value).replaceAll('"', '&quot;')
}

function escapeSql(value) {
  return value.replaceAll("'", "''")
}

function escapeYaml(value) {
  return JSON.stringify(value)
}
