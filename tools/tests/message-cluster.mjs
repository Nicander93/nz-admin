import assert from 'node:assert/strict'
import { randomUUID } from 'node:crypto'
import { setTimeout as delay } from 'node:timers/promises'

import { loginByHttp } from './login.mjs'

const nodes = [
  process.env.NZ_CLUSTER_NODE_A ?? 'http://127.0.0.1:8080',
  process.env.NZ_CLUSTER_NODE_B ?? 'http://127.0.0.1:8081',
]
const credentials = {
  tenantCode: process.env.E2E_TENANT_CODE ?? 'default',
  clientId: 'nz-web-account',
  username: process.env.E2E_USERNAME,
  password: process.env.E2E_PASSWORD,
}
assert(credentials.username && credentials.password, 'Configure isolated test databases and E2E credentials')
const admin = await loginByHttp(nodes[0], credentials)
const suffix = randomUUID().replaceAll('-', '').slice(0, 16)
const roleKey = `message_cluster_${suffix}`
const password = 'Message-cluster-password-123'
const users = [0, 1].map(index => ({ username: `msg_cluster_${index}_${suffix}` }))
const connections = []
const titles = new Set()
let roleId
let fullMenuIds = []
let failure

try {
  const menus = await api(0, admin, '/api/system/menu/list')
  const permissions = ['system:message:list', 'system:message:read', 'system:message:remove']
  fullMenuIds = permissions.map(permission => {
    const menu = menus.find(item => item.perm === permission)
    assert(menu, `Missing permission ${permission}`)
    return menu.id
  })
  await api(0, admin, '/api/system/role', 'POST', {
    name: roleKey, roleKey, status: 0, sort: 0, dataScope: 5,
  })
  roleId = (await api(1, admin, '/api/system/role/listAll')).find(role => role.roleKey === roleKey)?.id
  assert(roleId, 'Created role must be visible on the other node')
  await api(0, admin, `/api/system/role/${roleId}/menus`, 'PUT', fullMenuIds)
  for (const [index, user] of users.entries()) {
    await api(0, admin, '/api/system/user', 'POST', {
      username: user.username, nickname: user.username, password, status: 0,
    })
    user.id = (await api(1, admin, `/api/system/user/page?username=${user.username}`)).records
      .find(row => row.username === user.username)?.id
    assert(user.id, 'Created user must be visible on the other node')
    await api(0, admin, `/api/system/user/${user.id}/roles`, 'PUT', [roleId])
    // Keep real authentication protection enabled across this and the preceding cluster script.
    await delay(1100)
    user.token = await loginByHttp(nodes[index], { ...credentials, username: user.username, password })
    const info = await api(1 - index, user.token, '/api/auth/info')
    assert(info.permissions.includes('system:message:list'))
    assert(!info.permissions.includes('system:realtime:view'))
    assert.equal((await response(index, user.token, '/api/system/realtime/ticket')).code, 403)
    assert.equal(await api(index, user.token, '/api/system/message/unread-count'), 0)
    user.streams = []
    for (const [transport, issuer] of [['SSE', 0], ['WEBSOCKET', 1]]) {
      const stream = await connect(issuer, 1 - issuer, user.token, transport)
      connections.push(stream)
      user.streams.push(stream)
    }
  }
  const [recipient, other] = users
  const general = await connect(0, 1, admin, 'SSE', false)
  connections.push(general)
  const adminNotification = await connect(0, 1, admin, 'SSE')
  connections.push(adminNotification)
  const diagnostic = `diagnostic-${suffix}`
  await api(0, admin, '/api/system/realtime/test', 'POST', { message: diagnostic })
  await until(() => general.events.some(event => event.type === 'test' && event.payload?.message === diagnostic),
    'General diagnostic event must arrive on the other node')

  const firstTitle = `message-cluster-${suffix}-first`
  const otherTitle = `message-cluster-${suffix}-other`
  await send(0, recipient.id, firstTitle)
  await until(() => recipient.streams.every(stream => hasTitle(stream, firstTitle)),
    'Both remote notification transports must receive the private message')
  await send(1, other.id, otherTitle)
  await until(() => other.streams.every(stream => hasTitle(stream, otherTitle)),
    'Control recipient must receive its own private message on both transports')
  for (const stream of recipient.streams) assert(!hasTitle(stream, otherTitle), 'No other-user payload on recipient stream')
  for (const stream of other.streams) assert(!hasTitle(stream, firstTitle), 'No recipient payload on other-user stream')
  assert(!adminNotification.isClosed(), 'Valid admin notification stream must remain connected')
  assert(!adminNotification.events.some(event => event.type === 'test'), 'Notification scope must reject diagnostic payloads')
  assert(!adminNotification.events.some(event => event.type === 'system-message'), 'Admin notifications must not receive other-user messages')

  const first = (await inbox(1, recipient)).find(row => row.title === firstTitle)
  assert(first && first.readStatus === 0, 'Private message must be committed and unread')
  await api(0, recipient.token, `/api/system/message/${first.id}/read`, 'PUT')
  await until(() => recipient.streams.every(stream => stream.events.some(event =>
    event.type === 'system-message-state' && event.payload?.action === 'READ' && event.payload.id === first.id)),
  'Committed read state must reach both nodes/transports')
  assert.equal(await api(1, recipient.token, '/api/system/message/unread-count'), 0)
  assert.equal(await api(0, other.token, '/api/system/message/unread-count'), 1)
  assert(!other.streams.some(stream => stream.events.some(event => event.type === 'system-message-state')),
    'Read state must remain private')
  const reused = await fetch(ticketUrl(0, recipient.streams[0].ticket), { signal: AbortSignal.timeout(5000) })
  assert.equal(reused.status, 401, 'A consumed ticket must be rejected on its issuing node too')

  assert(users.every(user => user.streams.every(stream => stream.isOpen())),
    'All notification transports must be open before permission revocation')
  const pending = await api(0, recipient.token, '/api/system/realtime/notifications/ticket?transport=SSE')
  const listMenuId = menus.find(menu => menu.perm === 'system:message:list').id
  await api(0, admin, `/api/system/role/${roleId}/menus`, 'PUT', fullMenuIds.filter(id => id !== listMenuId))
  assert(!(await api(1, recipient.token, '/api/auth/info')).permissions.includes('system:message:list'),
    'Permission must actually be revoked before checking transports')
  await until(() => users.every(user => user.streams.every(stream => stream.isClosed())),
    'Role revocation must close notification transports on both nodes')
  assert.equal((await response(1, recipient.token, '/api/system/realtime/notifications/ticket')).code, 403)
  const rejected = await fetch(ticketUrl(1, pending), { signal: AbortSignal.timeout(5000) })
  assert.equal(rejected.status, 401, 'A pending cross-node ticket must fail after permission revocation')
  const countsBefore = recipient.streams.map(stream => stream.events.length)
  const offlineTitle = `message-cluster-${suffix}-offline`
  await send(1, recipient.id, offlineTitle)
  assert.deepEqual(recipient.streams.map(stream => stream.events.length), countsBefore,
    'Closed streams cannot receive a newly committed message')

  await api(1, admin, `/api/system/role/${roleId}/menus`, 'PUT', fullMenuIds)
  assert((await api(0, recipient.token, '/api/auth/info')).permissions.includes('system:message:list'))
  const restored = await connect(0, 1, recipient.token, 'SSE')
  connections.push(restored)
  const lastTitle = `message-cluster-${suffix}-restored`
  await send(0, recipient.id, lastTitle)
  await until(() => hasTitle(restored, lastTitle), 'Fresh connection after regrant must receive new messages')
  const history = await inbox(1, recipient)
  assert(history.some(row => row.title === offlineTitle && row.readStatus === 0), 'Offline message must be recoverable from database history')
  assert.equal(await api(1, recipient.token, '/api/system/message/unread-count'), 2)
  const pendingAgain = await fetch(ticketUrl(1, pending), { signal: AbortSignal.timeout(5000) })
  assert.equal(pendingAgain.status, 401, 'Regrant cannot revive a previously rejected ticket')
  console.log('PASS: notification scope across Redis, private SSE/WebSocket, read state, revocation, single-use tickets and offline history')
} catch (error) {
  failure = error
  throw error
} finally {
  const errors = []
  async function clean(action) {
    try { await action() } catch (error) { errors.push(error) }
  }

  for (const stream of connections) await clean(() => stream.close())
  if (!roleId) {
    await clean(async () => {
      roleId = (await api(0, admin, '/api/system/role/listAll'))
        .find(role => role.roleKey === roleKey)?.id
    })
  }
  if (roleId) {
    await clean(() => api(0, admin, `/api/system/role/${roleId}/menus`, 'PUT', fullMenuIds))
  }

  for (const user of users) {
    if (!user.id) {
      await clean(async () => {
        user.id = (await api(0, admin, `/api/system/user/page?username=${encodeURIComponent(user.username)}`))
          .records.find(row => row.username === user.username)?.id
      })
    }
    if (user.token) {
      await clean(async () => {
        for (const row of await inbox(0, user)) {
          if (titles.has(row.title)) {
            await api(0, user.token, `/api/system/message/${row.id}`, 'DELETE')
          }
        }
      })
      await clean(() => api(0, user.token, '/api/auth/logout', 'POST'))
    }
    if (user.id) {
      await clean(() => api(0, admin, `/api/system/user/${user.id}/roles`, 'PUT', []))
      await clean(() => api(0, admin, `/api/system/user/${user.id}`, 'DELETE'))
    }
  }
  if (!roleId) {
    await clean(async () => {
      roleId = (await api(0, admin, '/api/system/role/listAll'))
        .find(role => role.roleKey === roleKey)?.id
    })
  }
  if (roleId) await clean(() => api(0, admin, `/api/system/role/${roleId}`, 'DELETE'))
  await clean(() => api(0, admin, '/api/auth/logout', 'POST'))
  if (errors.length) {
    if (failure) console.error('Cluster cleanup failed after primary error:', ...errors)
    else throw new AggregateError(errors, 'Cluster cleanup failed')
  }
}

