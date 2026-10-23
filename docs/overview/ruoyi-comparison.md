# 功能对标与项目特色

本页对照成熟后台的功能分类与 NZ Admin 的实际实现，说明我们选择沉淀的机制及仍需补齐的能力。功能名相似不表示配置、组件或 API 完全兼容。

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

## 从对标转为自己的开发路径

参考 RuoYi-Vue-Plus 的[项目说明](https://github.com/dromara/RuoYi-Vue-Plus/blob/5.X/README.md)和上面的专题目录，按高频业务选择落点：

| 参考方向 | NZ Admin 当前落点 | 使用边界与证据 |
| --- | --- | --- |
| 用户、组织、角色与菜单 | 现成系统模块和套餐/角色交集 | [系统操作](/platform/system/) |
| 认证与第三方账号 | Sa-Token、客户端约束、OAuth2/OIDC | 每个外部 provider 独立验收；[认证](/platform/security/authentication) |
| 租户与数据权限 | 可信租户、功能相关角色并集、读写 SQL 过滤 | JDBC 需要显式隔离；[数据范围](/platform/security/data-scope) |
| 导入与导出 | 预检、错误行、全批事务新增、范围导出 | V31 权限；1000 行导入、5000 行导出；[Excel](/guide/backend/excel) |
| 可视化工作流 | 同版本官方经典设计器、业务 SPI 与可靠事件 | 不开放任意执行器/通用表单；[审批](/platform/workflow/) |
| 文件、通知和实时通信 | 存储配置、可插拔发送与逐用户历史 | Pub/Sub 不代替离线可靠消息；[扩展协议](/developer/extensions) |
| 生成代码与模块 | CRUD 预览、访问规则、模块 CLI | 业务状态与归属仍需领域校验；[模块生命周期](/developer/module-lifecycle) |
| 集群、调度与数据库 | 共享状态和审批两节点验证；当前 PG/Quartz | 动态数据源与分布式调度另行交付 |
| 开发者文档 | 源码驱动目录、结构契约、离线搜索与三级指南 | 静态结构不是线上健康；[开发者中心](/developer/) |

本轮突出三点：**批量操作先解释错误再写入、架构目录可追溯到源码、完成状态有分层验证依据**。后续优先级见[持续推进](/developer/roadmap)，不把规划条目当成已实现能力。
