# Agent 任务：nz-admin 脚手架产品化与扩展模块体系补齐

## 背景

`nz-admin` 当前已经完成基础后台脚手架能力：用户、角色、菜单、权限、部门、岗位、字典、参数、通知、日志、文件、定时任务、工作台、基础监控、CRUD 范式、模块开发指南、CI 等。接下来目标不是继续零散加页面，而是将项目进一步打磨成一个对标 `RuoYi-Vue-Plus` 的现代前后端分离后台管理脚手架。

本轮任务优先级如下：

1. 修正脚手架基础可用性。
2. 建立测试保护网。
3. 设计并逐步补齐扩展模块体系。
4. 补齐系统能力差距。
5. 沉淀文档与 Agent 友好规则。
6. CLI 工具放到最后，只做规划，不在本轮提前实现。

## 总目标

将 `nz-admin` 从“可运行的后台模板”升级为“可扩展、可测试、可二次开发、可被 Agent 理解和维护的后台管理脚手架”。

本轮不追求一次性复制 RuoYi-Vue-Plus 全部能力，而是建立清晰的演进骨架，使后续功能能够按模块稳定扩展。

---

## 一、优先任务 1：修正脚手架基础可用性

### 目标

确保新用户只 clone `nz-admin` 仓库，就能完成安装、启动、构建和基础开发，不依赖作者本地私有路径或外部未发布包。

### 必做项

1. 检查前端依赖。

   * 重点检查 `nz-web/package.json` 中是否存在本地 `link:` 依赖。
   * 若存在类似 `@nz-js-toolkit/crud: link:../../nz-js-toolkit/...`，需要处理。
   * 推荐方案优先级：

     1. 将必要代码内置到 `nz-web/src/utils/crud` 或合适目录；
     2. 或将其纳入当前 monorepo；
     3. 暂不推荐依赖未发布 npm 包。

2. 检查 README 快速启动流程是否真实可执行。

   * 后端启动命令。
   * 前端安装命令。
   * 数据库初始化命令。
   * 默认账号说明。
   * Swagger/OpenAPI 地址。
   * 环境变量说明。

3. 清理文档残留。

   * 检查 `.docs/`、`docs/`、`AGENTS.md` 中是否存在明显复制残留，如其他项目名。
   * 修正不符合 `nz-admin` 当前结构的描述。
   * 不要把大量说明塞进 `AGENTS.md`，细节应沉淀到 `docs/`。

4. 补充文档入口。

   * 若缺少 `docs/index.md`，新增一个文档索引。
   * 索引至少包含：

     * 项目定位；
     * 快速启动；
     * 架构说明；
     * CRUD 范式；
     * 模块开发指南；
     * 测试计划；
     * 扩展模块规划；
     * Agent 工作入口。

### 验收标准

* `pnpm install && pnpm build` 不依赖仓库外部本地路径。
* README 中的启动命令与实际项目一致。
* 文档中不存在明显其他项目名残留。
* 新增或更新 `docs/index.md`。
* CI 能验证基础构建链路。

---

## 二、优先任务 2：建立测试保护网

### 目标

补齐 `nz-common`、`nz-framework`、核心 starter、系统模块的关键测试，避免后续扩展模块时破坏基础能力。

### 原则

测试不追求覆盖率数字，优先保护高复用、高影响路径。

优先测试：

* 公共返回结构；
* 分页结构；
* 树结构转换；
* 全局异常处理；
* 权限上下文；
* 数据权限；
* 操作日志；
* 文件校验；
* 定时任务核心逻辑；
* 监控聚合接口；
* 前端核心 hooks。

暂不优先测试：

* 纯 Lombok POJO；
* 常量类；
* 无逻辑配置类；
* 第三方框架默认行为；
* 只有简单转调的薄封装。

### 后端测试任务

1. `nz-common-core`

   * 补公共模型、分页、树结构、错误码、异常相关测试。
   * 测试文件与业务类结构保持一致。
   * 测试类命名使用 `XxxTest`。

2. `nz-starter-web`

   * 补全局异常处理测试。
   * 补统一响应包装或参数校验相关测试。
   * 尽量使用轻量 Spring 上下文，不启动完整应用。

3. `nz-starter-auth`

   * 补当前登录用户解析测试。
   * 补权限异常转换测试。
   * 补 Sa-Token 上下文清理相关测试。

4. `nz-starter-log`

   * 补 `@Log` 关键路径测试。
   * 覆盖敏感字段脱敏。
   * 覆盖异常场景记录。

5. `nz-starter-datascope`

   * 扩展数据权限测试。
   * 至少覆盖管理员、部门权限、本人权限、空权限、组合权限。

6. `nz-starter-file`

   * 补文件大小、类型、路径安全测试。
   * 不依赖真实 OSS。

7. `nz-starter-quartz`

   * 补 cron 校验、任务注册、暂停、恢复、手动执行核心测试。
   * 不依赖真实分布式调度。

### 前端测试任务

1. 保持测试目录与源码结构对应。
2. 优先补：

   * 登录状态；
   * 动态路由；
   * 权限指令；
   * CRUD hook；
   * 工作台 hook；
   * 运行状态页 hook。
