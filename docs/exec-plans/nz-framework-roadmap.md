# nz-framework 完善执行计划

## 目标

将 `nz-server/nz-framework` 从“已有若干 starter 的薄框架层”，完善为一个简单、优雅、可复用的后台管理脚手架底座。

本计划对标 `ruoyi-vue-pro/yudao-framework` 的分层思路，但不照搬其大而全能力。当前项目优先服务单体管理后台，框架层重点提供 Web、认证、数据访问、日志、文件、定时任务、测试、缓存、保护、Excel、监控等高频基础能力。

## 设计原则

- 框架层只做通用机制，不绑定 `nz-system` 业务表和业务流程。
- 业务模块通过接口实现框架扩展点，`nz-framework` 不反向依赖 `nz-module`。
- 每个 starter 都应具备自动配置、配置属性、可覆盖 Bean、扩展接口、基础测试。
- 优先做最小可用能力，不为了对标而引入多租户、MQ、工作流、商城、支付等重型能力。
- 新增配置优先提供安全默认值，并允许通过 `application.yml` 覆盖。
- 公共协议和轻量对象放 `nz-common`，运行时自动配置和框架机制放 `nz-framework`。

## 当前基础

已有 starter：

- `nz-starter-web`：全局异常、CORS、Springdoc 分组。
- `nz-starter-auth`：Sa-Token 自动配置、自定义权限注解和异常处理。
- `nz-starter-datascope`：数据权限注解、切面和用户解析扩展点。
- `nz-starter-mybatis`：MyBatis-Plus 分页插件、字段自动填充、查询包装器。
- `nz-starter-log`：操作日志注解和业务类型枚举。
- `nz-starter-file`：文件存储接口、本地存储、OSS 存储和配置。
- `nz-starter-quartz`：Quartz 自动配置和任务基类。
- `nz-starter-test`：单元测试、数据库测试和随机对象工具。

主要短板：

- starter 规范还不统一，部分模块缺少配置属性和可覆盖扩展点。
- Web、MyBatis、日志等核心 starter 仍有硬编码或职责边界不清的问题。
- 操作日志切面等通用机制仍在 `nz-system`，没有完全沉淀到框架层。
- 缺少后台脚手架常用的缓存、防重复提交、限流、Excel、轻量监控等能力。
- 框架能力缺少统一文档、示例和验收测试保护。

## P0：框架边界和规范

目标：先统一 `nz-framework` 的模块边界、starter 约定和文档入口，避免后续越改越散。

- [ ] 更新 `docs/architecture.md`，补充 `nz-framework` starter 设计规范。
- [ ] 检查所有 `nz-starter-*` 的 `pom.xml` 描述和 `package-info.java`。
- [ ] 统一 starter 包结构：`config`、`core`、`annotation`、`aspect`、`properties`、`handler`、`support` 按需使用。
- [ ] 统一自动配置命名和注册方式，继续使用 `AutoConfiguration.imports`。
- [ ] 明确每个 starter 的业务扩展接口，接口放 framework，实现放 module。
- [ ] 梳理 `nz-common` 与 `nz-framework` 重叠对象，避免 `BaseEntity`、`BaseDO` 等概念重复扩大。
- [ ] 给新增或改造 starter 补最小测试，接入现有测试计划。

验收标准：

- 每个 starter 的职责能从 POM、包说明和自动配置类快速判断。
- 不新增 `nz-framework -> nz-module` 依赖。
- 新增公共能力有清晰配置项或扩展点。

## P1：Web Starter 加强

目标：把 Web 层打造成统一 HTTP 入口，提供稳定的异常、文档、跨域、请求上下文能力。

- [ ] 新增或完善业务异常模型：`BusinessException`、错误码接口、默认错误码。
- [ ] 全局异常处理区分参数异常、业务异常、认证异常、未知异常。
- [ ] 未知异常默认返回通用提示，避免直接暴露内部异常信息。
- [ ] CORS 改为 `nz.web.cors.*` 配置项，支持开关、允许源、方法、请求头、凭证。
- [ ] Springdoc 配置改为 `nz.web.swagger.*`，支持标题、版本、分组、开关。
- [ ] 增加请求 TraceId / RequestId 过滤器，写入 MDC 和响应头。
- [ ] 增加 `WebFrameworkProperties`，统一管理 Web 相关项目配置。
- [ ] 补充全局异常和自动配置测试。