async function response(node, token, path, method = 'GET', data) {
  const result = await fetch(nodes[node] + path, {
    method, signal: AbortSignal.timeout(15000),
    headers: { 'Content-Type': 'application/json', Authorization: token },
    body: data === undefined ? undefined : JSON.stringify(data),
  })
  return result.json()
}
async function api(node, token, path, method = 'GET', data) {
  const result = await response(node, token, path, method, data)
  assert.equal(result.code, 200, `${method} ${path}: ${result.msg}`)
  return result.data
}
async function send(node, userId, title) {
  titles.add(title)
  assert.equal(await api(node, admin, '/api/system/message/send', 'POST', {
    category: 'system', title, content: title, targetType: 'USERS', userIds: [userId],
  }), 1)
}
async function inbox(node, user) {
  return (await api(node, user.token, '/api/system/message/page?pageNum=1&pageSize=100')).records
}
async function until(predicate, message) {
  const deadline = Date.now() + 15000
  while (Date.now() < deadline) {
    if (await predicate()) return
    await delay(50)
  }
  assert.fail(message)
}
function hasTitle(stream, title) {
  return stream.events.some(event => event.type === 'system-message' && event.payload?.title === title)
}
function ticketUrl(node, ticket) {
  const url = new URL(ticket.path, nodes[node])
  url.searchParams.set('ticket', ticket.ticket)
  return url
}
async function connect(issuer, receiver, token, transport, notification = true) {
  const path = notification ? 'notifications/ticket' : 'ticket'
  const ticket = await api(issuer, token, `/api/system/realtime/${path}?transport=${transport}`)
  const url = ticketUrl(receiver, ticket)
  const events = []
  let closed = false
  if (transport === 'WEBSOCKET') {
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
    const socket = new WebSocket(url)
    let resolveClosed
    const closedPromise = new Promise(resolve => { resolveClosed = resolve })
    socket.addEventListener('close', () => {
      closed = true
      resolveClosed()
    }, { once: true })
    socket.addEventListener('error', () => {
      if (socket.readyState < WebSocket.CLOSING) socket.close()
    })
    socket.addEventListener('message', event => events.push(JSON.parse(String(event.data))))

    const waitForClose = () => new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error('WebSocket close event must arrive')), 10_000)
      closedPromise.then(() => {
        clearTimeout(timeout)
        resolve()
      })
    })
    try {
      await until(() => socket.readyState === WebSocket.OPEN || closed, 'WebSocket handshake must finish')
      assert.equal(socket.readyState, WebSocket.OPEN, 'Notification WebSocket must be accepted')
    } catch (error) {
      if (socket.readyState < WebSocket.CLOSING) socket.close()
      try { await waitForClose() } catch (cleanupError) {
        console.error('WebSocket cleanup failed after connection error:', cleanupError)
      }
      throw error
    }
    return {
      events,
      ticket,
      isOpen: () => socket.readyState === WebSocket.OPEN,
      isClosed: () => closed && socket.readyState === WebSocket.CLOSED,
      close: async () => {
        if (socket.readyState < WebSocket.CLOSING) socket.close()
        await waitForClose()
      },
    }
  }
  const abort = new AbortController()
  const timeout = setTimeout(() => abort.abort(), 10000)
  let result
  try { result = await fetch(url, { signal: abort.signal }) }
  finally { clearTimeout(timeout) }
  assert.equal(result.status, 200, 'Notification SSE must be accepted')
  const decoder = new TextDecoder()
  let pending = ''
  const reading = (async () => {
    try {
      for await (const chunk of result.body) {
        pending += decoder.decode(chunk, { stream: true }).replaceAll('\r\n', '\n')
        let end
        while ((end = pending.indexOf('\n\n')) >= 0) {
          const frame = pending.slice(0, end)
          pending = pending.slice(end + 2)
          const data = frame.split('\n').filter(line => line.startsWith('data:'))
            .map(line => line.slice(5).trimStart()).join('\n')
          if (data) events.push(JSON.parse(data))
        }
      }
    } catch (error) {
      if (!abort.signal.aborted) throw error
    } finally { closed = true }
  })()
  // Keep a handler attached even if a network failure arrives before cleanup.
  reading.catch(() => {})
  return {
    events, ticket, isOpen: () => !closed, isClosed: () => closed,
    close: async () => { abort.abort(); await reading },
  }
}
