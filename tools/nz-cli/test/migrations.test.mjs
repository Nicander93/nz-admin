import assert from 'node:assert/strict'
import { cp, rm, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { describe, it } from 'node:test'
import { CliError } from '../src/errors.mjs'
import { checkMigrations, inspectMigrations } from '../src/migrations.mjs'
import { createProjectFixture } from './helpers.mjs'

describe('migration checks', () => {
  it('accepts contiguous Flyway and upgrade scripts', async () => {
    const root = await createProjectFixture()
    try {
      const result = await checkMigrations(root)
      assert.equal(result.nextVersion, result.migrations.length + 1)
      assert.equal(result.upgrades.length, result.migrations.length - 1)
    } finally {
      await rm(root, { recursive: true, force: true })
    }
  })

  it('reports gaps, duplicates and missing upgrade scripts', async () => {
    const root = await createProjectFixture()
    try {
      const orphanVersion = (await checkMigrations(root)).nextVersion
      const directory = path.join(root, 'nz-server/nz-app/src/main/resources/db')
      await rm(path.join(directory, 'migration/V4__job_module.sql'))
      await cp(
        path.join(directory, 'migration/V3__client_module.sql'),
        path.join(directory, 'migration/V03__duplicate.sql'),
      )
      await writeFile(path.join(directory, `migration/V${orphanVersion}__orphan.sql`), '-- orphan\n')
      const result = await inspectMigrations(root)
      assert(result.errors.some((message) => message.includes('版本重复')))
      assert(result.errors.some((message) => message.includes('不连续')))
      assert(result.errors.some((message) => message.includes(`V${orphanVersion} 缺少`)))
      await assert.rejects(() => checkMigrations(root), CliError)
    } finally {
      await rm(root, { recursive: true, force: true })
    }
  })
})
