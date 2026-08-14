import assert from 'node:assert/strict'
import { readFile, rm, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { describe, it } from 'node:test'
import { rollbackBackup } from '../src/backup.mjs'
import { downloadCode } from '../src/codegen.mjs'
import { CliError } from '../src/errors.mjs'
import { createProjectFixture } from './helpers.mjs'

async function requestFixture(root) {
  const file = path.join(root, 'generator-request.json')
  await writeFile(file, JSON.stringify({
    schemaName: 'public',
    tableName: 'sys_notice',
    moduleName: 'system',
    businessName: 'notice',
    className: 'Notice',
    packageName: 'com.nz.admin.modules.system',
    featureName: '通知管理',
    author: 'test',
    parentMenuId: 1000,
  }))
  return file
}

describe('codegen command', () => {
  it('sends the authenticated request, writes the zip and supports rollback', async () => {
    const root = await createProjectFixture()
    try {
      const requestFile = await requestFixture(root)
      let captured
      const fetchImpl = async (url, init) => {
        captured = { url, init }
        return new Response(Buffer.from('zip-data'), {
          status: 200,
          headers: { 'content-type': 'application/octet-stream' },
        })
      }
      const output = path.join(root, 'generated/notice.zip')
      const result = await downloadCode(root, {
        requestFile,
        server: 'http://127.0.0.1:8080/',
        token: 'token-value',
        output,
      }, false, fetchImpl)

      assert.equal(captured.url, 'http://127.0.0.1:8080/api/generator/download')
      assert.equal(captured.init.headers.Authorization, 'token-value')
      assert.equal(await readFile(output, 'utf8'), 'zip-data')
      await rollbackBackup(root, result.backup.id)
      await assert.rejects(() => readFile(output))
    } finally {
      await rm(root, { recursive: true, force: true })
    }
  })

  it('reports JSON business errors and does not create an output file', async () => {
    const root = await createProjectFixture()
    try {
      const requestFile = await requestFixture(root)
      const fetchImpl = async () => new Response(JSON.stringify({ code: 500, msg: '仅支持单主键' }), {
        status: 400,
        headers: { 'content-type': 'application/json' },
      })
      await assert.rejects(
        () => downloadCode(root, { requestFile, token: 'token' }, false, fetchImpl),
        (error) => error instanceof CliError && error.message.includes('仅支持单主键'),
      )
    } finally {
      await rm(root, { recursive: true, force: true })
    }
  })

  it('validates a dry-run without requiring a token or network', async () => {
    const root = await createProjectFixture()
    try {
      const requestFile = await requestFixture(root)
      const result = await downloadCode(root, { requestFile }, true, () => {
        throw new Error('should not fetch')
      })
      assert.equal(result.request.tableName, 'sys_notice')
    } finally {
      await rm(root, { recursive: true, force: true })
    }
  })
})
