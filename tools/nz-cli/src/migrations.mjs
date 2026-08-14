import { readdir } from 'node:fs/promises'
import path from 'node:path'
import { CliError } from './errors.mjs'

const MIGRATION_PATTERN = /^V(\d+)__([a-z0-9_]+)\.sql$/
const UPGRADE_PATTERN = /^upgrade-p(\d+)-([a-z0-9-]+)\.sql$/

export async function inspectMigrations(root) {
  const dbDirectory = path.join(root, 'nz-server', 'nz-app', 'src', 'main', 'resources', 'db')
  const migrationDirectory = path.join(dbDirectory, 'migration')
  const migrationFiles = (await readdir(migrationDirectory)).filter((name) => name.endsWith('.sql'))
  const upgrades = (await readdir(dbDirectory)).filter((name) => name.startsWith('upgrade-') && name.endsWith('.sql'))
  const errors = []
  const migrations = []

  for (const name of migrationFiles) {
    const match = MIGRATION_PATTERN.exec(name)
    if (!match) {
      errors.push(`迁移文件命名不合法：${name}`)
      continue
    }
    migrations.push({ version: Number(match[1]), name, description: match[2] })
  }
  migrations.sort((left, right) => left.version - right.version)

  const seen = new Set()
  for (const migration of migrations) {
    if (seen.has(migration.version)) errors.push(`迁移版本重复：V${migration.version}`)
    seen.add(migration.version)
  }
  if (migrations.length === 0 || migrations[0].version !== 1) errors.push('迁移必须从 V1 开始')
  for (let index = 1; index < migrations.length; index += 1) {
    if (migrations[index].version !== migrations[index - 1].version + 1) {
      errors.push(`迁移版本不连续：V${migrations[index - 1].version} 后是 V${migrations[index].version}`)
    }
  }

  const upgradeVersions = new Map()
  for (const name of upgrades) {
    const match = UPGRADE_PATTERN.exec(name)
    if (!match) {
      errors.push(`升级脚本命名不合法：${name}`)
      continue
    }
    const version = Number(match[1])
    if (upgradeVersions.has(version)) errors.push(`P${version} 存在多个手工升级脚本`)
    upgradeVersions.set(version, name)
  }
  for (const migration of migrations.filter((item) => item.version >= 2)) {
    if (!upgradeVersions.has(migration.version)) errors.push(`V${migration.version} 缺少 upgrade-p${migration.version}-*.sql`)
  }
  for (const version of upgradeVersions.keys()) {
    if (!seen.has(version)) errors.push(`upgrade-p${version} 没有对应 Flyway 迁移`)
  }

  return {
    dbDirectory,
    migrationDirectory,
    migrations,
    upgrades: [...upgradeVersions.entries()].map(([version, name]) => ({ version, name })),
    errors,
    nextVersion: migrations.length === 0 ? 1 : migrations.at(-1).version + 1,
  }
}

export async function checkMigrations(root) {
  const result = await inspectMigrations(root)
  if (result.errors.length > 0) throw new CliError(result.errors.join('\n'), 1)
  return result
}
