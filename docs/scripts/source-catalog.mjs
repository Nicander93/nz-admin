import { createHash } from 'node:crypto'
import {
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  realpathSync,
  lstatSync,
  writeFileSync,
} from 'node:fs'
import { dirname, relative, resolve, sep } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { XMLParser, XMLValidator } from 'fast-xml-parser'
import { parse as parseYaml } from 'yaml'
import * as reading from './catalog.config.mjs'

const repository = 'https://github.com/Nicander93/nz-admin'
const autoImports =
  'src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports'
const manifestFile = 'src/main/resources/META-INF/nz/module.yaml'
const parser = new XMLParser({ parseTagValue: false, processEntities: false })
const array = (value) =>
  value == null || value === '' ? [] : Array.isArray(value) ? value : [value]
const ordered = (values) => [...values].sort((a, b) => a.localeCompare(b, 'en'))
const unique = (values) => ordered(new Set(values))
const slash = (value) => value.split(sep).join('/')

function invariant(condition, message) {
  if (!condition) throw new Error(message)
}

function walk(directory, root) {
  if (!existsSync(directory)) return []
  const actual = realpathSync(directory)
  invariant(actual.startsWith(`${root}${sep}`), `源码目录越界：${directory}`)
  invariant(
    !lstatSync(directory).isSymbolicLink(),
    `源码目录不接受链接：${directory}`,
  )
  return readdirSync(directory, { withFileTypes: true })
    .flatMap((item) => {
      const path = resolve(directory, item.name)
      if (item.isSymbolicLink()) return []
      return item.isDirectory() ? walk(path, root) : [path]
    })
    .sort()
}

function layerFor(path, packaging) {
  if (packaging === 'pom') return 'aggregation'
  if (path.startsWith('nz-server/nz-common/')) return 'common'
  if (path.startsWith('nz-server/nz-framework/')) return 'framework'
  if (path.startsWith('nz-server/nz-module/')) return 'business'
  if (path === 'nz-server/nz-app') return 'application'
  throw new Error(`无法识别模块层级：${path}`)
}

