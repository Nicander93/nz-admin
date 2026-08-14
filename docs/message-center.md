# 站内消息中心

站内消息中心按租户保存逐用户消息，支持管理员发送、当前用户收件箱、未读数量、查看后自动已读、全部已读和删除。顶部导航每 60 秒刷新未读角标，页面操作后会立即触发刷新；启用实时通信时，发送成功还会向在线接收人发布 `system-message` 事件。

## 数据与权限

`sys_message` 每一行只属于一个租户和一个接收用户。MyBatis 租户拦截器负责租户行级隔离，业务服务继续校验当前登录用户必须等于消息接收人，避免通过消息 ID 越权读取或删除。

权限如下：

- `system:message:list`：收件箱与未读数量。
- `system:message:query`：查看本人消息详情。
- `system:message:read`：标记本人消息已读。
- `system:message:remove`：删除本人消息。
- `system:message:send`：向当前租户的启用用户发送消息。

管理员角色由 V17 自动获得上述菜单和按钮权限。普通角色需要在角色管理中显式授权。

## 接口

- `GET /api/system/message/page`
- `GET /api/system/message/{messageId}`
- `GET /api/system/message/unread-count`
- `PUT /api/system/message/{messageId}/read`
- `PUT /api/system/message/read-all`
- `DELETE /api/system/message/{messageId}`
- `POST /api/system/message/send`

发送范围支持当前租户全部启用用户或最多 500 个指定用户。指定用户中只要存在禁用、不存在或不属于当前租户的账号，整次发送就会失败。跳转地址只允许以单个 `/` 开头的站内路径，扩展数据必须是有效 JSON。

消息内容在前端按纯文本展示，不支持 HTML。这样可以直接阻断通过消息正文注入脚本；需要富文本时应增加独立的清洗和内容安全策略，不能直接改用 `v-html`。

## 迁移与运行边界

新库通过 `V17__message_center.sql` 建表和初始化权限；人工升级脚本是 `db/upgrade-p17-message-center.sql`。消息行采用逐接收人存储，因此已读和删除互不影响。

当前版本没有自动归档或保留期任务。数据规模增长后，应按业务合规要求新增前向 Flyway 迁移和独立清理任务。实时推送仅用于提升到达速度，收件箱数据库始终是事实来源；推送失败不会回滚已经提交的消息。
