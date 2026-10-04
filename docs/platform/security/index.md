# 认证、授权与数据隔离

一次业务请求要通过四层检查：登录身份、功能权限、租户隔离、记录归属和业务状态。任何一层缺失，都不能靠其他层补足。

| 要解决的问题 | 机制 | 指南 |
| --- | --- | --- |
| 调用者是谁 | 令牌与可信服务端会话 | [登录与会话](/platform/security/authentication) |
| 可以调用哪个操作 | 菜单按钮与 SaCheckPermission | [菜单配置](/platform/system/menus) |
| 只能看到哪个租户 | 请求上下文与表级隔离 | [租户接入](/platform/security/tenant-isolation) |
| 同租户可以操作哪些记录 | 功能上下文与数据范围规则 | [数据范围](/platform/security/data-scope) |
| 当前记录能否执行此动作 | Service 状态与归属检查 | [持久化与事务](/guide/backend/persistence) |

## 一个例子

用户有请假提交权限，不代表可以提交别人的单据；审批人有流程 action 权限，不代表可以办理其他任务；角色有全部数据范围，也只表示当前租户与相应功能范围内的全部。

前端隐藏入口只是交互控制，服务端必须独立验证。直接 JDBC、文件下载、导出和异步回调要逐条确认隔离来源，不默认继承 MyBatis 插件或当前登录上下文。

第三方登录、短信验证码和实时连接各有状态或一次性票据规则，分别见[第三方登录](/social-login)、[短信](/sms-management)、[实时通信](/realtime-communication)。
