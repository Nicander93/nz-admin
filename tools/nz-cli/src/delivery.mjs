import { access, readFile } from 'node:fs/promises'
import path from 'node:path'
import { CliError } from './errors.mjs'
import { runChecked } from './processes.mjs'

const REQUIRED_FILES = [
  'deploy/compose.yaml',
  'deploy/.env.example',
  'nz-server/Dockerfile',
  'nz-server/.dockerignore',
  'nz-web/Dockerfile',
  'nz-web/.dockerignore',
  'nz-web/nginx.conf',
  'docs/deployment.md',
]

const REQUIRED_ENVIRONMENT = [
  'NZ_DB_PASSWORD',
  'NZ_INITIAL_ADMIN_PASSWORD',
  'NZ_FILE_CONFIG_KEY',
  'NZ_MINIO_ROOT_PASSWORD',
]

function parseEnvironment(content) {
  const values = new Map()
  for (const sourceLine of content.split(/\r?\n/)) {
    const line = sourceLine.trim()
    if (!line || line.startsWith('#')) continue
    const separator = line.indexOf('=')
    if (separator <= 0) continue
    const key = line.slice(0, separator).trim()
    let value = line.slice(separator + 1).trim()
    if ((value.startsWith('"') && value.endsWith('"'))
        || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1)
    }
    values.set(key, value)
  }
  return values
}

function enabled(value) {
  return String(value).toLowerCase() === 'true'
}

export function inferDeliveryProfiles(environment) {
  const profiles = []
  if (enabled(environment.get('NZ_REDIS_ENABLED'))) profiles.push('redis')
  if (environment.get('NZ_FILE_STORAGE_TYPE') === 's3') profiles.push('storage')
  return profiles
}

export function validateDeliveryEnvironment(environment) {
  for (const key of REQUIRED_ENVIRONMENT) {
    if (!environment.get(key)) throw new CliError(`部署环境缺少配置：${key}`)
  }
  const secrets = [
    ['NZ_DB_PASSWORD', 16],
    ['NZ_INITIAL_ADMIN_PASSWORD', 8],
    ['NZ_FILE_CONFIG_KEY', 32],
    ['NZ_MINIO_ROOT_PASSWORD', 16],
  ]
  for (const [key, minimum] of secrets) {
    const value = environment.get(key)
    if (/replace|change[-_]?me|example/i.test(value) || value.length < minimum) {
      throw new CliError(`部署配置 ${key} 仍是示例值或少于 ${minimum} 个字符`)
    }
  }
  if (enabled(environment.get('NZ_FIELD_ENCRYPTION_ENABLED'))) {
    const key = environment.get('NZ_FIELD_ENCRYPTION_KEY') || ''
    if (key.length < 32) throw new CliError('启用字段加密时，NZ_FIELD_ENCRYPTION_KEY 至少需要 32 个字符')
  }
  return { profiles: inferDeliveryProfiles(environment) }
}

async function readEnvironmentFile(root, value, validateSecrets) {
  const file = path.resolve(root, value)
  let content
  try {
    content = await readFile(file, 'utf8')
  } catch {
    throw new CliError(`无法读取部署环境文件：${path.relative(root, file)}`)
  }
  const environment = parseEnvironment(content)
  return {
    file,
    environment,
    profiles: validateSecrets ? validateDeliveryEnvironment(environment).profiles : inferDeliveryProfiles(environment),
  }
}

export async function checkDelivery(root, options = {}) {
  for (const relative of REQUIRED_FILES) {
    try {
      await access(path.join(root, relative))
    } catch {
      throw new CliError(`缺少交付文件：${relative}`)
    }
  }

  const compose = await readFile(path.join(root, 'deploy/compose.yaml'), 'utf8')
  for (const service of ['postgres:', 'backend:', 'frontend:', 'redis:', 'minio:']) {
    if (!compose.includes(service)) throw new CliError(`Compose 缺少服务：${service.slice(0, -1)}`)
  }
  if (!compose.includes('minio-init:')) throw new CliError('Compose 缺少服务：minio-init')
  for (const marker of [
    'condition: service_healthy',
    'NZ_INITIAL_ADMIN_PASSWORD',
    'NZ_FILE_CONFIG_KEY',
    'profiles: ["redis"]',
    'profiles: ["storage"]',
    '/actuator/health/readiness',
    'postgres-data:',
    'upload-data:',
  ]) {
    if (!compose.includes(marker)) throw new CliError(`Compose 缺少交付约束：${marker}`)
  }

  let profiles = []
  if (options.envFile) {
    profiles = (await readEnvironmentFile(root, options.envFile, true)).profiles
  } else {
    const example = await readEnvironmentFile(root, 'deploy/.env.example', false)
    for (const key of REQUIRED_ENVIRONMENT) {
      if (!example.environment.has(key)) throw new CliError(`环境模板缺少配置：${key}`)
    }
  }
  return { files: REQUIRED_FILES.length, services: 6, profiles }
}

function normalizeProfiles(profiles) {
  const result = new Set()
  for (const profile of profiles || []) {
    if (!['redis', 'storage'].includes(profile)) throw new CliError(`不支持 Compose profile：${profile}`)
    result.add(profile)
  }
  return [...result].sort()
}

export function deliveryComposeSpec(root, envFile, profiles, action) {
  const args = ['compose', '--env-file', envFile, '-f', path.join(root, 'deploy', 'compose.yaml')]
  for (const profile of normalizeProfiles(profiles)) args.push('--profile', profile)
  if (action === 'config') args.push('config', '--quiet')
  else if (action === 'up') args.push('up', '-d', '--build')
  else if (action === 'down') args.push('down')
  else if (action === 'ps') args.push('ps')
  else throw new CliError(`不支持交付操作：${action}`)
  return { command: 'docker', args, cwd: root }
}

export async function runDeliveryCompose(root, options = {}, spawnSyncImpl) {
  const environment = await readEnvironmentFile(root, options.envFile || 'deploy/.env', true)
  const requestedProfiles = String(options.profiles || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
  const profiles = normalizeProfiles([...environment.profiles, ...requestedProfiles])
  const run = (action) =>
    runChecked(deliveryComposeSpec(root, environment.file, profiles, action), spawnSyncImpl)

  if (options.action === 'up') {
    run('config')
    run('up')
  } else {
    run(options.action)
  }
  return { profiles }
}

export async function smokeDelivery(baseUrl, fetchImpl = fetch) {
  const origin = String(baseUrl || 'http://127.0.0.1').replace(/\/$/, '')
  const checks = [
    { name: '前端', path: '/', validate: (body) => body.includes('id="app"') },
    { name: 'Nginx', path: '/healthz', validate: (body) => body.trim() === 'ok' },
    { name: '后端', path: '/health/ready', validate: (body) => JSON.parse(body).status === 'UP' },
  ]
  const results = []
  for (const check of checks) {
    let response
    try {
      response = await fetchImpl(origin + check.path, { signal: AbortSignal.timeout(10000) })
    } catch (error) {
      throw new CliError(`${check.name}冒烟失败：${error.message}`)
    }
    const body = await response.text()
    if (!response.ok || !check.validate(body)) {
      throw new CliError(`${check.name}冒烟失败：HTTP ${response.status}`)
    }
    results.push({ name: check.name, url: origin + check.path })
  }
  return results
}
