import assert from 'node:assert/strict'
import { rm } from 'node:fs/promises'
import { describe, it } from 'node:test'
import { runCli } from '../src/cli.mjs'
import { CliError } from '../src/errors.mjs'
import { checkMigrations } from '../src/migrations.mjs'
import { createProjectFixture, sourceRoot } from './helpers.mjs'

describe('CLI integration', () => {
  it('reaches the delivery check command', async () => {
    const output = []
    const code = await runCli(['delivery', 'check', '--root', sourceRoot], {
      write: (line) => output.push(line),
    })
    assert.equal(code, 0)
    assert(output.some((line) => line.includes('6 个服务')))
  })

  it('requires confirmation before changing delivery runtime state', async () => {
    await assert.rejects(
      () => runCli(['delivery', 'up', '--root', sourceRoot]),
      (error) => error instanceof CliError && error.message.includes('--yes'),
    )
  })

  it('runs doctor with injected environment probes', async () => {
    const root = await createProjectFixture()
    try {
      const output = []
      const code = await runCli(['doctor', '--root', root], {
        write: (line) => output.push(line),
        probe: (command) => ({
          ok: true,
          output: command === 'node' ? 'v24.0.0' : command === 'java' ? 'openjdk version "17.0.12"' : '10.0.0',
        }),
      })
      assert.equal(code, 0)
      assert(output.some((line) => line.includes('数据库迁移')))
    } finally {
      await rm(root, { recursive: true, force: true })
    }
  })

  it('requires confirmation for module creation', async () => {
    const root = await createProjectFixture()
    try {
      await assert.rejects(
        () => runCli(['module', 'add', 'audit', '--title', '审计', '--root', root]),
        (error) => error instanceof CliError && error.message.includes('--dry-run'),
      )
    } finally {
      await rm(root, { recursive: true, force: true })
    }
  })

  it('prints the full file plan in module dry-run mode', async () => {
    const root = await createProjectFixture()
    try {
      const { nextVersion } = await checkMigrations(root)
      const output = []
      const code = await runCli(
        ['module', 'add', 'audit', '--title', '审计', '--menu-id', '9200', '--dry-run', '--root', root],
        { write: (line) => output.push(line) },
      )
      assert.equal(code, 0)
      assert(output.some((line) => line.includes(`V${nextVersion}__audit_module.sql`)))
      assert(output.some((line) => line.includes('将修改')))
    } finally {
      await rm(root, { recursive: true, force: true })
    }
  })
})