export function collectCatalog(root, config = reading) {
  root = realpathSync(root)
  const inputs = new Map()
  const read = (path) => {
    const absolute = resolve(root, path)
    invariant(existsSync(absolute), `源码文件缺失：${path}`)
    const actual = realpathSync(absolute)
    invariant(actual.startsWith(`${root}${sep}`), `源码路径越过仓库：${path}`)
    const text = readFileSync(actual, 'utf8')
    inputs.set(
      slash(relative(root, actual)),
      createHash('sha256').update(text.replace(/\r\n/g, '\n')).digest('hex'),
    )
    return text
  }
  const guide = (link) => {
    invariant(
      typeof link === 'string' &&
        /^\/[A-Za-z0-9_/-]+$/.test(link) &&
        !link.includes('..'),
      `指南路径非法：${link}`,
    )
    const path = `docs${link.endsWith('/') ? `${link}index` : link}.md`
    invariant(existsSync(resolve(root, path)), `指南缺页：${link}`)
    invariant(
      realpathSync(resolve(root, path)).startsWith(`${root}${sep}`),
      `指南路径越界：${link}`,
    )
    return link
  }
  const modules = []
  const visit = (pomPath) => {
    const xml = read(pomPath)
    invariant(XMLValidator.validate(xml) === true, `POM XML 无效：${pomPath}`)
    const project = parser.parse(xml).project
    invariant(project?.artifactId, `POM 缺少 artifactId：${pomPath}`)
    invariant(
      !modules.some((item) => item.id === project.artifactId),
      `重复 artifactId：${project.artifactId}`,
    )
    const path = slash(dirname(pomPath))
    const entry = config.moduleGuides[project.artifactId]
    invariant(
      Array.isArray(entry) && entry.length === 2,
      `模块缺少阅读映射：${project.artifactId}`,
    )
    const description = String(project.description ?? '').trim()
    invariant(description.length > 0, `POM 缺少模块说明：${pomPath}`)
    const declared = array(project.dependencies?.dependency).map((item) => ({
      id: item.artifactId,
      group: item.groupId,
      scope: item.scope || 'compile',
    }))
    const module = {
      id: project.artifactId,
      title: entry[0],
      description,
      path,
      pom: pomPath,
      layer: layerFor(path, project.packaging),
      guide: guide(entry[1]),
      declared,
      dependencies: [],
      testDependencies: [],
      providedDependencies: [],
      dependents: [],
      autoConfigurations: [],
      contracts: [],
      tests: [],
      manifest: null,
      frontend: null,
    }
    modules.push(module)
    for (const child of array(project.modules?.module)) {
      const next = slash(relative(root, resolve(root, path, child, 'pom.xml')))
      invariant(!next.startsWith('../'), `reactor 路径越界：${child}`)
      visit(next)
    }
  }
  visit('nz-server/pom.xml')
  const byId = new Map(modules.map((item) => [item.id, item]))
  for (const id of Object.keys(config.moduleGuides))
    invariant(byId.has(id), `过期的模块阅读映射：${id}`)
  for (const module of modules) {
    for (const dep of module.declared.filter(
      (item) => item.group === 'com.nz',
    )) {
      invariant(
        byId.has(dep.id),
        `${module.id} 引用了 reactor 中不存在的模块 ${dep.id}`,
      )
      if (dep.scope === 'test') module.testDependencies.push(dep.id)
      else if (['provided', 'system'].includes(dep.scope))
        module.providedDependencies.push(dep.id)
      else {
        invariant(
          ['compile', 'runtime'].includes(dep.scope),
          `内部依赖 scope 不受支持：${module.id} -> ${dep.id}: ${dep.scope}`,
        )
        module.dependencies.push(dep.id)
      }
    }
    delete module.declared
    module.dependencies = unique(module.dependencies)
    module.testDependencies = unique(module.testDependencies)
    module.providedDependencies = unique(module.providedDependencies)
    module.autoConfigurations = existsSync(
      resolve(root, module.path, autoImports),
    )
      ? read(`${module.path}/${autoImports}`)
          .split(/\r?\n/)
          .map((line) => line.trim())
          .filter((line) => line && !line.startsWith('#'))
          .map((name) => {
            const source = `${module.path}/src/main/java/${name.replaceAll('.', '/')}.java`
            read(source)
            return { name, source }
          })
      : []
    module.tests = walk(resolve(root, module.path, 'src/test/java'), root)
      .filter((path) => path.endsWith('Test.java'))
      .map((path) => slash(relative(root, path)))
    if (module.layer !== 'aggregation') {
      for (const path of walk(
        resolve(root, module.path, 'src/main/java'),
        root,
      ).filter((path) => path.endsWith('.java'))) {
        const name = path.split(sep).at(-1).slice(0, -5)
        const text = readFileSync(path, 'utf8')
        if (
          new RegExp(
            `\\bpublic\\s+(?:sealed\\s+)?interface\\s+${name}\\b`,
          ).test(text)
        ) {
          const source = slash(relative(root, path))
          read(source)
          module.contracts.push({ name, source })
        }
      }
    }
    if (module.layer === 'business') {
      const source = `${module.path}/${manifestFile}`
      const manifestText = read(source)
      const raw = parseYaml(manifestText)
      invariant(
        raw &&
          typeof raw === 'object' &&
          typeof raw.code === 'string' &&
          /^[a-z][a-z0-9-]*$/.test(raw.code),
        `模块清单 code 非法：${source}`,
      )
      const list = (name) => {
        const value = raw[name] ?? []
        invariant(
          Array.isArray(value) &&
            value.every(
              (item) =>
                typeof item === 'string' && /^[a-z][a-z0-9-]*$/.test(item),
            ),
          `清单 ${name} 非法：${source}`,
        )
        return unique(value)
      }
      invariant(
        raw.defaultEnabled == null || typeof raw.defaultEnabled === 'boolean',
        `defaultEnabled 必须为布尔：${source}`,
      )
      // 与 Java 注册器保持一致：其有意只支持简单标量和块列表，不接受 YAML 的全部语法。
      const runtimeValues = {},
        runtimeLists = {}
      let listKey = null
      for (const line of manifestText.split(/\r?\n/)) {
        const text = line.trim()
        if (!text || text.startsWith('#')) continue
        if (text.startsWith('-')) {
          invariant(listKey, `清单列表格式不兼容运行注册器：${source}`)
          ;(runtimeLists[listKey] ??= []).push(text.slice(1).trim())
          continue
        }
        const colon = text.indexOf(':')
        invariant(colon > 0, `清单格式不兼容运行注册器：${source}`)
        const key = text.slice(0, colon).trim(),
          value = text.slice(colon + 1).trim()
        listKey = value ? null : key
        if (value) runtimeValues[key] = value
      }
      for (const key of [
        'code',
        'name',
        'version',
        'description',
        'frontendModule',
        'defaultEnabled',
      ]) {
        if (raw[key] != null)
          invariant(
            runtimeValues[key] === String(raw[key]),
            `清单 ${key} 请使用运行注册器支持的简单标量：${source}`,
          )
      }
      for (const key of ['requiredModules', 'requiredStarters'])
        invariant(
          JSON.stringify(unique(runtimeLists[key] ?? [])) ===
            JSON.stringify(list(key)),
          `清单 ${key} 请使用块列表，不能使用非空 inline sequence：${source}`,
        )

      module.manifest = {
        source,
        code: raw.code,
        name: String(raw.name ?? raw.code),
        requiredModules: list('requiredModules'),
        requiredStarters: list('requiredStarters'),
        defaultEnabled: raw.defaultEnabled ?? true,
        frontendModule: String(raw.frontendModule ?? raw.code),
      }
      const frontendSource = `nz-web/src/modules/${module.manifest.frontendModule}/manifest.ts`
      const text = read(frontendSource)
      const literal = (field) =>
        text.match(new RegExp(`\\b${field}\\s*:\\s*(['\"])([^'\"]+)\\1`))?.[2]
      const code = literal('code'),
        componentPrefix = literal('componentPrefix')
      invariant(
        code === module.manifest.frontendModule && componentPrefix,
        `前端清单与后端声明不一致：${frontendSource}`,
      )
      module.frontend = {
        source: frontendSource,
        code,
        componentPrefix,
        title: literal('title') ?? code,
      }
    }
  }
  const businessByCode = new Map()
  const prefixes = new Set()
  for (const module of modules.filter((item) => item.manifest)) {
    invariant(
      !businessByCode.has(module.manifest.code),
      `重复业务模块 code：${module.manifest.code}`,
    )
    invariant(
      !prefixes.has(module.frontend.componentPrefix),
      `重复前端组件前缀：${module.frontend.componentPrefix}`,
    )
    businessByCode.set(module.manifest.code, module)
    prefixes.add(module.frontend.componentPrefix)
  }
  // 这里检查 POM 显式生产依赖；不把它冒充 Maven profile 的完整解析结果或运行健康。
  const reachable = (id) => {
    const seen = new Set()
    const follow = (current) => {
      for (const next of byId.get(current).dependencies)
        if (!seen.has(next)) {
          seen.add(next)
          follow(next)
        }
    }
    follow(id)
    return seen
  }
  const visiting = new Set(),
    visited = new Set()
  const checkCycles = (id) => {
    invariant(
      !visiting.has(id),
      `生产依赖存在环：${[...visiting, id].join(' -> ')}`,
    )
    if (visited.has(id)) return
    visiting.add(id)
    for (const dep of byId.get(id).dependencies) checkCycles(dep)
    visiting.delete(id)
    visited.add(id)
  }
  for (const module of modules) checkCycles(module.id)
  for (const module of modules) {
    for (const depId of [
      ...module.dependencies,
      ...module.providedDependencies,
    ]) {
      const dep = byId.get(depId)
      invariant(
        !(
          module.layer === 'common' &&
          ['framework', 'business', 'application'].includes(dep.layer)
        ),
        `公共协议反向依赖：${module.id} -> ${dep.id}`,
      )
      invariant(
        !(
          module.layer === 'framework' &&
          ['business', 'application'].includes(dep.layer)
        ),
        `框架反向依赖业务：${module.id} -> ${dep.id}`,
      )
      invariant(
        !(module.layer === 'business' && dep.layer === 'application'),
        `业务依赖启动层：${module.id} -> ${dep.id}`,
      )
      if (module.layer === 'business' && dep.layer === 'business')
        invariant(
          module.manifest.requiredModules.includes(dep.manifest.code),
          `跨业务依赖缺少 requiredModules：${module.id} -> ${dep.id}`,
        )
      if (module.dependencies.includes(depId)) dep.dependents.push(module.id)
    }
    if (module.manifest) {
      const all = reachable(module.id)
      for (const code of module.manifest.requiredModules) {
        const dep = businessByCode.get(code)
        invariant(
          dep && all.has(dep.id),
          `${module.id} 声明 requiredModules ${code} 但缺少对应生产依赖`,
        )
      }
      for (const starter of module.manifest.requiredStarters)
        invariant(
          all.has(`nz-starter-${starter}`),
          `${module.id} 声明 requiredStarters ${starter} 但生产依赖不可达`,
        )
    }
  }
  const contracts = modules.flatMap((module) =>
    module.contracts.map((item) => ({ ...item, module: module.id })),
  )
  const extensions = config.extensionSpecs
    .map(([name, purpose, link]) => {
      const found = contracts.filter((item) => item.name === name)
      invariant(found.length === 1, `扩展接口缺失或不唯一：${name}`)
      return { ...found[0], purpose, guide: guide(link) }
    })
    .sort((a, b) => a.name.localeCompare(b.name, 'en'))
  const capabilities = config.capabilitySpecs.map((item) => {
    invariant(
      ['available', 'conditional', 'planned'].includes(item.status),
      `能力状态非法：${item.id}`,
    )
    invariant(
      item.status === 'planned' || item.sources.length > 0,
      `已实现能力缺少源码：${item.id}`,
    )
    for (const id of item.modules)
      invariant(byId.has(id), `能力引用未知模块：${item.id} -> ${id}`)
    for (const source of item.sources) read(source)
    return { ...item, guide: guide(item.guide) }
  })
  invariant(
    new Set(capabilities.map((item) => item.id)).size === capabilities.length,
    '能力 id 重复',
  )
  return {
    schemaVersion: 1,
    repository,
    modules: modules
      .sort((a, b) => a.id.localeCompare(b.id, 'en'))
      .map((item) => ({ ...item, dependents: unique(item.dependents) })),
    extensions,
    capabilities,
    sources: [...inputs]
      .sort(([a], [b]) => a.localeCompare(b, 'en'))
      .map(([path, sha256]) => ({ path, sha256 })),
  }
}

