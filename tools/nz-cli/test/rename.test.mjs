import assert from 'node:assert/strict'
import { mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { describe, it } from 'node:test'
import { rollbackBackup } from '../src/backup.mjs'
import { renameProject } from '../src/rename.mjs'
import { createProjectFixture } from './helpers.mjs'

describe('rename command', () => {
  it('replaces text files, excludes vendored directories and supports rollback', async () => {
    const root = await createProjectFixture()
    try {
      const source = path.join(root, 'sample.md')
      const vendored = path.join(root, 'ruoyi-vue-pro/sample.md')
      await writeFile(source, 'nz-admin / nz-admin\n')
      await mkdir(path.dirname(vendored), { recursive: true })
      await writeFile(vendored, 'nz-admin\n')

      const result = await renameProject(root, 'nz-admin', 'acme-admin')
      assert.equal(await readFile(source, 'utf8'), 'acme-admin / acme-admin\n')
      assert.equal(await readFile(vendored, 'utf8'), 'nz-admin\n')
      assert(result.matches.some((match) => match.path === 'sample.md'))

      await rollbackBackup(root, result.backup.id)
      assert.equal(await readFile(source, 'utf8'), 'nz-admin / nz-admin\n')
    } finally {
      await rm(root, { recursive: true, force: true })
    }
  })
})
