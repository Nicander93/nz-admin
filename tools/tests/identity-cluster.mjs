import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { setTimeout as delay } from "node:timers/promises";

import { loginByHttp } from "./login.mjs";

const nodes = [
  process.env.NZ_CLUSTER_NODE_A ?? "http://127.0.0.1:8080",
  process.env.NZ_CLUSTER_NODE_B ?? "http://127.0.0.1:8081",
];
const credentials = {
  tenantCode: process.env.E2E_TENANT_CODE ?? "default",
  clientId: "nz-web-account",
  username: process.env.E2E_USERNAME,
  password: process.env.E2E_PASSWORD,
};
assert(
  credentials.username && credentials.password,
  "Configure an isolated test database and E2E credentials",
);
async function response(node, token, path, method = "GET", data) {
  const result = await fetch(nodes[node] + path, {
    method,
    signal: AbortSignal.timeout(10000),
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: token } : {}),
    },
    body: data === undefined ? undefined : JSON.stringify(data),
  });
  return result.json();
}
async function api(node, token, path, method = "GET", data) {
  const result = await response(node, token, path, method, data);
  assert.equal(result.code, 200, `${method} ${path}: ${result.msg}`);
  return result.data;
}
async function until(predicate, message) {
  const deadline = Date.now() + 10000;
  while (Date.now() < deadline) {
    if (await predicate()) return;
    await delay(50);
  }
  assert.fail(message);
}
async function connect(node, token, transport) {
  const ticket = await api(
    node,
    token,
    `/api/system/realtime/ticket?transport=${transport}`,
  );
  const url = new URL(ticket.path, nodes[1 - node]);
  url.searchParams.set("ticket", ticket.ticket);
  let closed = false;
  let content = "";
  if (transport === "SSE") {
    const abort = new AbortController();
    const handshakeTimeout = setTimeout(() => abort.abort(), 10000);
    let result;
    try {
      result = await fetch(url, { signal: abort.signal });
    } finally {
      clearTimeout(handshakeTimeout);
    }
    assert.equal(result.status, 200);
    const reader = result.body.getReader();
    const read = (async () => {
      try {
        while (true) {
          const item = await reader.read();
          if (item.done) break;
          content += new TextDecoder().decode(item.value);
        }
      } catch {
        closed = true;
      } finally {
        closed = true;
      }
    })();
    try {
      await until(
        () => content.includes("connected") || closed,
        "SSE must connect on the other node",
      );
      assert(
        content.includes("connected"),
        "SSE closed before confirming the connection",
      );
    } catch (error) {
      abort.abort();
      await read;
      throw error;
    }
    return {
      isClosed: () => closed,
      close: async () => {
        abort.abort();
        await read;
      },
    };
  }
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  const socket = new WebSocket(url);
  socket.addEventListener("message", (event) => {
    content += String(event.data);
  });
  socket.addEventListener("close", () => {
    closed = true;
  });
  socket.addEventListener("error", () => {
    closed = true;
  });
  try {
    await until(
      () => content.includes("connected") || closed,
      "WebSocket must connect on the other node",
    );
    assert(
      content.includes("connected"),
      "WebSocket closed before confirming the connection",
    );
  } catch (error) {
    socket.close();
    throw error;
  }
  return {
    isClosed: () => closed,
    close: async () => {
      socket.close();
      await until(() => closed, "WebSocket cleanup must finish");
    },
  };
}

