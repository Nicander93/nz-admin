# 扩展模块体系

## 分层

- app：启动、环境、Flyway 迁移和模块装配。
- common：返回、异常、分页、任务和模块描述协议。
- framework：跨业务 starter，禁止依赖业务模块。
- business：按 `nz-module/<name>` 隔离领域规则、API 和持久化。
- web：API 客户端、模块清单、页面状态和测试。

新增业务使用独立 `nz-module/<name>`；可复用技术使用 `nz-starter-<name>`。

## 当前能力

已完成用户、角色、菜单、部门、岗位、字典、参数、公告、日志、在线用户、任务、文件、工作台、客户端管理 v0、文件配置、可选 Redis 指标、邮件、SSE/WebSocket、可删除 demo 模块、独立代码生成模块，以及工作流分类、定义版本和发布状态机。

短信与第三方登录已经落地。后续主要缺口是工作流实例与任务审批，动态数据源和分布式增强属于后续扩展。代码生成与模板已经落地；根目录 CLI 已提供模块脚手架、Maven 接入、迁移检查、生成器下载和回滚。

## 模块约定

- 每个后端业务模块提供 `META-INF/nz/module.yaml` 和 Spring Boot 自动装配入口。
- 模块开关使用 `nz.modules.<code>.enabled`，修改后重启生效，不支持热卸载。
- 业务模块不得被 framework 反向依赖；可删除模块也不能被其他业务模块直接依赖。
- 前端模块在 `src/modules/<code>/manifest.ts` 声明，注册表自动发现清单。
- 后端按 `controller/service/mapper/entity/convert` 分层，DO 不直接返回。
- 每个纵向切片同时提交 API、页面、菜单与按钮权限、Flyway 迁移、测试和文档。

demo 模块的启停、删除和数据库处理见 [demo-module.md](demo-module.md)。代码生成器的接口、模板和限制见 [code-generator.md](code-generator.md)。
