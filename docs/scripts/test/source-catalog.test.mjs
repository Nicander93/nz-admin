import assert from 'node:assert/strict'
import {
  mkdtempSync,
  mkdirSync,
  readFileSync,
  rmSync,
  writeFileSync,
  symlinkSync,
} from 'node:fs'
import { basename, dirname, join, resolve } from 'node:path'
import { tmpdir } from 'node:os'
import test from 'node:test'
import {
  collectCatalog,
  renderSourceIndex,
  writeCatalog,
} from '../source-catalog.mjs'

function fixture(t) {
  const base = resolve(tmpdir())
  const root = mkdtempSync(join(base, 'nz-catalog-'))
  t.after(() => {
    assert.equal(dirname(root), base)
    assert.match(basename(root), /^nz-catalog-/)
    rmSync(root, { recursive: true, force: true })
  })
  const write = (file, value) => {
    const path = join(root, file)
    mkdirSync(dirname(path), { recursive: true })
    writeFileSync(path, value)
  }
  const mutate = (file, fn) =>
    write(file, fn(readFileSync(join(root, file), 'utf8')))
  const paths = {
    'nz-server': 'nz-server',
    'nz-common-core': 'nz-server/nz-common/nz-common-core',
    'nz-starter-alpha': 'nz-server/nz-framework/nz-starter-alpha',
    'nz-demo': 'nz-server/nz-module/nz-demo',
    'nz-app': 'nz-server/nz-app',
  }
  const dep = (id) =>
    `<dependency><groupId>com.nz</groupId><artifactId>${id}</artifactId></dependency>`
  for (const [id, path] of Object.entries(paths)) {
    const children =
      id === 'nz-server'
        ? '<packaging>pom</packaging><modules>' +
          Object.values(paths)
            .filter((value) => value !== 'nz-server')
            .map((value) => `<module>${value.slice(10)}</module>`)
            .join('') +
          '</modules>'
        : ''
    const dependencies =
      id === 'nz-demo'
        ? ['nz-common-core', 'nz-starter-alpha']
        : id === 'nz-app'
          ? ['nz-demo']
          : []
    write(
      `${path}/pom.xml`,
      `<project><modelVersion>4.0.0</modelVersion><groupId>com.nz</groupId><artifactId>${id}</artifactId><description>${id} 说明</description>${children}<dependencies>${dependencies.map(dep).join('')}</dependencies></project>`,
    )
  }
  write('docs/guide.md', '# 指南\n')
  const manifest = `${paths['nz-demo']}/src/main/resources/META-INF/nz/module.yaml`
  write(
    manifest,
    'code: demo\nname: Demo\nrequiredModules: []\nrequiredStarters:\n  - alpha\ndefaultEnabled: true\nfrontendModule: demo\n',
  )
  const frontend = 'nz-web/src/modules/demo/manifest.ts'
  write(
    frontend,
    "export default { code: 'demo', componentPrefix: 'demo', title: 'Demo' }",
  )
  const contract = `${paths['nz-starter-alpha']}/src/main/java/example/AlphaPort.java`
  write(contract, 'package example; public interface AlphaPort {}')
  write(
    `${paths['nz-starter-alpha']}/src/main/java/example/AlphaAutoConfiguration.java`,
    'package example; public class AlphaAutoConfiguration {}',
  )
  write(
    `${paths['nz-starter-alpha']}/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`,
    'example.AlphaAutoConfiguration\n',
  )
  const config = {
    moduleGuides: Object.fromEntries(
      Object.keys(paths).map((id) => [id, [id, '/guide']]),
    ),
    extensionSpecs: [['AlphaPort', '示例协议', '/guide']],
    capabilitySpecs: [
      {
        id: 'alpha',
        title: 'Alpha',
        status: 'available',
        summary: '可用',
        guide: '/guide',
        modules: ['nz-starter-alpha'],
        sources: [contract],
      },
    ],
  }
  return {
    root,
    paths,
    manifest,
    frontend,
    contract,
    config,
    write,
    mutate,
    dep,
  }
}

