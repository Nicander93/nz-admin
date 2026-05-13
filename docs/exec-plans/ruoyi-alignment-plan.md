# RuoYi 对标实施计划

## 目标

将 `nz-admin` 打磨成一个对标 RuoYi 核心能力、但更简洁优雅的前后端分离脚手架。

项目不追求完整复制 RuoYi，而是保留后台管理系统最常用、最能支撑二次开发的能力：

- 开箱即用：拉取项目后能初始化数据、启动后端、启动前端、登录系统。
- 模块清晰：后端 starter、common、module、app 分层清楚，前端页面、API、hooks 有固定范式。
- 权限闭环：用户、角色、菜单、按钮权限、数据权限、动态路由能稳定串起来。
- 开发高效：新增业务模块时，有统一 CRUD 写法和轻量代码生成能力。
- 文档可靠：编码正常，README 和 docs 能指导后续开发。

## 当前进度判断

- 对标 RuoYi 完整功能：约 60%。
- 作为轻量优雅脚手架：约 70%。

已具备的基础：

- 后端 Maven 多模块结构：`nz-common`、`nz-framework`、`nz-module`、`nz-app`。
- 现代技术栈：Spring Boot 3.3、Java 17、MyBatis-Plus、Sa-Token、Hutool、Springdoc。
- 系统基础模块：用户、角色、菜单、部门、岗位、字典、参数、通知、文件、操作日志、登录日志、在线用户、定时任务。
- 权限链路：登录、用户信息、角色权限、菜单路由、按钮权限、Sa-Token 权限注解、前端 `v-permission`。
- 前端页面：Vue3、Element Plus、Pinia、动态路由、hooks 风格 CRUD 页面。
- 测试基础：后端和前端已有部分单元测试、E2E 测试。

主要差距：

- 初始化 SQL 只有建表，缺少管理员、角色、菜单、权限、字典等种子数据。
- 项目还没有形成完整的开箱即用启动链路。
- 定时任务还不是完整在线调度体系。
- 代码生成器尚未实现。
- 监控、健康检查、接口文档入口、运行状态页还不完整。
- README、docs、部分中文注释和提示存在编码显示问题。

## P0：开箱即用闭环

目标：让项目能被新开发者拉下来后顺利启动、登录、看到菜单、完成基础 CRUD。

- [x] 补齐 `init.sql` 种子数据。
  - [x] 新增默认管理员用户。
  - [x] 新增超级管理员角色。
  - [x] 新增用户、角色、菜单、部门、岗位、字典、参数、通知、日志、在线用户、定时任务、文件等菜单。
  - [x] 新增按钮权限标识。
  - [x] 新增基础字典和系统参数。
- [x] 跑通登录闭环。
  - [x] 前端登录成功后能获取用户信息。
  - [x] 前端能加载动态菜单。
  - [x] 根路由能跳转到第一个可访问页面。
- [x] 跑通核心 CRUD 闭环。
  - [x] 用户管理可查询、新增、编辑、删除。
  - [x] 角色管理可授权菜单。
  - [x] 菜单管理可维护目录、菜单、按钮。
  - [x] 部门、岗位、字典、参数、通知可正常维护。
- [x] 修复中文编码问题。
  - [x] 修复 `readme.md`。
  - [x] 修复 `docs/convert.md`。
  - [x] 检查后端注释和错误提示。
  - [x] 检查前端错误提示。
- [x] 完善环境配置。
  - [x] `application.yml` 只保留公共配置。
  - [x] `application-dev.yml` 放开发默认值。
  - [x] `application-prod.yml` 去掉本地默认密码，改为环境变量或占位配置。
- [x] 补一份快速启动文档。
  - [x] 后端启动步骤。
  - [x] 前端启动步骤。
  - [x] 数据库初始化步骤。
  - [x] 默认账号说明。

本轮结论：P0 已补齐开箱即用数据、运行时幂等初始化、生产环境变量配置和 README 快速启动说明。后端 `./mvnw clean compile -DskipTests` 已通过；前端构建因当前机器 `node_modules` 为 WSL 软链、Windows Node 无法解析依赖且 WSL 缺少原生 Node，未完成本轮构建验收。

## P1：RuoYi 核心体验补齐

本轮进度（节选）：用户管理已对齐重置密码（默认密码取自 `sys.user.initPassword`）、岗位多选（`sys_user_post`）、详情含 `postIds`；角色菜单分配成功后刷新 Pinia 权限与菜单列表；`init.sql` 增补 `system:user:resetPwd` 按钮种子。菜单管理：树表展示路径/组件/状态，按 `sort` 稳定排序，展开折叠不再误刷接口，删除菜单级联子节点并清理 `sys_role_menu`，按钮走 `system:menu:*` 权限。日志体系：`@Log` 切面改为只捕获业务 `Exception`、请求参数脱敏（password/pwd）、结果 JSON 序列化；登录失败写 `sys_login_log`；`GlobalExceptionHandler` 通过 `ApiExceptionLogRecorder` 落库接口异常栈；操作/登录日志支持按 id 批量删与按天清理（`DELETE .../clean?days=`）。P1 补充：修复 `QuartzJobService` 重复类体与双实现 `JobService` 冲突；`SpringBeanJobFactory` 注入 Quartz Job；暂停/恢复与库状态一致（`scheduleJobIfActive`）；`DisallowConcurrentQuartzJob` 对应禁止并发；Cron 校验接口 `GET /api/system/job/cron-valid`；文件上传大小走 `nz.file.max-file-size-bytes` 与 `spring.servlet.multipart`；排除 Spring Boot 自带 Quartz 自动配置避免双调度器；README 写明 Springdoc 与文件存储配置。