3. 不要把测试文件混到源码目录内。

### CI 调整

当前 CI 应从“局部测试”逐步升级为：

```bash
cd nz-server
./mvnw test

cd nz-web
pnpm test
pnpm build
```

如全量测试时间过长，可先拆为：

* backend-compile；
* backend-unit；
* frontend-unit；
* frontend-build。

### 验收标准

* 新增测试能在本地通过。
* CI 至少覆盖后端核心模块测试和前端测试构建。
* 测试失败能定位具体规则。
* 新增测试不依赖真实 Redis、OSS、外部网络。

---

## 三、优先任务 3：扩展模块体系设计

### 目标

建立 `nz-admin` 的模块分层与扩展能力，使系统后续可以像 RuoYi-Vue-Plus 一样持续增加系统能力，而不是把所有功能堆进 `nz-system`。

### 模块分层建议

建议将系统能力划分为以下模块层级：

```text
core
├── auth                 # 登录、Token、权限上下文
├── user                 # 用户、角色、菜单、部门、岗位
└── dict-config          # 字典、参数

ops
├── log                  # 操作日志、登录日志、异常日志
├── monitor              # 服务监控、缓存监控、运行状态
├── online               # 在线用户
└── job                  # 定时任务、任务日志

storage
├── file                 # 文件管理
├── file-config          # 文件配置管理
└── oss                  # OSS / S3 / MinIO 扩展

security
├── datascope            # 数据权限
├── sensitive            # 数据脱敏
├── encryption           # 字段加解密
├── idempotent           # 幂等
└── rate-limit           # 限流

infra
├── redis                # Redis 集成
├── websocket            # WebSocket
├── sse                  # SSE
├── mail                 # 邮件
└── sms                  # 短信

tenant
├── tenant               # 租户管理
├── tenant-package       # 租户套餐
└── tenant-permission    # 租户菜单权限

client
├── client               # 客户端管理
├── login-method         # 登录方式配置
└── token-policy         # Token 时效策略

devtools
├── codegen              # 代码生成
├── api-doc              # 接口文档增强
└── demo                 # 使用案例模块

ai
├── assistant            # 系统 AI 助手
├── docs-qa              # 文档问答
└── operation-helper     # 操作辅助
```

### 本轮需要完成的设计文档

新增：

```text
docs/extension-module-system.md
```

文档必须包含：

1. 模块分层设计。
2. 每类模块的职责边界。
3. 当前已具备能力映射。
4. 当前缺失能力映射。
5. 推荐实现顺序。
6. 模块启停策略。
7. 后端包结构建议。
8. 前端菜单与页面结构建议。
9. 数据库初始化与升级脚本约定。
10. 测试与文档同步要求。

### 当前能力映射要求

请基于现有代码实际检查，不要只按文档猜测。

输出类似：

```text
已具备：
- 用户管理
- 角色管理
- 菜单管理
- 部门管理
- 岗位管理
- 字典管理
- 参数管理
- 通知公告
- 操作日志
- 登录日志
- 在线用户
- 定时任务
- 文件管理
- 工作台
- 基础运行监控

部分具备：
- 文件存储：已有本地/OSS配置，但缺文件配置管理页面
- 监控：已有基础运行状态，但缺缓存监控、服务深度监控
- 定时任务：已有 Quartz 能力，但不是 RuoYi-Vue-Plus 式分布式任务体系

缺失：
- 租户管理
- 租户套餐
- 客户端管理
- Redis 缓存监控
- 数据脱敏
- 字段加解密
- 幂等
- 限流
- WebSocket
- SSE
- 邮件
- 短信
- 代码生成
- Demo 案例模块
- AI 助手
```

---

## 四、优先任务 4：补齐第一批扩展模块

### 目标

不要一次性实现所有扩展模块。先补最能体现脚手架价值、且依赖较少的第一批能力。

### 第一批建议实现

#### 1. 文件配置管理

当前已有文件上传和存储配置说明，但更像静态配置。建议补一个“文件配置管理”模块，为后续 S3、MinIO、阿里云 OSS、腾讯云 COS 等做扩展。

功能：

* 文件配置列表；
* 新增文件配置；
* 编辑文件配置；
* 启用/停用；
* 默认配置；
* 存储类型；
* endpoint；
* bucket；
* domain；
* accessKey 脱敏展示；
* secret 不明文返回；
* 连接测试接口可后置。

验收：

* 后端 CRUD；
* 前端页面；
* 菜单权限；
* 初始化 SQL；
* 基础测试；
* 文档说明。

#### 2. 缓存监控

当前运行监控里 Redis 缓存状态是延后项。若项目已引入 Redis，则补缓存监控；若尚未引入，则先设计 starter 和配置，不强制引入业务依赖。

功能：

* Redis 是否启用；
* 连接状态；
* Redis version；
* used_memory；
* connected_clients；
* keyspace；
* commandstats；
* 慢查询可后置。

验收：

