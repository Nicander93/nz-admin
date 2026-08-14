import { access, readdir } from 'node:fs/promises'
import path from 'node:path'
import { applyChanges } from './backup.mjs'
import { CliError, invariant } from './errors.mjs'
import { inspectMigrations } from './migrations.mjs'
import { setModuleEnabled } from './module-config.mjs'
import { readUtf8, relativeDisplay } from './project.mjs'
import {
  autoConfiguration,
  autoConfigurationImport,
  backendPom,
  frontendApi,
  frontendManifest,
  frontendManifestTest,
  frontendPage,
  infoController,
  manifestTest,
  menuMigration,
  moduleManifest,
  namesFor,
  packageInfo,
} from './scaffold-templates.mjs'

const CODE_PATTERN = /^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/

async function exists(file) {
  try {
    await access(file)
    return true
  } catch {
    return false
  }
}

function validateText(value, field, maximum) {
  invariant(typeof value === 'string' && value.trim() !== '', `${field}不能为空`)
  invariant(!/[\r\n]/.test(value), `${field}不能包含换行`)
  invariant(value.length <= maximum, `${field}不能超过 ${maximum} 个字符`)
  return value.trim()
}

function insertModule(pom, artifactId) {
  const entry = `<module>${artifactId}</module>`
  if (pom.includes(entry)) throw new CliError(`Maven 聚合模块已存在：${artifactId}`)
  const closing = pom.indexOf('    </modules>')
  if (closing === -1) throw new CliError('nz-module/pom.xml 缺少 </modules>')
  return `${pom.slice(0, closing)}        ${entry}\n${pom.slice(closing)}`
}

function dependencyBlock(artifactId) {
  return [
    '            <dependency>',
    '                <groupId>com.nz</groupId>',
    `                <artifactId>${artifactId}</artifactId>`,
    '                <version>${project.version}</version>',
    '            </dependency>',
  ].join('\n')
}

function appDependencyBlock(artifactId) {
  return [
    '        <dependency>',
    '            <groupId>com.nz</groupId>',
    `            <artifactId>${artifactId}</artifactId>`,
    '        </dependency>',
  ].join('\n')
}

function insertDependency(pom, artifactId, block) {
  if (pom.includes(`<artifactId>${artifactId}</artifactId>`)) {
    throw new CliError(`Maven 依赖已存在：${artifactId}`)
  }
  const closing = pom.indexOf('</dependencies>')
  if (closing === -1) throw new CliError('pom.xml 缺少 </dependencies>')
  const lineStart = pom.lastIndexOf('\n', closing) + 1
  return `${pom.slice(0, lineStart)}${block}\n${pom.slice(lineStart)}`
}

function appendMigrationResource(testSource, migrationPath) {
  if (testSource.includes(`"${migrationPath}"`)) throw new CliError(`迁移测试已包含 ${migrationPath}`)
  const pattern = /(\s+"db\/migration\/V\d+__[a-z0-9_]+\.sql")(\r?\n\s+\);)/
  if (!pattern.test(testSource)) throw new CliError('无法定位 FlywayMigrationResourcesTest.MIGRATIONS')
  return testSource.replace(pattern, `$1,\n            "${migrationPath}"$2`)
}