test('提取实际依赖、自动配置、清单和接口，且输出确定', (t) => {
  const f = fixture(t)
  const a = collectCatalog(f.root, f.config)
  assert.deepEqual(a, collectCatalog(f.root, f.config))
  assert.equal(a.modules.length, 5)
  const demo = a.modules.find((item) => item.id === 'nz-demo')
  assert.deepEqual(demo.dependencies, ['nz-common-core', 'nz-starter-alpha'])
  assert.equal(demo.manifest.defaultEnabled, true)
  assert.equal(demo.frontend.componentPrefix, 'demo')
  const starter = a.modules.find((item) => item.id === 'nz-starter-alpha')
  assert.equal(
    starter.autoConfigurations[0].name,
    'example.AlphaAutoConfiguration',
  )
  assert.equal(a.extensions[0].source, f.contract)
  assert.match(renderSourceIndex(a), /AlphaPort/)
  assert.match(renderSourceIndex(a), /不代表模块已/)
})

test('必需 Starter 在业务自身依赖中不可达时拒绝', (t) => {
  const f = fixture(t)
  f.mutate(`${f.paths['nz-demo']}/pom.xml`, (text) =>
    text.replace(f.dep('nz-starter-alpha'), ''),
  )
  assert.throws(
    () => collectCatalog(f.root, f.config),
    /requiredStarters alpha.*不可达/,
  )
})

test('框架反向依赖业务时拒绝', (t) => {
  const f = fixture(t)
  f.mutate(`${f.paths['nz-demo']}/pom.xml`, (text) =>
    text.replace(f.dep('nz-starter-alpha'), ''),
  )
  f.mutate(f.manifest, (text) =>
    text.replace('requiredStarters:\n  - alpha', 'requiredStarters: []'),
  )
  f.mutate(`${f.paths['nz-starter-alpha']}/pom.xml`, (text) =>
    text.replace('<dependencies>', `<dependencies>${f.dep('nz-demo')}`),
  )
  assert.throws(() => collectCatalog(f.root, f.config), /框架反向依赖业务/)
})

test('公共协议反向依赖 Starter 时拒绝', (t) => {
  const f = fixture(t)
  f.mutate(`${f.paths['nz-common-core']}/pom.xml`, (text) =>
    text.replace(
      '<dependencies>',
      `<dependencies>${f.dep('nz-starter-alpha')}`,
    ),
  )
  assert.throws(() => collectCatalog(f.root, f.config), /公共协议反向依赖/)
})

test('生产依赖环不会导致无限遍历', (t) => {
  const f = fixture(t)
  f.mutate(`${f.paths['nz-starter-alpha']}/pom.xml`, (text) =>
    text.replace('<dependencies>', `<dependencies>${f.dep('nz-demo')}`),
  )
  assert.throws(() => collectCatalog(f.root, f.config), /生产依赖存在环/)
})

test('跨业务依赖必须同步清单', (t) => {
  const f = fixture(t)
  f.config.moduleGuides['nz-other'] = ['Other', '/guide']
  f.mutate('nz-server/pom.xml', (text) =>
    text.replace('</modules>', '<module>nz-module/nz-other</module></modules>'),
  )
  const path = 'nz-server/nz-module/nz-other'
  f.write(
    `${path}/pom.xml`,
    '<project><artifactId>nz-other</artifactId><description>Other</description></project>',
  )
  f.write(
    `${path}/src/main/resources/META-INF/nz/module.yaml`,
    'code: other\nfrontendModule: other\n',
  )
  f.write(
    'nz-web/src/modules/other/manifest.ts',
    "export default { code: 'other', componentPrefix: 'other' }",
  )
  f.mutate(`${f.paths['nz-demo']}/pom.xml`, (text) =>
    text.replace('<dependencies>', `<dependencies>${f.dep('nz-other')}`),
  )
  assert.throws(
    () => collectCatalog(f.root, f.config),
    /跨业务依赖缺少 requiredModules/,
  )
})

