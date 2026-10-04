# 按症状排查运行问题

先确定故障发生在哪一层，再采取动作。保留请求时间、节点、用户/租户和错误码，避免重启或修改数据后丢失证据。

## 健康检查

| 地址 | 检查什么 |
| --- | --- |
| 前端 `/healthz` | 前端容器可响应 |
| 前端 `/health/ready` | 代理后的后端就绪 |
| 后端 `:8080/actuator/health/readiness` | 应用与数据库、按配置启用的 Redis |

这三个地址没有 `/api` 前缀。请求 `/api/actuator/...` 会经过业务认证或得到不存在结果，不能据此判断后端没启动。

## 启动失败

连接拒绝先查监听与发布端口；认证失败查账号和库名。Flyway 校验失败检查实际历史与文件变更，不随意 repair。租户表审计失败检查表是否受管或有独立隔离登记，不关闭审计掩盖问题。

Compose 环境先运行 delivery check 并看后端日志；不要删除现有数据卷试图清除启动错误。完整说明见[环境准备](/start/environment)与[迁移流程](/guide/database/migrations)。

## 页面缺失或无数据

按模块 → 套餐 → 角色菜单 → 按钮权限 → component → 数据范围 → 归属检查。401 排查会话和令牌，403 排查功能授权与对象参与人。共享 Redis 不可用与无效令牌是不同故障。

菜单与数据排查分别见[菜单接入](/platform/system/menus)和[权限范围](/platform/security/data-scope)。

## 文件与实时连接

文件失败核对原 storageType、当前路径/bucket、凭据密钥和权限，不只检查新配置。SSE/WebSocket 失败先看票据是否过期或已消费，再检查代理缓冲、Upgrade、来源和节点共享配置。

SSE 重连需要新票据，旧票据不能重复使用；在线推送成功不代表离线用户已收到可靠通知。见[文件配置](/file-configuration)与[实时通信](/realtime-communication)。

## 审批状态不同步

先判断引擎任务是否完成，再查业务事件 attempts、next_attempt、last_error 和处理器。运行轨迹与业务状态可能处在短暂同步阶段，不能直接修改业务状态或任务表纠正界面。见[工作流排错](/platform/workflow/troubleshooting)。

## 文档页面 404

检查 DOCS_BASE 是否与部署前缀一致，静态服务器是否支持 clean URL 的 `.html` 回退。应用 SPA 的 index.html 回退与文档静态文章回退不同，见[文档站部署](/documentation-site)。