验收标准：

- Controller 无需重复处理常见异常。
- 生产环境未知异常响应不泄漏堆栈和内部类名。
- CORS、Swagger、TraceId 可通过配置开启或关闭。

## P2：Auth Starter 加强

目标：框架层只封装认证授权机制，不绑定系统用户、角色、菜单表。

- [ ] 定义轻量登录用户模型或上下文对象，例如 `LoginUser`。
- [ ] 定义 `AuthUserResolver` / `PermissionResolver` 扩展接口，由 `nz-system` 实现。
- [ ] 封装登录用户工具类，统一获取用户 ID、用户名、权限、角色。
- [ ] Sa-Token 拦截路径、排除路径、Token 策略配置化。
- [ ] 权限注解行为和 Sa-Token 原生注解关系写入文档。
- [ ] 权限异常返回统一接入 Web 异常模型。
- [ ] 补充权限注解、匿名路径、上下文清理测试。

验收标准：

- `nz-framework` 不依赖用户表、角色表、菜单表。
- 业务模块可以替换登录用户解析逻辑。
- 权限失败响应格式与全局响应一致。

## P3：MyBatis Starter 加强

目标：统一数据库访问体验，减少业务模块重复写分页、条件查询和字段填充。

- [ ] 数据库类型从配置读取，不硬编码 PostgreSQL。
- [ ] 定义统一分页结果，例如 `PageResult<T>`。
- [ ] 增强 `LambdaQueryWrapperX` 常用方法：`eqIfPresent`、`likeIfPresent`、`betweenIfPresent`、`inIfPresent`、排序辅助。
- [ ] 新增 `BaseMapperX`，封装通用分页查询和列表查询。
- [ ] 梳理 `BaseEntity` 与 `BaseDO` 边界，确定项目只推荐一种持久化基类。
- [ ] 明确创建人、更新人、创建时间、更新时间、逻辑删除字段规范。
- [ ] 数据权限 SQL 注入点增加字段白名单或受控构造，避免任意 SQL 拼接扩大风险。
- [ ] 补充分页、Wrapper、字段填充、自动配置测试。

验收标准：

- 新增业务 CRUD 不需要直接暴露 MyBatis-Plus `Page` 给前端。
- 常用查询条件能通过统一 Wrapper 表达。
- 数据库类型可配置，后续切换 MySQL/PostgreSQL 不需要改 Java 代码。

## P4：Log Starter 重构

目标：让操作日志注解、切面和采集逻辑属于框架，落库实现由业务模块提供。

- [ ] 将通用操作日志切面从 `nz-system` 下沉到 `nz-starter-log`。
- [ ] 定义 `OperationLogRecorder` 接口，负责接收框架采集后的日志事件。
- [ ] `nz-system` 实现 `OperationLogRecorder`，继续保存到操作日志表。
- [ ] 日志采集字段统一：模块、业务类型、方法、URL、IP、用户、请求参数、返回结果、耗时、异常。
- [ ] 增加敏感字段脱敏配置，例如 `password`、`token`、`secret`。
- [ ] 增加是否记录请求参数、是否记录响应结果的注解开关。
- [ ] 补充成功、失败、脱敏、无 recorder 时的测试。

验收标准：

- `@Log` 注解不依赖 `nz-system` 切面也能生效。
- 没有日志落库实现时，业务接口不受影响。
- 敏感字段不会进入操作日志。

## P5：Redis / Cache Starter

目标：提供后台系统高频缓存能力，但不强制项目依赖 Redis 才能启动。

- [ ] 新增 `nz-starter-redis` 或 `nz-starter-cache`。
- [ ] 提供 RedisTemplate / StringRedisTemplate 默认序列化配置。
- [ ] 提供 Spring Cache 默认配置和 key 前缀配置。
- [ ] 提供缓存工具接口，避免业务散落操作 Redis key。
- [ ] 允许 Redis starter 按依赖引入启用，没有引入时不影响应用启动。
- [ ] 为字典、参数、权限缓存预留接入方式。
- [ ] 补充序列化、CacheManager、条件装配测试。