async function nextMenuId(migrationDirectory) {
  let maximum = 0
  const files = (await readdir(migrationDirectory)).filter((name) => name.endsWith('.sql'))
  for (const name of files) {
    const content = await readUtf8(path.join(migrationDirectory, name))
    const blocks = content.match(/INSERT\s+INTO\s+sys_menu[\s\S]*?;/gi) ?? []
    for (const block of blocks) {
      for (const match of block.matchAll(/\(\s*(\d+)\s*,/g)) maximum = Math.max(maximum, Number(match[1]))
    }
  }
  return Math.ceil((maximum + 1) / 10) * 10
}

export async function planModuleAdd(root, input) {
  const code = input.code
  invariant(CODE_PATTERN.test(code), '模块编码只能使用小写字母、数字和单个连字符，且必须以字母开头')
  const title = validateText(input.title, '模块名称', 80)
  const description = validateText(input.description || `${title}业务模块`, '模块说明', 200)
  const parentMenuId = input.parentMenuId ?? 0
  invariant(Number.isSafeInteger(parentMenuId) && parentMenuId >= 0, '父菜单 ID 必须是非负整数')

  const names = namesFor(code)
  const moduleRoot = path.join(root, 'nz-server', 'nz-module', names.artifactId)
  if (await exists(moduleRoot)) throw new CliError(`目标模块目录已存在：${relativeDisplay(root, moduleRoot)}`)
  if (await exists(path.join(root, 'nz-web', 'src', 'modules', code))) {
    throw new CliError(`前端模块清单已存在：${code}`)
  }

  const migrations = await inspectMigrations(root)
  if (migrations.errors.length > 0) throw new CliError(`迁移基线不合法：\n${migrations.errors.join('\n')}`)
  const version = migrations.nextVersion
  const menuId = input.menuId ?? (await nextMenuId(migrations.migrationDirectory))
  invariant(Number.isSafeInteger(menuId) && menuId > 0, '菜单 ID 必须是正整数')

  const javaMain = path.join(
    moduleRoot,
    'src',
    'main',
    'java',
    'com',
    'nz',
    'admin',
    'modules',
    names.packageSegment,
  )
  const javaTest = path.join(
    moduleRoot,
    'src',
    'test',
    'java',
    'com',
    'nz',
    'admin',
    'modules',
    names.packageSegment,
  )
  const migrationName = `V${version}__${code.replaceAll('-', '_')}_module.sql`
  const upgradeName = `upgrade-p${version}-${code}-module.sql`
  const migrationContent = menuMigration({ code, title, parentMenuId, menuId })

  const modulePomPath = path.join(root, 'nz-server', 'nz-module', 'pom.xml')
  const rootPomPath = path.join(root, 'nz-server', 'pom.xml')
  const appPomPath = path.join(root, 'nz-server', 'nz-app', 'pom.xml')
  const applicationPath = path.join(root, 'nz-server', 'nz-app', 'src', 'main', 'resources', 'application.yml')
  const migrationTestPath = path.join(
    root,
    'nz-server',
    'nz-app',
    'src',
    'test',
    'java',
    'com',
    'nz',
    'admin',
    'migration',
    'FlywayMigrationResourcesTest.java',
  )

  const changes = new Map()
  changes.set(path.join(moduleRoot, 'pom.xml'), backendPom({ ...names, title }))
  changes.set(
    path.join(javaMain, 'config', `Nz${names.pascal}ModuleAutoConfiguration.java`),
    autoConfiguration(names),
  )
  changes.set(path.join(javaMain, 'controller', 'ModuleInfoController.java'), infoController({ ...names, title }))
  changes.set(path.join(javaMain, 'package-info.java'), packageInfo({ ...names, title }))
  changes.set(
    path.join(moduleRoot, 'src', 'main', 'resources', 'META-INF', 'nz', 'module.yaml'),
    moduleManifest({ code, title, description }),
  )
  changes.set(
    path.join(
      moduleRoot,
      'src',
      'main',
      'resources',
      'META-INF',
      'spring',
      'org.springframework.boot.autoconfigure.AutoConfiguration.imports',
    ),
    autoConfigurationImport(names),
  )
  changes.set(
    path.join(javaTest, `Nz${names.pascal}ModuleManifestTest.java`),
    manifestTest(names),
  )
  changes.set(path.join(root, 'nz-web', 'src', 'modules', code, 'manifest.ts'), frontendManifest({ code, title }))
  changes.set(path.join(root, 'nz-web', 'src', 'api', code, 'index.ts'), frontendApi({ code }))
  changes.set(path.join(root, 'nz-web', 'src', 'views', code, 'index.vue'), frontendPage({ code, title }))
  changes.set(
    path.join(root, 'nz-web', 'tests', 'unit', 'views', code, 'manifest.test.ts'),
    frontendManifestTest({ code }),
  )
  changes.set(path.join(migrations.migrationDirectory, migrationName), migrationContent)
  changes.set(path.join(migrations.dbDirectory, upgradeName), migrationContent)

  changes.set(modulePomPath, insertModule(await readUtf8(modulePomPath), names.artifactId))
  changes.set(
    rootPomPath,
    insertDependency(await readUtf8(rootPomPath), names.artifactId, dependencyBlock(names.artifactId)),
  )
  changes.set(
    appPomPath,
    insertDependency(await readUtf8(appPomPath), names.artifactId, appDependencyBlock(names.artifactId)),
  )
  changes.set(applicationPath, setModuleEnabled(await readUtf8(applicationPath), code, true))
  changes.set(
    migrationTestPath,
    appendMigrationResource(await readUtf8(migrationTestPath), `db/migration/${migrationName}`),
  )

  return { code, title, description, version, menuId, migrationName, upgradeName, changes }
}

export async function addModule(root, input, dryRun = false) {
  const plan = await planModuleAdd(root, input)
  if (dryRun) return { ...plan, backup: undefined }
  const backup = await applyChanges(root, `module-add-${plan.code}`, plan.changes)
  return { ...plan, backup }
}
