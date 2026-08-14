import { readFile } from 'node:fs/promises'
import path from 'node:path'
import { applyChanges } from './backup.mjs'
import { CliError, invariant } from './errors.mjs'

const REQUIRED_FIELDS = [
  'schemaName',
  'tableName',
  'moduleName',
  'businessName',
  'className',
  'packageName',
  'featureName',
  'author',
  'parentMenuId',
]

export async function readGeneratorRequest(file) {
  let request
  try {
    request = JSON.parse(await readFile(file, 'utf8'))
  } catch (error) {
    throw new CliError(`无法读取生成器请求：${error.message}`)
  }
  invariant(request && typeof request === 'object' && !Array.isArray(request), '生成器请求必须是 JSON 对象')
  const missing = REQUIRED_FIELDS.filter((field) => request[field] === undefined || request[field] === '')
  invariant(missing.length === 0, `生成器请求缺少字段：${missing.join(', ')}`)
  invariant(Number.isSafeInteger(request.parentMenuId) && request.parentMenuId >= 0, 'parentMenuId 必须是非负整数')
  return request
}

function validateServer(server) {
  let url
  try {
    url = new URL(server)
  } catch {
    throw new CliError('--server 必须是合法 URL')
  }
  invariant(url.protocol === 'http:' || url.protocol === 'https:', '--server 只支持 HTTP 或 HTTPS')
  return url.toString().replace(/\/$/, '')
}

export async function downloadCode(root, input, dryRun = false, fetchImpl = fetch) {
  const request = await readGeneratorRequest(path.resolve(input.requestFile))
  const server = validateServer(input.server || 'http://localhost:8080')
  const output = path.resolve(input.output || path.join(root, `${request.moduleName}-${request.businessName}.zip`))
  const relative = path.relative(root, output)
  invariant(relative !== '' && !relative.startsWith('..') && !path.isAbsolute(relative), '输出文件必须位于项目目录内')

  if (dryRun) return { request, server, output, backup: undefined }
  invariant(typeof input.token === 'string' && input.token.length > 0, '缺少登录令牌：使用 --token 或 NZ_TOKEN')

  const response = await fetchImpl(`${server}/api/generator/download`, {
    method: 'POST',
    headers: {
      Authorization: input.token,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  })
  const contentType = response.headers.get('content-type') || ''
  if (!response.ok || contentType.includes('application/json')) {
    const text = await response.text()
    let message = text
    try {
      const payload = JSON.parse(text)
      message = payload.msg || payload.message || text
    } catch {
      // 保留服务端原始文本，便于定位代理或网关错误。
    }
    throw new CliError(`代码生成失败（HTTP ${response.status}）：${message}`, 1)
  }
  const data = Buffer.from(await response.arrayBuffer())
  invariant(data.length > 0, '生成器返回了空文件')
  const backup = await applyChanges(root, 'codegen', new Map([[output, data]]))
  return { request, server, output, bytes: data.length, backup }
}