目标：补齐后台管理系统最常用的稳定能力，让系统模块能支撑真实二次开发。

- [x] 完善用户管理。
  - [x] 重置密码。
  - [x] 分配角色。
  - [x] 部门关联。
  - [x] 岗位关联。
  - [x] 状态启停。
  - [x] 用户详情出参避免泄漏密码等敏感字段。
- [x] 完善角色管理。
   - [x] 菜单授权。
   - [x] 数据范围设置。
   - [x] 角色状态启停。
   - [x] 权限更新后刷新当前用户权限。
- [x] 完善菜单管理。
  - [x] 明确目录、菜单、按钮三类类型。
  - [x] 支持组件路径。
  - [x] 支持图标。
  - [x] 支持显示/隐藏。
  - [x] 支持排序。
  - [x] 支持权限标识维护。
- [x] 完善日志体系。
  - [x] 操作日志和 `@Log` 注解稳定闭环。
  - [x] 登录日志记录成功和失败。
  - [x] 异常日志或接口异常记录。
  - [x] 日志清理功能。
- [x] 完善定时任务。
  - [x] 应用启动时加载启用任务。
  - [x] 新增或修改任务时注册/更新调度。
  - [x] 暂停和恢复真正影响调度状态。
  - [x] 手动执行记录执行日志。
  - [x] 支持 cron 校验。
  - [x] 支持并发策略和错过执行策略。
- [x] 完善文件服务。
  - [x] 上传大小限制。
  - [x] 文件类型校验。
  - [x] 路径安全校验。
  - [x] 本地存储和 OSS 存储配置说明。
- [x] 完善接口文档。
  - [x] Springdoc 入口可访问。
  - [x] Controller 方法补充必要注解。
  - [x] README 中说明接口文档地址。

## P2：脚手架差异化打磨

目标：形成比 RuoYi 更轻、更现代、更容易二次开发的脚手架体验。子阶段索引见 [docs/p2-phase-overview.md](../p2-phase-overview.md)。

### P2.0 规范对齐与基线

- [x] 对齐 `.docs/architecture.md`（Java 17、entity 分包描述与代码一致）。
- [x] 对齐 `.docs/naming-convention-service-dao-and-model.md`（`dataobject` 目录）。
- [x] 补充 `docs/p2-phase-overview.md` 作为 P2 文档入口。

### P2.1 统一 CRUD 范式

- [x] 后端分页与 `R` / `Page` 约定文档化（见 `docs/crud-paradigm.md`）。
- [x] 前端 hooks `table` / `form` / `actions` 与只读页例外说明。
- [x] 选定代表页面：通知/角色（分页）、菜单/部门（树表）、用户（复杂）、文件（只读）。
- [x] `nz-web/src/api/system/index.ts` 补全 `dict` barrel 导出，减少 API 导入分叉。

### P2.2 模块开发指南

- [x] `docs/module-development-guide.md`：后端步骤、前端步骤、权限与菜单、测试引用、UI 引用。

### P2.3 轻量工作台

- [x] 后端聚合接口 `GET /api/system/workbench/snapshot`（最近登录/操作日志各 5 条）。
- [x] 前端 `views/workbench/`，快捷入口 + 双列表；菜单种子 `工作台`（`system:workbench:view`）。

### P2.4 基础监控

- [x] 后端 `GET /api/system/monitor/summary`：数据库连通性、JVM 堆与运行时间、CPU 逻辑核数。
- [x] 前端 `views/system/monitor/`；菜单「运行状态」挂在「系统监控」下（`system:monitor:query`）。
- [ ] 缓存状态（待引入 Redis 后再做）。

### P2.5 测试与 CI

- [x] 后端 `WorkbenchServiceImpl` 单元测试（快照组装）。
- [x] 前端 `useWorkbenchHome` 单测。
- [x] E2E：登录后进入工作台可见关键区块。
- [x] GitHub Actions：`nz-server` 编译、`nz-web` pnpm install + test + build。

**本轮结论**：P2.0–P2.5 已落地（Redis 缓存监控与代码生成器仍按上文延后）。新增 `GET /api/system/workbench/snapshot`、`GET /api/system/monitor/summary`、前端工作台与运行状态页、`docs/crud-paradigm.md` / `docs/module-development-guide.md`、`.github/workflows/ci.yml`；旧库可执行 `nz-app/src/main/resources/db/upgrade-p2-menus.sql` 补菜单与权限。

### 延后：轻量代码生成器

- [ ] 读取数据库表结构。
- [ ] 生成后端 DO、Query、Mapper、Service、Controller。
- [ ] 生成前端 API、hooks、列表页、表单弹窗。
- [ ] 生成菜单和按钮权限 SQL。
- [ ] 支持预览和下载。

## 推荐实施顺序

1. 修复编码和文档，避免后续继续污染中文内容。
2. 补初始化种子数据，先保证能登录和看到完整菜单。
3. 跑通用户、角色、菜单三件套，这是权限系统核心。
4. 补齐部门、岗位、字典、参数、通知等基础 CRUD 的一致性。
5. 完善日志和定时任务。
6. 补快速启动、模块开发、权限开发文档。
7. 做轻量代码生成器。
8. 做监控、首页、CI 和更多测试。

## 每轮实现后的验收建议

- 后端至少执行一次 `./mvnw clean compile -DskipTests`。
- 前端至少执行一次 `pnpm test` 和 `pnpm build`。
- 涉及登录、菜单、权限、核心页面时，补一次 E2E 或手动联调记录。
- 每完成一个阶段，在本文档中勾选对应事项，并补充关键结论。