验收标准：

- 引入 starter 后可直接使用 Spring Cache。
- 未引入 Redis starter 时，现有功能不被破坏。
- 缓存 key 命名有统一前缀和规范。

## P6：Protection Starter

目标：补齐管理后台常用保护能力，先做高频、轻量、可理解的功能。

- [ ] 新增 `nz-starter-protection`。
- [ ] 实现防重复提交注解 `@RepeatSubmit`。
- [ ] 实现接口限流注解 `@RateLimit`，优先支持本地模式，Redis 模式后置。
- [ ] 实现简单 XSS 防护或输入清理能力，避免误伤富文本。
- [ ] 保护类异常接入统一错误码和全局异常。
- [ ] 补充重复提交、限流命中、限流释放、XSS 边界测试。

验收标准：

- 登录、提交表单、删除等接口可按需启用防重复提交。
- 限流策略对业务透明，触发后返回统一响应。
- XSS 处理默认谨慎，不全局破坏合法 HTML 场景。

## P7：Excel Starter

目标：补齐后台管理导入导出基础能力，降低业务列表导出的重复代码。

- [ ] 新增 `nz-starter-excel`，优先基于 EasyExcel。
- [ ] 提供统一导出响应工具。
- [ ] 提供导入模板下载工具。
- [ ] 提供导入错误行收集和响应结构。
- [ ] 支持字典值转换扩展接口。
- [ ] 选择用户、角色或日志列表作为首个接入样例。
- [ ] 补充导出文件名、表头、字典转换、导入错误测试。

验收标准：

- 新增导出接口不需要重复写响应头和流处理。
- 导入失败能返回明确行号和错误原因。
- 字典转换不依赖具体业务表。

## P8：Monitor Starter

目标：提供轻量运行状态能力，服务后台首页和部署排查，不做复杂监控平台。

- [ ] 新增 `nz-starter-monitor` 或在 Web starter 中按需扩展 Actuator 配置。
- [ ] 接入 Spring Boot Actuator 健康检查。
- [ ] 提供 JVM、内存、磁盘、数据库连接状态读取能力。
- [ ] 暴露给后台工作台的状态对象保持轻量。
- [ ] 监控端点路径和暴露范围配置化。
- [ ] 补充健康检查和状态读取测试。

验收标准：

- 后台首页可展示基础运行状态。
- 部署时可通过健康检查判断服务可用性。
- 默认不暴露敏感环境变量和配置详情。

## P9：File Starter 加强

目标：让文件存储更安全、更可配置，业务模块只处理文件业务记录。

- [ ] 文件大小、扩展名、MIME 白名单配置化。
- [ ] 本地存储路径增加路径穿越防护。
- [ ] 文件名标准化，避免特殊字符和目录分隔符污染路径。
- [ ] 统一文件访问 URL 生成逻辑。
- [ ] OSS 配置补充 endpoint、bucket、访问域名、路径前缀等说明。
- [ ] 可选支持临时访问 URL，作为后续增强。
- [ ] 补充大小限制、类型校验、路径安全测试。

验收标准：

- 上传非法路径、非法类型、超大文件时返回统一错误。
- 本地和 OSS 存储通过同一接口使用。
- 业务模块不直接依赖 OSS SDK。

## P10：Quartz Starter 加强

目标：把定时任务调度机制沉淀到框架层，业务模块只维护任务定义和执行记录。

- [ ] 统一 Quartz JobDataMap 字段规范。
- [ ] 下沉任务注册、更新、暂停、恢复、删除工具类。
- [ ] 定义任务执行日志 recorder 接口，由业务模块实现。
- [ ] 支持 cron 校验工具。
- [ ] 支持并发策略和错过执行策略配置。
- [ ] 补充集群模式配置说明，但不默认强制开启。
- [ ] 补充注册、暂停、恢复、手动执行、异常记录测试。

验收标准：

