# 扩展模块体系

## 分层

- app：启动、环境和 SQL 初始化。
- common：返回、异常、分页和作业公共能力。
- framework：跨业务 starter，禁止依赖业务模块。
- business：领域规则、API 和持久化。
- web：API 边界、页面、状态和测试。

新增业务使用 nz-module-<name>；可复用技术使用 nz-starter-<name>。

## 实际能力

- 已具备：用户、角色、菜单、部门、岗位、字典、参数、公告、日志、在线用户、任务、文件、工作台和基础监控。
- 部分具备：数据权限、限流/防重复提交基础能力、本地/OSS 静态文件配置、缓存 starter、基础监控。
- 缺失：客户端管理、文件配置管理、Redis 指标、demo、租户、字段加密、WebSocket/SSE、邮件短信、代码生成和 AI 助手。

## 实现顺序

1. 客户端管理 v0：clientId、名称、登录类型、token 时长、状态、备注；不改现有登录流程。
2. 文件配置：先只读脱敏展示，再考虑持久化和动态切换；密钥不得返回。
3. Redis 监控：可选 starter，未配置时返回未启用，不能阻止应用启动。
4. 示例模块：包含 CRUD、权限、SQL、测试，且可整体删除。

## 启停、包、菜单和 SQL

- starter 用条件化自动配置；业务模块目前由 Maven 聚合，不承诺运行时热启停。
- 后端按 modules.<module>/{controller,service,mapper,entity,convert} 组织；Entity/DO 不直接返回。
- 前端按 src/api/<module> 与 src/views/<module>/<feature> 组织，页面状态放 hooks.ts。
- 初始 SQL 位于 db/init.sql，已部署升级用 db/upgrade-<version>-*.sql。
- 每项模块变更同步 API、菜单/按钮权限 SQL、测试和文档。

本轮不实现 npm CLI、OAuth/OIDC、分布式任务和完整 AI 助手。

