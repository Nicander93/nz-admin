# 实时通信

`nz-starter-realtime` 提供 SSE 和原生 WebSocket 传输。业务模块通过
`RealtimePublisher` 发布消息，不直接持有浏览器连接。

## 连接流程

浏览器不能为 `EventSource` 和原生 `WebSocket` 统一设置认证请求头，因此连接分两步：

1. 调用 `GET /api/system/realtime/ticket?transport=SSE|WEBSOCKET` 获取一次性票据。
2. 使用票据连接 `/realtime/sse` 或 `/realtime/ws`。

票据默认 30 秒失效，只能消费一次，并且只能用于签发时指定的传输类型。票据关联的用户
ID 和租户 ID 来自服务端登录上下文，不读取浏览器提交的租户字段。

SSE 连接异常后，前端会关闭浏览器自带的自动重连。重新连接必须重新领取票据，旧票据
不能重放。

用户正常退出或管理员在“在线用户”页面强制退出时，服务端会撤销该用户尚未消费的票据，
并主动关闭其在当前租户下的 SSE 和 WebSocket 连接。

## 管理接口

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/system/realtime/ticket` | `system:realtime:view` | 签发 SSE 或 WebSocket 票据 |
| GET | `/api/system/realtime/stats` | `system:realtime:view` | 返回当前节点连接数 |
| POST | `/api/system/realtime/test` | `system:realtime:send` | 向当前用户在当前租户下的连接发送测试消息 |

测试发送请求：

    {
      "message": "hello"
    }

返回值是成功投递的连接数量。同一用户同时打开 SSE 和 WebSocket 时，返回值可能大于
1。

## 业务发布

业务服务注入 `RealtimePublisher` 后可以按用户、租户或当前节点广播：

    publisher.publishToUser(tenantId, userId, RealtimeMessage.of("notice", payload));
    publisher.publishToTenant(tenantId, RealtimeMessage.of("job-progress", payload));
    publisher.broadcast(RealtimeMessage.of("maintenance", payload));

用户定向发布必须同时提供租户 ID 和用户 ID，避免相同用户 ID 或调用错误造成跨租户
消息泄漏。

## 配置

    nz:
      realtime:
        enabled: true
        ticket-ttl: 30s
        sse-timeout: 30m
        allowed-origins: []

对应环境变量：

- `NZ_REALTIME_ENABLED`
- `NZ_REALTIME_TICKET_TTL`
- `NZ_REALTIME_SSE_TIMEOUT`

`allowed-origins` 为空时使用 Spring WebSocket 的同源限制。只有前后端确实跨域部署时才应
配置允许来源。

## 部署

Vite 开发服务器代理 `/realtime`。容器 Nginx 对 SSE 关闭响应缓冲，并为 WebSocket
转发 `Upgrade` 和 `Connection` 请求头。自定义网关时需要保留这两类配置，否则页面
可能能领取票据，但连接无法建立。

V13 Flyway 迁移注册“实时通信”菜单和 `system:realtime:send` 按钮权限。旧库可以执行：

    nz-server/nz-app/src/main/resources/db/upgrade-p13-realtime-communication.sql

## 当前限制

连接、票据和发布器都保存在当前 JVM。单节点部署可以直接使用；多节点部署需要增加
Redis Pub/Sub 或消息代理，并让一次性票据在节点间共享，或者确保票据签发与连接握手
落在同一节点。本切片没有把节点内统计伪装成集群统计。

## 验证

    cd nz-server
    JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw \
      -pl nz-framework/nz-starter-realtime,nz-module/nz-system,nz-app -am test

    cd ../nz-web
    pnpm test
    pnpm build
