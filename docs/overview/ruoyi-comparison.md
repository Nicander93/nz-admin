# 与 RuoYi-Vue-Plus 文档的对照

本页说明借鉴的阅读结构与 nz-admin 的实际落点。它是文档与能力边界的对照，不表示两个项目的功能、组件或接口完全兼容。

## 参考了哪些目录

核对 RuoYi-Plus 官方文档仓库的 [5.X 导航](https://github.com/dromara/plus-doc/blob/master/5.X/ruoyi-vue-plus/_sidebar.md)和 [6.X 导航](https://github.com/dromara/plus-doc/blob/master/6.X/ruoyi-vue-plus/_sidebar.md)。它把快速开始、框架基础、扩展和说明分开，具体问题对应独立页面。

| 参考目录关注点 | 本站阅读入口 |
| --- | --- |
| 初始化与部署 | [环境准备](/start/environment)、[配置](/start/configuration)、[运维](/operations/) |
| 新模块与接口 | [模块开发](/module-development-guide)、[后端开发](/guide/backend/) |
| 数据与权限 | [表设计](/guide/database/)、[权限与隔离](/platform/security/) |
| 扩展机制 | [接口保护](/guide/backend/protection)、存储与通知专题 |
| 测试说明 | [测试分层](/guide/testing/) |

新文章依据 nz-admin 源码独立编写，使用本项目的包名、字段、API 和配置。版本升级时优先核对本站实现，不把 RuoYi 配置直接复制过来。

## 本项目已有实现

Vue3/TypeScript、Java 17/Spring Boot、PostgreSQL/Flyway、角色菜单按钮、租户与数据范围、CRUD 生成、文件、通知和实时连接。新工作流复用 Warm-Flow 1.8.9 官方经典 UI，业务中心和回调适配本项目协议。开发和交付验证见[能力总览](/capabilities)。

## 仍有边界的领域

| 领域 | nz-admin 当前边界 | 选择或接入前需要做什么 |
| --- | --- | --- |
| 数据库 | 当前主数据源为 PostgreSQL | 不宣称任意数据库或动态数据源可直接替换 |
| 可视化审批 | 官方经典模型与受控字段 | 仿钉钉、通用动态表单另行设计与验证 |
| 任务调度 | 当前 Quartz 任务模块 | 分布式调度与全局只执行一次单独验收 |
| 身份供应商 | OAuth2/OIDC 扩展、配置 provider | 真实供应商账号与回调单独验收 |
| 实时通知 | Redis 在线转发、数据库消息历史 | 离线可靠队列不由 Pub/Sub 提供 |
| 外部扩展 | 未集成的 AI/MCP、链路监控等不承诺支持 | 按真实需求选组件，不为目录齐全添加空功能 |

下一步按[开发路线](/developer-guide)进入具体指南。遇到尚未实现的能力，先明确业务需求和集成边界，再扩展框架模块；文档不会把路线图当成交付结果。