test('前后端 code 不一致时拒绝', (t) => {
  const f = fixture(t)
  f.mutate(f.frontend, (text) => text.replace("code: 'demo'", "code: 'wrong'"))
  assert.throws(
    () => collectCatalog(f.root, f.config),
    /前端清单与后端声明不一致/,
  )
})

test('不存在的自动配置和指南不能形成虚假索引', (t) => {
  const f = fixture(t)
  f.config.moduleGuides['nz-app'][1] = '/missing'
  assert.throws(() => collectCatalog(f.root, f.config), /指南缺页/)
  f.config.moduleGuides['nz-app'][1] = '/guide'
  f.mutate(
    `${f.paths['nz-starter-alpha']}/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`,
    () => 'example.MissingConfiguration',
  )
  assert.throws(() => collectCatalog(f.root, f.config), /源码文件缺失/)
})

test('源码快照变动会失败，check 不暗中改写生成文件', (t) => {
  const f = fixture(t)
  writeCatalog(f.root, false, f.config)
  writeCatalog(f.root, true, f.config)
  const file = join(f.root, 'docs/data/source-catalog.json')
  const before = readFileSync(file, 'utf8')
  f.mutate(`${f.paths['nz-starter-alpha']}/pom.xml`, (text) =>
    text.replace('说明', '更新说明'),
  )
  assert.throws(() => writeCatalog(f.root, true, f.config), /源码目录已过期/)
  assert.equal(readFileSync(file, 'utf8'), before)
})

test('测试依赖与生产依赖分开，不扩大业务依赖可达性', (t) => {
  const f = fixture(t)
  f.mutate(`${f.paths['nz-demo']}/pom.xml`, (text) =>
    text.replace(
      f.dep('nz-starter-alpha'),
      f
        .dep('nz-starter-alpha')
        .replace('</dependency>', '<scope>test</scope></dependency>'),
    ),
  )
  assert.throws(
    () => collectCatalog(f.root, f.config),
    /requiredStarters alpha.*不可达/,
  )
})

test('非运行期 provided 声明不能满足必需 Starter', (t) => {
  const f = fixture(t)
  f.mutate(`${f.paths['nz-demo']}/pom.xml`, (text) =>
    text.replace(
      f.dep('nz-starter-alpha'),
      f
        .dep('nz-starter-alpha')
        .replace('</dependency>', '<scope>provided</scope></dependency>'),
    ),
  )
  assert.throws(
    () => collectCatalog(f.root, f.config),
    /requiredStarters alpha.*不可达/,
  )
})

test('目录清单拒绝运行注册器不能识别的 inline 非空列表', (t) => {
  const f = fixture(t)
  f.mutate(f.manifest, (text) =>
    text.replace('requiredStarters:\n  - alpha', 'requiredStarters: [alpha]'),
  )
  assert.throws(() => collectCatalog(f.root, f.config), /请使用块列表/)
})

test('目录清单拒绝与运行注册器不同的 quoted code', (t) => {
  const f = fixture(t)
  f.mutate(f.manifest, (text) => text.replace('code: demo', 'code: "demo"'))
  assert.throws(() => collectCatalog(f.root, f.config), /简单标量/)
})

test('源码起始目录为外部符号链接时在扫描前拒绝', (t) => {
  if (process.platform === 'win32')
    return t.skip('Windows 创建符号链接需要额外权限')
  const f = fixture(t)
  const outside = fixture(t)
  const java = join(f.root, f.paths['nz-starter-alpha'], 'src/test/java')
  assert.ok(java.startsWith(f.root + '/'))
  mkdirSync(dirname(java), { recursive: true })
  symlinkSync(
    join(outside.root, outside.paths['nz-starter-alpha'], 'src/main/java'),
    java,
    'dir',
  )
  assert.throws(() => collectCatalog(f.root, f.config), /源码目录越界/)
})
