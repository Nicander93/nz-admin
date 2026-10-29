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
| GET | `/api/system/realtime/notifications/ticket` | `system:message:list` | 签发仅接收个人消息事件的通知票据 |
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

Vite 开发服务器与生产预览只代理 `/realtime/sse` 和 `/realtime/ws`，避免截获诊断台 `/realtime` 页面的文档请求。实时代理保留浏览器的 `Host`（`changeOrigin: false`），让后端按默认同源策略核对 WebSocket 的 `Origin`。若改写成后端 Host 而保留原 Origin，取票可以成功，握手却会返回 403。

容器 Nginx 对 SSE 关闭响应缓冲，并为 WebSocket 转发原 Host、`Upgrade` 和 `Connection` 请求头。自定义网关也应保留这些配置。确有跨域需求时明确配置允许来源，不应为修复代理问题放开所有来源。

V13 Flyway 迁移注册“实时通信”菜单和 `system:realtime:send` 按钮权限。旧库可以执行：

    nz-server/nz-app/src/main/resources/db/upgrade-p13-realtime-communication.sql

## 当前限制

单节点默认使用内存状态。开启 `NZ_CLUSTER_ENABLED=true` 后，票据与撤销状态由 Redis 共享，
用户/租户消息与退出通过 Redis Pub/Sub 跨节点转发；所有节点需要同一 Redis 数据库与前缀。
连接与统计仍属于当前节点。Pub/Sub 不提供离线可靠投递，历史通知由站内消息数据库保存。
配置与实际验收见[双节点指南](/operations/cluster)。

## 验证

    cd nz-server
    JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw \
      -pl nz-framework/nz-starter-realtime,nz-module/nz-system,nz-app -am test

    cd ../nz-web
    pnpm test
    pnpm build

## 业务授权与作用域

`RealtimeAccessPolicy` 与 `IdentityValidator` 共同约束票据和长连接。前者核对用途与当前权限，后者核对账号、租户状态及身份版本。Spring 集成缺少策略时拒绝授权，不能把可选注入变成默认放行。

连接 principal 包含固定 scope。系统实现中 `GENERAL` 用于实时测试台；`USER_NOTIFICATIONS` 用于个人消息。投递时同时传入 `USER/TENANT/BROADCAST` 类型，策略可以丢弃当前通道不允许的事件。过滤一个事件不会关闭合法连接；权限撤销或验证失败会关闭。Redis 转发保留投递类型，远端节点执行同样的策略。

实现自定义策略时，应从 principal 恢复接收人的租户上下文，复用业务权限服务，不借用发布线程的登录态。安全读取需要隔离业务事务，避免未提交的修改造成误撤销。策略不会改变数据库收件箱作为消息事实来源的规则。

带 scope 的共享票据已切换到 v2 Redis 键；节点升级要求与通知客户端行为见[站内消息中心](/message-center)。
