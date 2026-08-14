import assert from 'node:assert/strict'
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import { describe, it } from 'node:test'
import {
  checkDelivery,
  deliveryComposeSpec,
  inferDeliveryProfiles,
  runDeliveryCompose,
  smokeDelivery,
  validateDeliveryEnvironment,
} from '../src/delivery.mjs'
import { CliError } from '../src/errors.mjs'
import { sourceRoot } from './helpers.mjs'

describe('delivery commands', () => {
  it('checks the repository delivery contract', async () => {
    const result = await checkDelivery(sourceRoot)
    assert.equal(result.files, 8)
    assert.equal(result.services, 6)
  })

  it('validates deployment secrets and infers optional profiles', () => {
    const environment = new Map([
      ['NZ_DB_PASSWORD', 'database-password-123'],
      ['NZ_INITIAL_ADMIN_PASSWORD', 'admin-password-123'],
      ['NZ_FILE_CONFIG_KEY', '12345678901234567890123456789012'],
      ['NZ_MINIO_ROOT_PASSWORD', 'minio-password-123'],
      ['NZ_REDIS_ENABLED', 'true'],
      ['NZ_FILE_STORAGE_TYPE', 's3'],
    ])
    assert.deepEqual(validateDeliveryEnvironment(environment).profiles, ['redis', 'storage'])
    assert.deepEqual(inferDeliveryProfiles(environment), ['redis', 'storage'])

    environment.set('NZ_DB_PASSWORD', 'replace-with-password')
    assert.throws(
      () => validateDeliveryEnvironment(environment),
      (error) => error instanceof CliError && error.message.includes('NZ_DB_PASSWORD'),
    )
  })

  it('builds deterministic compose commands', () => {
    const spec = deliveryComposeSpec(
      '/workspace/nz-admin',
      '/workspace/nz-admin/deploy/.env',
      ['storage', 'redis', 'redis'],
      'up',
    )
    assert.equal(spec.command, 'docker')
    assert.deepEqual(spec.args.slice(0, 5), [
      'compose', '--env-file', '/workspace/nz-admin/deploy/.env',
      '-f', '/workspace/nz-admin/deploy/compose.yaml',
    ])
    assert.deepEqual(spec.args.slice(-7), ['--profile', 'redis', '--profile', 'storage', 'up', '-d', '--build'])
  })

  it('validates config before starting and selects profiles from the environment', async () => {
    const root = await mkdtemp(path.join(os.tmpdir(), 'nz-delivery-test-'))
    try {
      await mkdir(path.join(root, 'deploy'))
      await writeFile(path.join(root, 'deploy', '.env'), [
        'NZ_DB_PASSWORD=database-password-123',
        'NZ_INITIAL_ADMIN_PASSWORD=admin-password-123',
        'NZ_FILE_CONFIG_KEY=12345678901234567890123456789012',
        'NZ_MINIO_ROOT_PASSWORD=minio-password-123',
        'NZ_REDIS_ENABLED=true',
        'NZ_FILE_STORAGE_TYPE=local',
      ].join('\n'))
      const calls = []
      const result = await runDeliveryCompose(root, {
        action: 'up',
        envFile: 'deploy/.env',
      }, (command, args, options) => {
        calls.push({ command, args, options })
        return { status: 0 }
      })

      assert.deepEqual(result.profiles, ['redis'])
      assert.equal(calls.length, 2)
      assert.equal(calls[0].args.at(-2), 'config')
      assert.equal(calls[0].args.at(-1), '--quiet')
      assert.deepEqual(calls[1].args.slice(-3), ['up', '-d', '--build'])
      assert(calls.every((call) => call.args.includes('redis')))
    } finally {
      await rm(root, { recursive: true, force: true })
    }
  })

  it('smokes frontend, proxy and backend readiness', async () => {
    const responses = new Map([
      ['http://example.test/', '<div id="app"></div>'],
      ['http://example.test/healthz', 'ok\n'],
      ['http://example.test/health/ready', '{"status":"UP"}'],
    ])
    const result = await smokeDelivery('http://example.test/', async (url) => ({
      ok: responses.has(url),
      status: responses.has(url) ? 200 : 404,
      text: async () => responses.get(url) ?? '',
    }))
    assert.deepEqual(result.map((item) => item.name), ['前端', 'Nginx', '后端'])
  })
})