export function renderSourceIndex(catalog) {
  const source = (path) => `[${path}](${repository}/blob/master/${path})`
  const labels = {
    aggregation: '聚合工程',
    application: '应用装配',
    common: '公共对象与协议',
    framework: '框架 Starter',
    business: '业务模块',
  }
  const lines = [
    '<!-- 由 pnpm catalog:update 生成；编辑 scripts/catalog.config.mjs 或对应源码。 -->',
    '# 源码与模块索引',
    '',
    '本页从仓库 POM、清单、自动配置和公开接口生成，可被站内搜索检索。它记录源码声明，不代表模块已在某个进程启用，也不替代行为测试。',
    '',
    '使用[开发者能力目录](/developer/catalog)按关键词、架构层和扩展点筛选。维护方法见[源码与文档同步](/developer/documentation-contract)。',
    '',
  ]
  for (const [layer, title] of Object.entries(labels)) {
    lines.push(`## ${title}`, '')
    for (const module of catalog.modules.filter(
      (item) => item.layer === layer,
    )) {
      lines.push(
        `### ${module.id}`,
        '',
        `${module.title}。${module.description}`,
        '',
        `- 指南：[${module.title}](${module.guide})`,
        `- POM：${source(module.pom)}`,
        `- 显式生产依赖：${module.dependencies.map((id) => '`' + id + '`').join('、') || '没有仓库内部生产依赖'}`,
      )
      if (module.providedDependencies.length)
        lines.push(
          `- 非运行期声明（provided/system）：${module.providedDependencies.join('、')}`,
        )
      if (module.manifest)
        lines.push(
          `- 业务清单：${source(module.manifest.source)}；默认${module.manifest.defaultEnabled ? '启用' : '关闭'}。`,
          `- 前端清单：${source(module.frontend.source)}；组件前缀 \`${module.frontend.componentPrefix}\`。`,
        )
      if (module.autoConfigurations.length)
        lines.push(
          `- 自动配置：${module.autoConfigurations.map((item) => `[${item.name.split('.').at(-1)}](${repository}/blob/master/${item.source})`).join('、')}`,
        )
      if (module.contracts.length)
        lines.push(
          `- 公开接口：${module.contracts.map((item) => `[${item.name}](${repository}/blob/master/${item.source})`).join('、')}`,
        )
      if (module.tests.length)
        lines.push(
          `- 测试入口：${source(module.tests[0])}（共 ${module.tests.length} 个测试源文件，不代表本次执行结果）。`,
        )
      lines.push('')
    }
  }
  lines.push('## 扩展协议', '')
  for (const item of catalog.extensions)
    lines.push(
      `### ${item.name}`,
      '',
      item.purpose,
      '',
      `接口：${source(item.source)}。接入指南：[阅读](${item.guide})。`,
      '',
    )
  return lines.join('\n')
}

