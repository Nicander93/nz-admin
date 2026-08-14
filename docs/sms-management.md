# 短信管理

短信管理提供渠道、模板、发送记录和测试发送能力。框架层只处理供应商调用，业务层负责租户数据、权限、模板渲染和发送日志。

## 能力边界

- `nz-starter-sms` 定义 `SmsProvider` 扩展点和统一 `SmsGateway`，不依赖 `nz-system`。
- `nz-system` 保存渠道、模板和发送记录，并提供管理接口与页面。
- 内置 `log` 渠道只记录发送元数据，适合本地开发，不会调用外部短信服务。
- 内置 `webhook` 渠道向配置的地址发送标准 JSON，可用于对接企业网关或供应商适配服务。
- 业务可以注册新的 `SmsProvider` Bean，并用其 `code()` 作为渠道的供应商编码。

## 配置

```yaml
nz:
  sms:
    enabled: true
    logging-provider-enabled: true
    webhook-connect-timeout: 3s
    verification:
      enabled: true
      template-code: verification-code
      code-length: 6
      max-attempts: 5
      ttl: 5m
      resend-interval: 60s
    webhook-read-timeout: 5s
```

`NZ_SMS_VERIFICATION_FIXED_CODE` 只用于本地联调，例如设置为 `123456`。未设置时验证码随机生成；生产环境不得设置固定验证码。其余验证码配置也可用同名大写环境变量覆盖。

`NZ_SMS_ENABLED=false` 时短信网关不会装配，但账号密码登录仍可正常启动。此时调用验证码接口会返回明确的未启用提示。

生产环境建议开启 `nz.field-encryption.enabled` 并配置密钥。开启后，渠道密钥和发送记录中的手机号会加密保存；接口不会返回渠道密钥明文，手机号只返回脱敏结果。

## 渠道

渠道编码在租户内唯一。删除仍被模板引用的渠道会被拒绝。

- `log`：无需地址和密钥，返回本地消息 ID。
- `webhook`：必须配置 `endpoint`。请求头中的 `X-Access-Key` 来自 access key，`Authorization: Bearer ...` 来自 secret。

Webhook 请求体：

```json
{
  "channelCode": "company-gateway",
  "phoneNumber": "13800138000",
  "templateCode": "verification-code",
  "content": "您的验证码是 123456，5 分钟内有效。",
  "parameters": { "code": "123456" },
  "signature": "NZ Admin"
}
```

网关可以通过响应头 `X-Message-Id` 返回供应商消息 ID。未返回时，系统会生成本地 ID。

## 模板与发送

模板变量使用 `{{name}}`。测试发送时必须提供所有变量，缺失变量会在调用供应商前被拒绝。

发送记录先以 `PENDING` 保存，再更新为 `SUCCESS` 或 `FAILED`。供应商异常会保留最多 500 个字符的错误信息，同时向调用方返回业务错误。

管理接口统一位于 `/api/system/sms`：

| 接口 | 权限 | 用途 |
| --- | --- | --- |
| `GET /channels/page`、`GET /templates/page`、`GET /logs/page` | `system:sms:list` | 分页查询 |
| `GET /channels/{id}`、`GET /templates/{id}` | `system:sms:query` | 查看详情 |
| `POST /channels`、`POST /templates` | `system:sms:add` | 新增 |
| `PUT /channels`、`PUT /templates` | `system:sms:edit` | 修改 |
| `DELETE /channels/{id}`、`DELETE /templates/{id}` | `system:sms:remove` | 删除 |
| `POST /send-test` | `system:sms:send` | 测试发送 |

## 短信验证码登录

登录页提供账号密码和短信验证码两种模式。两种模式分别绑定客户端：

- `nz-web-account` 只允许 `account` 登录；
- `nz-web-sms` 只允许 `sms` 登录；
- 禁用客户端、混用登录类型或设置非法 token 超时时间都会在签发令牌前被拒绝。

公开认证接口为：

| 接口 | 用途 |
| --- | --- |
| `POST /api/auth/sms/code` | 发送登录验证码 |
| `POST /api/auth/sms/login` | 验证手机号和验证码并签发令牌 |

验证码按租户和手机号隔离，只保存摘要，默认 5 分钟有效、60 秒内不可重发、最多尝试 5 次，验证成功后立即消费。不存在或被禁用的手机号采用相同的发送成功响应，避免泄露账号是否存在。供应商发送失败时会立即作废本次验证码。

当前验证码状态保存在单节点 JVM 内存中，应用重启后失效，也不支持多节点共享。部署多个实例前应把 `SmsVerificationCodeStore` 替换为 Redis 实现，并保持原子发送频率和尝试次数语义。

认证接口是登录前公开入口，因此不配置菜单和按钮权限；它的授权边界由租户、客户端状态和 `login_type` 共同控制。客户端管理菜单可用于启停对应登录方式。

## 数据库升级

新部署由 Flyway 自动执行 `V14__sms_management.sql`。人工升级脚本是 `nz-server/nz-app/src/main/resources/db/upgrade-p14-sms-management.sql`。迁移会创建三张短信表、默认日志渠道、验证码模板、菜单和按钮权限。

短信登录由 `V15__sms_login.sql` 增加手机号检索摘要和两个登录客户端；对应人工脚本是 `upgrade-p15-sms-login.sql`。历史用户第一次按手机号查询时会校验解密后的手机号并回填摘要。
