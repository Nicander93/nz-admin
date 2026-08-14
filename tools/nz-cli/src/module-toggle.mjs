import { access } from 'node:fs/promises'
import path from 'node:path'
import { applyChanges } from './backup.mjs'
import { CliError, invariant } from './errors.mjs'
import { setModuleEnabled } from './module-config.mjs'
import { readUtf8 } from './project.mjs'

export async function toggleModule(root, code, enabled, dryRun = false) {
  invariant(/^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/.test(code), '模块编码格式不正确')
  const manifest = path.join(
    root,
    'nz-server',
    'nz-module',
    `nz-${code}`,
    'src',
    'main',
    'resources',
    'META-INF',
    'nz',
    'module.yaml',
  )
  try {
    await access(manifest)
  } catch {
    throw new CliError(`后端模块不存在：${code}`)
  }

  const application = path.join(root, 'nz-server', 'nz-app', 'src', 'main', 'resources', 'application.yml')
  const updated = setModuleEnabled(await readUtf8(application), code, enabled)
  if (dryRun) return { code, enabled, changed: true }
  const backup = await applyChanges(root, `module-${enabled ? 'enable' : 'disable'}-${code}`, new Map([[application, updated]]))
  return { code, enabled, changed: true, backup }
}
