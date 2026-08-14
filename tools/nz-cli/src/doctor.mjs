import { access, readdir } from 'node:fs/promises'
import path from 'node:path'
import { spawnSync } from 'node:child_process'
import { inspectMigrations } from './migrations.mjs'
import { readUtf8 } from './project.mjs'

function probe(command, args = []) {
  const executable = process.platform === 'win32' && command === 'pnpm' ? 'pnpm.cmd' : command
  const result = spawnSync(executable, args, {
    encoding: 'utf8',
    shell: process.platform === 'win32',
    windowsHide: true,
  })
  return {
    ok: !result.error && result.status === 0,
    output: `${result.stdout || ''}${result.stderr || ''}`.trim().split(/\r?\n/)[0] || result.error?.message || '',
  }
}

function majorVersion(value) {
  const match = /v?(\d+)(?:\.(\d+))?/.exec(value)
  return match ? [Number(match[1]), Number(match[2] || 0)] : [0, 0]
}

export async function doctor(root, probeImpl = probe) {
  const checks = []
  const node = probeImpl('node', ['--version'])
  const [nodeMajor, nodeMinor] = majorVersion(node.output)
  checks.push({
    name: 'Node.js',
    ok: node.ok && (nodeMajor > 22 || (nodeMajor === 22 && nodeMinor >= 13)),
    detail: node.output || '未安装',
  })
  const java = probeImpl('java', ['-version'])
  checks.push({ name: 'Java', ok: java.ok && majorVersion(java.output)[0] >= 17, detail: java.output || '未安装' })
  const pnpm = probeImpl('pnpm', ['--version'])
  checks.push({ name: 'pnpm', ok: pnpm.ok, detail: pnpm.output || '未安装' })

  const wrapper = path.join(root, 'nz-server', process.platform === 'win32' ? 'mvnw.cmd' : 'mvnw')
  try {
    await access(wrapper)
    checks.push({ name: 'Maven Wrapper', ok: true, detail: path.relative(root, wrapper) })
  } catch {
    checks.push({ name: 'Maven Wrapper', ok: false, detail: '文件不存在' })
  }

  const migrations = await inspectMigrations(root)
  checks.push({
    name: '数据库迁移',
    ok: migrations.errors.length === 0,
    detail: migrations.errors.length === 0 ? `V1-V${migrations.nextVersion - 1} 连续` : migrations.errors.join('；'),
  })

  const moduleDirectory = path.join(root, 'nz-server', 'nz-module')
  const moduleNames = (await readdir(moduleDirectory, { withFileTypes: true }))
    .filter((entry) => entry.isDirectory() && entry.name.startsWith('nz-'))
    .map((entry) => entry.name)
    .sort()
  const aggregator = await readUtf8(path.join(moduleDirectory, 'pom.xml'))
  const rootPom = await readUtf8(path.join(root, 'nz-server', 'pom.xml'))
  const appPom = await readUtf8(path.join(root, 'nz-server', 'nz-app', 'pom.xml'))
  const inconsistent = moduleNames.filter(
    (name) =>
      !aggregator.includes(`<module>${name}</module>`) ||
      !rootPom.includes(`<artifactId>${name}</artifactId>`) ||
      !appPom.includes(`<artifactId>${name}</artifactId>`),
  )
  checks.push({
    name: '模块装配',
    ok: inconsistent.length === 0,
    detail: inconsistent.length === 0 ? `${moduleNames.length} 个业务模块已接入` : `缺少 POM 接入：${inconsistent.join(', ')}`,
  })

  return { checks, ok: checks.every((check) => check.ok) }
}