- 任务调度 API 不散落在业务 Service 中。
- 任务执行日志保存失败不影响调度器稳定性。
- 应用重启后启用任务能按配置恢复调度。

## P11：Test Starter 加强

目标：让框架和业务模块新增测试更低成本。

- [ ] 完善 Controller 测试基类。
- [ ] 完善 Service Mockito 测试基类。
- [ ] 完善 MyBatis H2 测试基类，必要时补 Testcontainers 方案文档。
- [ ] 增加 Sa-Token / 登录用户上下文 mock 工具。
- [ ] 增加 RequestContextHolder / MDC 清理工具。
- [ ] 增加常用随机对象、集合、分页测试工具。
- [ ] 将稳定测试范式同步到 `docs/exec-plans/common-framework-test-plan.md`。

验收标准：

- 新增 starter 能快速写自动配置和核心行为测试。
- 测试不依赖真实 MySQL、Redis、OSS、外部网络。
- 上下文类测试后能清理状态，避免污染其他用例。

## 不做事项

当前阶段不纳入 `nz-framework`：

- 多租户。
- 微服务治理。
- MQ 消息队列。
- WebSocket。
- 工作流。
- 支付、商城、CRM、ERP 等业务领域能力。
- 复杂熔断、分布式事务、分布式幂等框架。
- 重型监控链路，例如 SkyWalking、Prometheus 全套接入。

这些能力如果后续确有业务需求，应作为单独专项计划评估，不默认进入脚手架底座。

## 推荐实施顺序

1. P0：先固化框架边界和 starter 规范。
2. P1 + P3：优先增强 Web 和 MyBatis，这是所有业务模块的基础。
3. P4：重构操作日志，让注解、切面、采集逻辑完成框架化。
4. P2：完善认证上下文和权限扩展点。
5. P5 + P6：补缓存和接口保护能力。
6. P7 + P8：补 Excel 和轻量监控，提升后台管理体验。
7. P9 + P10：增强文件和定时任务，减少业务层调度和存储细节。
8. P11：持续完善测试支撑，并反向约束新增框架能力。

## 每阶段验收命令

后端编译：

```bash
cd nz-server
./mvnw clean compile -DskipTests
```

框架模块测试：

```bash
cd nz-server
./mvnw test -pl nz-framework -am
```

按 starter 测试：

```bash
cd nz-server
./mvnw test -pl nz-framework/nz-starter-web -am
./mvnw test -pl nz-framework/nz-starter-mybatis -am
./mvnw test -pl nz-framework/nz-starter-log -am
```

全量后端测试：

```bash
cd nz-server
./mvnw test
```

## 执行记录

后续每完成一个阶段，在这里补充：

- 完成日期。
- 改动模块。
- 关键设计结论。
- 验收命令和结果。
- 遗留问题。

### P0

- 完成日期：2026-05-13
- 改动模块：`docs/architecture.md`、`nz-framework/nz-starter-log`、`nz-framework/nz-starter-quartz`
- 关键设计结论：
  - 在 `architecture.md` 中补齐了 `nz-framework` starter 设计规范，统一了 POM 描述、根包说明、自动配置注册、扩展点位置和公共对象边界。
  - 为 `nz-starter-log` 增加了自动配置入口和 `AutoConfiguration.imports`，后续日志切面与 recorder 可以直接按统一规范继续下沉。
  - 为 `nz-starter-quartz` 补齐了根包说明和自动配置注册文件，和其它 starter 保持同一装配方式。
- 验收命令和结果：
  - `cd nz-server && ./mvnw -pl nz-framework -am compile -DskipTests`：通过。
  - `cd nz-server && export JAVA_HOME=\"$HOME/.jdks/jdk-17.0.2\" && export PATH=\"$JAVA_HOME/bin:$PATH\" && ./mvnw -pl nz-framework/nz-starter-log,nz-framework/nz-starter-quartz -am test`：通过。
- 遗留问题：
  - `nz-starter-log` 当前只有自动配置骨架，日志采集切面、recorder 扩展点和脱敏能力放在 `P4` 继续完善。
  - 本地默认 JDK 是 21，运行 Maven 测试时需要显式切到 JDK 17。