const admin = await loginByHttp(nodes[0], credentials);
const suffix = randomUUID().replaceAll("-", "");
const username = `identity_${suffix.slice(0, 20)}`;
const roleKey = `identity_${suffix.slice(0, 20)}`;
const password = "Identity-cluster-password-123";
const changedPassword = "Identity-cluster-changed-123";
let userId;
let roleId;
const connections = [];
try {
  await api(0, admin, "/api/system/user", "POST", {
    username,
    password,
    nickname: username,
    status: 0,
  });
  const users = await api(
    0,
    admin,
    `/api/system/user/page?username=${username}`,
  );
  userId = users.records.find((user) => user.username === username)?.id;
  assert(userId, "Created user must be queryable");
  await api(0, admin, "/api/system/role", "POST", {
    name: roleKey,
    roleKey,
    status: 0,
    sort: 0,
    dataScope: 5,
  });
  roleId = (await api(0, admin, "/api/system/role/listAll")).find(
    (role) => role.roleKey === roleKey,
  )?.id;
  assert(roleId, "Created role must be queryable");
  const menus = await api(0, admin, "/api/system/menu/list");
  await api(
    0,
    admin,
    `/api/system/role/${roleId}/menus`,
    "PUT",
    menus
      .filter((menu) => menu.perm === "system:realtime:view")
      .map((menu) => menu.id),
  );
  await api(0, admin, `/api/system/user/${userId}/roles`, "PUT", [roleId]);
  let previousLogin = 0;
  const login = async (node, value) => {
    // 同一浏览器客户端的多次真实登录遵守一秒防重窗口。
    const remaining = 1100 - (Date.now() - previousLogin);
    if (remaining > 0) await delay(remaining);
    const token = await loginByHttp(nodes[node], {
      ...credentials,
      username,
      password: value,
    });
    previousLogin = Date.now();
    return token;
  };
  const old = await login(1, password);
  await api(0, old, "/api/auth/info");
  const sse = await connect(0, old, "SSE");
  connections.push(sse);
  const ws = await connect(1, old, "WEBSOCKET");
  connections.push(ws);
  const pending = await api(
    0,
    old,
    "/api/system/realtime/ticket?transport=SSE",
  );

  await api(0, admin, "/api/system/user", "PUT", { id: userId, status: 1 });
  assert.equal((await response(1, old, "/api/auth/info")).code, 401);
  await until(
    () => sse.isClosed() && ws.isClosed(),
    "Both remote transports must close after disable",
  );
  const rejected = await fetch(
    `${nodes[1]}${pending.path}?ticket=${encodeURIComponent(pending.ticket)}`,
  );
  assert.equal(
    rejected.status,
    401,
    "Pending ticket must fail after identity invalidation",
  );

  await api(1, admin, "/api/system/user", "PUT", { id: userId, status: 0 });
  assert.equal(
    (await response(0, old, "/api/auth/info")).code,
    401,
    "Reenable cannot revive an old token",
  );
  const fresh = await login(1, password);
  assert.notEqual(fresh, old);
  await api(0, fresh, "/api/auth/info");
  const freshSse = await connect(0, fresh, "SSE");
  connections.push(freshSse);
  const freshWs = await connect(1, fresh, "WEBSOCKET");
  connections.push(freshWs);

  await api(1, admin, "/api/system/user", "PUT", {
    id: userId,
    password: changedPassword,
  });
  assert.equal((await response(0, fresh, "/api/auth/info")).code, 401);
  await until(
    () => freshSse.isClosed() && freshWs.isClosed(),
    "Password change must close both remote transports",
  );
  const current = await login(0, changedPassword);
  assert.notEqual(current, fresh);
  await api(1, current, "/api/auth/info");
  assert.equal(
    (await response(1, fresh, "/api/auth/info")).code,
    401,
    "New login cannot revive an older token",
  );
  await api(0, current, "/api/auth/logout", "POST");
  console.log(
    "PASS: cross-node identity versions, disable/reenable, password change, SSE/WebSocket closure and pending ticket rejection",
  );
} finally {
  for (const connection of connections) await connection.close();
  if (userId) {
    await api(0, admin, `/api/system/user/${userId}/roles`, "PUT", []);
    await api(0, admin, `/api/system/user/${userId}`, "DELETE");
  }
  if (roleId) await api(0, admin, `/api/system/role/${roleId}`, "DELETE");
  await api(0, admin, "/api/auth/logout", "POST");
}
