import assert from 'node:assert/strict'
import { access, readFile, rm } from 'node:fs/promises'
import path from 'node:path'
import { describe, it } from 'node:test'
import { rollbackBackup } from '../src/backup.mjs'
import { checkMigrations } from '../src/migrations.mjs'
import { addModule } from '../src/module-add.mjs'
import { toggleModule } from '../src/module-toggle.mjs'
import { createProjectFixture } from './helpers.mjs'

describe('module commands', () => {
  it('plans a complete module without writing in dry-run mode', async () => {
    const root = await createProjectFixture()
    try {
      const expectedVersion = (await checkMigrations(root)).nextVersion
      const result = await addModule(root, {
        code: 'audit-center',
        title: '审计中心',
        description: '审计与追踪',
        parentMenuId: 0,
        menuId: 9100,
      }, true)
      assert.equal(result.version, expectedVersion)
      assert.equal(result.menuId, 9100)
      assert(result.changes.size >= 18)
      await assert.rejects(() => access(path.join(root, 'nz-server/nz-module/nz-audit-center')))
    } finally {
      await rm(root, { recursive: true, force: true })
    }
  })

  it('creates backend, frontend, permissions and migrations, then rolls back', async () => {
    const root = await createProjectFixture()
    try {
      const modulePom = path.join(root, 'nz-server/nz-module/pom.xml')
      const expectedVersion = (await checkMigrations(root)).nextVersion
      const before = await readFile(modulePom, 'utf8')
      const result = await addModule(root, {
        code: 'audit-center',
        title: '审计中心',
        description: '审计与追踪',
        parentMenuId: 1000,
        menuId: 9100,
      })
      const moduleRoot = path.join(root, 'nz-server/nz-module/nz-audit-center')
      await access(path.join(moduleRoot, 'pom.xml'))
      await access(path.join(root, 'nz-web/src/views/audit-center/index.vue'))
      const controller = await readFile(
        path.join(moduleRoot, 'src/main/java/com/nz/admin/modules/auditcenter/controller/ModuleInfoController.java'),
        'utf8',
      )
      assert(controller.split('\n').length > 20)
      const migration = await readFile(
        path.join(root, `nz-server/nz-app/src/main/resources/db/migration/V${expectedVersion}__audit_center_module.sql`),
        'utf8',
      )
      assert.match(migration, /audit-center:module:view/)
      assert.match(await readFile(modulePom, 'utf8'), /<module>nz-audit-center<\/module>/)
      assert.doesNotMatch(await readFile(path.join(root, 'nz-server/pom.xml'), 'utf8'), /\\n/)
      assert.match(
        await readFile(path.join(root, 'nz-server/nz-app/src/main/resources/application.yml'), 'utf8'),
        /audit-center:\n      enabled: true/,
      )
      assert.equal((await checkMigrations(root)).nextVersion, expectedVersion + 1)

      await rollbackBackup(root, result.backup.id)
      assert.equal(await readFile(modulePom, 'utf8'), before)
      await assert.rejects(() => access(moduleRoot))
    } finally {
      await rm(root, { recursive: true, force: true })
    }
  })

  it('toggles an existing module and restores the previous setting', async () => {
    const root = await createProjectFixture()
    try {
      const application = path.join(root, 'nz-server/nz-app/src/main/resources/application.yml')
      const before = await readFile(application, 'utf8')
      const result = await toggleModule(root, 'demo', false)
      assert.match(await readFile(application, 'utf8'), /demo:\n      enabled: false/)
      await rollbackBackup(root, result.backup.id)
      assert.equal(await readFile(application, 'utf8'), before)
    } finally {
      await rm(root, { recursive: true, force: true })
    }
  })
})