* 没配置 Redis 时系统可正常启动；
* 配置 Redis 时显示缓存状态；
* 前端运行状态页增加缓存区块；
* 测试覆盖 Redis 未启用场景。

#### 3. 客户端管理 v0

这是对标 RuoYi-Vue-Plus 的关键系统能力之一，但可以先做轻量版。

功能：

* 客户端列表；
* clientId；
* clientName；
* 登录类型；
* token 有效期；
* 状态；
* 备注；
* 创建时间；
* 修改时间。

暂不做复杂 OAuth/OIDC，不要过度设计。

验收：

* 后端 CRUD；
* 前端页面；
* 菜单权限；
* 初始化 SQL；
* 登录逻辑暂不强依赖客户端配置，但预留扩展点。

#### 4. Demo 案例模块

RuoYi-Vue-Plus 有 Demo 案例，这是脚手架学习价值的一部分。建议 `nz-admin` 也补一个轻量案例模块。

功能：

* 示例 CRUD；
* 示例树表；
* 示例字典使用；
* 示例文件上传；
* 示例权限按钮；
* 示例数据权限可后置。

验收：

* 不污染系统管理主流程；
* 能作为二次开发参考；
* 文档明确“示例模块可删除”。

---

## 五、优先任务 5：系统能力后续规划

以下能力本轮只做规划，不要求全部实现：

### P2 后续 / P3 能力池

1. 多租户管理。
2. 租户套餐管理。
3. 数据脱敏。
4. 字段加解密。
5. 幂等。
6. 限流。
7. WebSocket。
8. SSE。
9. 邮件。
10. 短信。
11. 代码生成器。
12. AI 助手。
13. 服务监控增强。
14. SQL 监控。
15. Docker Compose 一键环境。
16. 多数据源。

请新增或更新：

```text
docs/exec-plans/ruoyi-plus-alignment-next-plan.md
```

该文档需要给出：

* 对标能力矩阵；
* 已完成；
* 部分完成；
* 未完成；
* 不打算做；
* 推荐实现优先级；
* 每项能力的最小可用版本定义。

---

## 六、优先任务 6：Agent 友好能力

### 目标

让后续 coding agent 进入仓库后能快速理解项目结构、扩展模块规则、测试规则和常见任务路径。

### 必做项

1. 保持 `AGENTS.md` 简短。
2. 新增或完善：

   * `docs/index.md`
   * `docs/extension-module-system.md`
   * `docs/exec-plans/ruoyi-plus-alignment-next-plan.md`
   * `docs/agent-task-recipes.md`

### `docs/agent-task-recipes.md` 至少包含

1. 新增一个系统 CRUD 模块怎么做。
2. 新增一个扩展 starter 怎么做。
3. 新增一个前端菜单页面怎么做。
4. 新增一个权限按钮怎么做。
5. 新增一个初始化 SQL 怎么做。
6. 新增一个测试怎么做。
7. 修改系统能力后需要同步哪些文档。

---

## 七、最后任务：CLI 工具规划，不实现

### 目标

CLI 是重要方向，但本轮不实现。只做设计文档，避免过早分散主线。

新增：

```text
docs/exec-plans/npm-cli-tool-plan.md
```

内容包括：

1. CLI 定位。
2. 为什么放到后续阶段。
3. 未来命令设计。
4. 与代码生成器、扩展模块的关系。
5. 包名修改范围。
6. 模块启停能力。
7. 风险点。

建议命令草案：

```bash
nz-admin doctor
nz-admin rename
nz-admin dev
nz-admin build
nz-admin module add
nz-admin module enable
nz-admin module disable
nz-admin codegen
```

注意：本轮不要创建 npm 包，不要实现 CLI 命令。

---

## 八、执行顺序

严格按以下顺序执行：

1. 修正基础可用性。
2. 补测试保护网第一批。
3. 编写扩展模块体系文档。
4. 实现第一批扩展模块：

   * 文件配置管理；
   * 缓存监控；
   * 客户端管理 v0；
   * Demo 案例模块。
5. 编写 RuoYi-Vue-Plus 对标后续计划。
6. 补 Agent 任务文档。
7. 编写 CLI 工具规划文档，但不实现。

---

## 九、最终验收命令

后端：

```bash
cd nz-server
./mvnw clean test
./mvnw -pl nz-app -am compile -DskipTests
```

前端：

```bash
cd nz-web
pnpm install
pnpm test
pnpm build
```

如新增 E2E：

```bash
cd nz-web
pnpm e2e
```

---

## 十、交付要求

最终提交应包含：

1. 基础可用性修复。
2. 第一批测试补齐。
3. 扩展模块体系文档。
4. 第一批扩展模块实现。
5. RuoYi-Vue-Plus 对标后续计划。
6. Agent 任务 recipes。
7. CLI 工具规划文档。
8. README 或 docs/index.md 更新。
9. CI 配置必要调整。

所有新增系统功能必须同时包含：

* 后端代码；
* 前端页面；
* 菜单权限 SQL；
* 基础测试；
* 文档说明；
* 验收命令结果说明。

CLI 工具只允许规划，不允许本轮实现。
