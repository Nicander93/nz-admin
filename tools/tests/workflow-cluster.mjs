import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
import { setTimeout as delay } from 'node:timers/promises'

const nodes = [process.env.NZ_CLUSTER_NODE_A ?? 'http://127.0.0.1:8080', process.env.NZ_CLUSTER_NODE_B ?? 'http://127.0.0.1:8081']
const credentials = { tenantCode: process.env.E2E_TENANT_CODE ?? 'default', clientId: 'nz-web-account', username: process.env.E2E_USERNAME, password: process.env.E2E_PASSWORD }
assert(credentials.username && credentials.password, 'Configure E2E_USERNAME and E2E_PASSWORD for an isolated test database')
let token = ''
async function response(node, path, method = 'GET', body, key) {
  const result = await fetch(nodes[node] + path, { method, signal: AbortSignal.timeout(15000),
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: token } : {}), ...(key ? { 'Idempotency-Key': key } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body) })
  return result.json()
}
async function api(node, path, method = 'GET', body, key) {
  const result = await response(node, path, method, body, key)
  assert.equal(result.code, 200, `${method} ${path}: ${result.msg}`)
  return result.data
}
async function eventually(predicate, label) {
  const end = Date.now() + 15000
  while (Date.now() < end) { if (await predicate()) return; await delay(100) }
  assert.fail(label)
}

// Authenticate on one process and use its session on the other.
token = await api(0, '/api/auth/login', 'POST', credentials)
const identity = await api(1, '/api/auth/info')
const code = `cluster_${randomUUID().replaceAll('-', '')}`
const drafts = await Promise.all([0, 1, 0].map(node => api(node, '/api/workflow/designer/definitions', 'POST', { flowCode: code, flowName: code, businessType: 'leave' }, randomUUID())))
await Promise.all(drafts.map((id, index) => api(index % 2, `/api/workflow/engine/definitions/${id}/publish`, 'POST')))
const definitions = (await api(1, '/api/workflow/designer/definitions')).filter(row => row.flowCode === code)
assert.equal(new Set(definitions.map(row => row.version)).size, 3, 'Concurrent versions must be distinct')
assert.equal(definitions.filter(row => row.isPublish === 1).length, 1, 'Exactly one version is current')

const leave = await api(0, '/api/demo/leave', 'POST', { flowCode: code, reason: code, startDate: '2026-10-10', endDate: '2026-10-11' }, randomUUID())
const submitted = await Promise.all([0, 1].map(node => api(node, `/api/demo/leave/${leave}/submit`, 'POST', undefined, randomUUID())))
assert.equal(submitted[0], submitted[1], 'Different request keys must still start one business instance')
const instance = submitted[0]
const snapshot = await api(1, `/api/workflow/engine/instances/${instance}`)
const task = snapshot.tasks[0].id
const actionKey = randomUUID()
const action = { type: 'PASS', comment: code }
await Promise.all([0, 1].map(node => api(node, `/api/workflow/engine/tasks/${task}/action`, 'POST', action, actionKey)))
const finished = await api(0, `/api/workflow/engine/instances/${instance}`)
assert.equal(finished.tasks.length, 0)
assert.equal(finished.history.filter(item => item.message === code).length, 1, 'Shared idempotency must not duplicate approvals')
await eventually(async () => (await api(1, '/api/demo/leave')).find(row => row.id === leave)?.status === 'APPROVED', 'Both callback workers must converge on approved business state')
console.log('PASS: shared session, concurrent versions/publication, duplicate submit, cross-node approval and callback delivery')

// Issue a ticket on A, consume it on B, and receive a message published on A.
const ticket = await api(0, '/api/system/realtime/ticket?transport=SSE')
const stop = new AbortController()
const stream = await fetch(`${nodes[1]}${ticket.path}?ticket=${encodeURIComponent(ticket.ticket)}`, { signal: stop.signal })
assert.equal(stream.status, 200)
const reader = stream.body.getReader()
let received = ''
let closed = false
const consume = (async () => {
  try { while (true) { const { done, value } = await reader.read(); if (done) break; received += new TextDecoder().decode(value) } }
  catch (error) { if (!stop.signal.aborted) throw error }
  finally { closed = true }
})()
try {
  const marker = `cluster-message-${randomUUID()}`
  await api(0, '/api/system/message/send', 'POST', { category: 'system', title: marker, content: marker, targetType: 'USERS', userIds: [identity.user.id] })
  await eventually(() => received.includes(marker), 'Redis Pub/Sub must reach the SSE connection on the other node')
  const reused = await fetch(`${nodes[0]}${ticket.path}?ticket=${encodeURIComponent(ticket.ticket)}`, { signal: AbortSignal.timeout(5000) })
  assert.equal(reused.status, 401, 'Ticket cannot be consumed twice across nodes')
  await api(0, '/api/auth/logout', 'POST')
  await eventually(() => closed, 'Logout must close remote realtime connections')
  assert.equal((await response(1, '/api/auth/info')).code, 401, 'Logout must revoke the shared session')
  console.log('PASS: cross-node ticket consumption, message broadcast and logout revocation')
} finally { stop.abort(); await consume }
