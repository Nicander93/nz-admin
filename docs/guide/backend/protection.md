# 限流、防重与业务幂等

连续点击、请求频率和网络重试是不同问题。先选对应机制，再决定是否组合使用。

## 三种机制

| 机制 | 解决什么 | 不能代替什么 |
| --- | --- | --- |
| `@RateLimit` | 一个窗口允许多少次请求 | 业务只执行一次 |
| `@RepeatSubmit` | 短时间相同参数重复提交 | 断网后重放原成功结果 |
| `@Idempotent` | 按请求键重放已成功响应 | 不同键提交同一业务的约束 |

注解来自 `com.nz.admin.framework.protection.annotation`。当前参数示例：

```java
@RateLimit(permits = 5, windowSeconds = 60)
@RepeatSubmit(intervalSeconds = 1)
```

频率与重复窗口要按实际操作设置，不能为了测试方便关闭生产登录频控。

## 对关键提交要求请求键

请假创建、提交和流程办理使用：

```java
@Idempotent(required = true)
```

前端通过 `Idempotency-Key` 请求头传入一次操作的唯一键。成功后清理键，下次新的操作生成新键；网络失败后重试同一请求时保留原键。相同键不能用于不同参数，框架会校验请求指纹。

```ts
const key = crypto.randomUUID()
await request.post('/api/demo/leave/example-id/submit', undefined, {
  headers: { 'Idempotency-Key': key },
})
```

上例的单据 ID 需替换为本人的真实草稿 ID。网络失败的重试应复用 `key`，不能每次重试重新随机生成。

## 数据库和外部副作用

幂等执行器把数据库修改与成功响应放在同一事务中，失败回滚允许重试。未传键时，`required=false` 的接口按普通调用执行；必须要求键的操作应显式配置 true。

两个不同请求键仍可能指向同一张单据。请假另外使用业务记录与业务键锁防止重复启动。外部邮件、支付或第三方 API 不受本地数据库回滚保护，接收方也要具备幂等能力。

## 单节点和集群

本地频控状态与 Redis 集群共享状态的运行方式不同。多节点要启用 cluster、Redis 和一致前缀，否则每个节点各自计数。业务幂等还依赖共享 PostgreSQL；Redis 开启不能代替数据库事务。

验证同键并发、不同键同业务、失败回滚、成功重试和跨节点重放。参考[测试指南](/guide/testing/backend)与[双节点配置](/operations/cluster)。