export function writeCatalog(root, check = false, config = reading) {
  const catalog = collectCatalog(root, config)
  const outputs = [
    ['docs/data/source-catalog.json', `${JSON.stringify(catalog, null, 2)}\n`],
    ['docs/reference/source-index.md', renderSourceIndex(catalog)],
  ]
  for (const [path, expected] of outputs) {
    const file = resolve(root, path)
    if (check)
      invariant(
        existsSync(file) &&
          readFileSync(file, 'utf8').replace(/\r\n/g, '\n') === expected,
        `源码目录已过期：${path}；请在 docs 执行 pnpm catalog:update 并检查差异`,
      )
    else {
      mkdirSync(dirname(file), { recursive: true })
      writeFileSync(file, expected)
    }
  }
  return catalog
}

const root = resolve(fileURLToPath(new URL('../../', import.meta.url)))
if (
  process.argv[1] &&
  pathToFileURL(resolve(process.argv[1])).href === import.meta.url
) {
  try {
    invariant(
      process.argv.slice(2).every((arg) => arg === '--check'),
      '只支持 --check，默认更新目录',
    )
    const catalog = writeCatalog(root, process.argv.includes('--check'))
    console.log(
      `源码目录${process.argv.includes('--check') ? '校验' : '更新'}通过：${catalog.modules.length} 个 Maven 模块，${catalog.extensions.length} 个扩展协议`,
    )
  } catch (error) {
    console.error(error.message)
    process.exitCode = 1
  }
}
