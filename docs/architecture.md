# 后端模块与目录边界

## Maven 模块边界

`nz-server` 按职责分为四层：

- `nz-app`：启动层，只放启动类、运行配置、Flyway 迁移和启动期装配。
- `nz-common`：公共基础对象和轻量协议，不放业务逻辑。
- `nz-framework`：框架级 starter，提供可复用能力、自动配置、注解和扩展点。
- `nz-module`：业务模块，业务代码按模块沉淀在这里。

依赖方向保持单向：

```text
nz-app -> nz-module -> nz-framework -> nz-common
```

`nz-framework` 不能依赖 `nz-module`，业务模块可以按需依赖框架 starter。

## 模块说明规则

后端模块需要同时维护 Maven POM 描述和 Java 包说明，两者分工不同：

- `pom.xml` 的 `<description>` 写一句话，说明 Maven 模块的定位、核心职责和聚合范围，方便在 IDE、依赖树和模块列表中快速识别。
- `package-info.java` 写模块代码边界，说明根包职责、模块内领域或职责划分，以及不应该放进该模块的内容。
- 聚合模块只补 `<description>`；没有 Java 源码根包时，不为了说明而创建空代码包。
- starter 模块的 `package-info.java` 要强调框架能力、自动配置、注解和扩展点，避免写成具体业务说明。
- 业务模块的 `package-info.java` 要列出稳定业务域，并说明各技术层使用同一套域名称，便于横向对照。

## nz-framework starter 设计规范

`nz-framework` 下的 `nz-starter-*` 模块统一遵循下面的约定：

- 每个 starter 都要在 `pom.xml` 里写清楚模块职责，不用业务词汇替代框架词汇。
- 每个 starter 根包都要提供 `package-info.java`，说明模块边界、主要子包和不该进入模块的内容。
- 自动配置类统一放在 `config` 包，命名为 `NzXxxAutoConfiguration` 或与领域一致的 `XxxAutoConfiguration`。
- 自动配置注册统一使用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 注解放在 `annotation` 包，切面放在 `aspect` 包，配置属性放在 `properties` 包，扩展接口优先放在 `core` 或 `support` 包。
- starter 只提供通用机制、默认实现和扩展点，不依赖 `nz-module`，也不直接访问业务表。
- 业务模块只实现 framework 暴露的扩展接口，例如用户解析、日志落库、任务日志记录等。
- 新增 starter 时，至少补一组最小自动配置或关键行为测试，再接业务样例。

当前 `nz-framework` 的 starter 职责建议收口为：

- `nz-starter-web`：统一异常、Web 配置、文档、请求上下文。
- `nz-starter-auth`：认证上下文、权限注解、Sa-Token 接入。
- `nz-starter-datascope`：数据权限注解、切面和当前用户解析扩展点。
- `nz-starter-mybatis`：持久层自动配置、分页、查询辅助、审计字段填充。
- `nz-starter-tenant`：可信租户上下文、异步上下文传递和 MyBatis 行级隔离。
- `nz-starter-log`：操作日志注解、切面、采集与记录扩展点。
- `nz-starter-file`：文件存储抽象、本地/OSS 实现与安全校验。
- `nz-starter-encryption`：字段加密协议、密钥轮换、MyBatis 类型处理器和脱敏工具。
- `nz-starter-quartz`：定时任务调度装配、任务执行封装和调度辅助。
- `nz-starter-test`：测试基类、测试配置和上下文清理工具。

## 公共对象边界

`nz-common` 与 `nz-framework` 的对象边界保持克制：

- `nz-common` 放轻量协议和跨层公共对象，例如统一返回结构、错误码接口、分页结果、任务调用协议等。
- `nz-framework` 放运行期自动配置、切面、过滤器、持久层基类、上下文工具和默认实现。
- 持久化基类只在 `nz-framework` 的 MyBatis starter 内定义，不再把数据库审计字段模型扩散到 `nz-common`。
- 如果对象同时依赖 Spring、MyBatis-Plus、Sa-Token、Quartz 等运行时框架，它应放在 `nz-framework`，而不是 `nz-common`。

## nz-system 目录规则

`nz-module/nz-system` 是系统管理业务模块。模块内继续按技术层组织，但每一层下面都要按业务域稳定分组：

```text
com.nz.admin.modules.system
├─ controller
│  ├─ user
│  ├─ role
│  ├─ file
│  └─ ...
├─ service
│  ├─ user
│  ├─ role
│  ├─ file
│  └─ ...
├─ mapper
│  ├─ user
│  ├─ role
│  ├─ file
│  └─ ...
├─ entity
│  ├─ dataobject
│  ├─ query
│  ├─ vo
│  └─ dto
└─ convert
   ├─ user
   ├─ role
   ├─ file
   └─ ...
```

新增业务域时，优先补齐对应的 `controller/service/mapper/entity/convert` 子包，不把新类直接散放在大目录下。

## 文件存储边界

文件存储的底层能力放在 `nz-framework/nz-starter-file`：

- `FileStorageService`：存储策略接口。
- `FileStorageObject`：存储层中立文件对象，不绑定业务表。
- `LocalFileStorageServiceImpl`：本地存储实现。
- `OssFileStorageServiceImpl`：OSS 存储实现。
- `FileStorageProperties`：`nz.file` 配置，生效的数据库配置会在运行时更新该单例。
- `FileConfigSecretCodec`：使用部署密钥对持久化的 OSS Secret 做 AES-GCM 编解码。

`nz-system` 只负责系统文件管理业务：

- `FileController`：系统文件接口。
- `FileService` / `FileServiceImpl`：上传、下载、删除、入库。
- `FileDO` / `FileMapper` / `FileQuery`：系统文件表与查询。

这样 OSS SDK 等存储实现细节只留在 `nz-starter-file`，业务模块通过抽象接口使用文件能力。

## 多租户边界

多租户的技术机制放在 `nz-framework/nz-starter-tenant`，租户和套餐业务放在 `nz-system`：

- framework 只认识租户 ID、受管表和服务端令牌会话，不访问租户业务表。
- system 负责租户编码解析、状态与到期校验、套餐菜单和租户基础数据初始化。
- `nz-starter-mybatis` 通过 `MybatisPlusInterceptorCustomizer` 接受租户拦截器，并保证租户条件先于分页执行。
- 租户 ID 只从登录解析结果和服务端会话恢复，不接受前端租户 ID 请求头。
- 默认租户可以管理所有租户；普通租户的菜单权限始终与套餐菜单求交集。

具体受管表、登录流程和升级规则见 [multi-tenancy.md](multi-tenancy.md)。

## 可移除业务模块

可移除模块必须同时满足以下约束：

- 模块通过自己的自动装配类扫描 Controller、Service 和 Mapper，`nz-app` 不扫描业务实现包。
- 模块提供 `META-INF/nz/module.yaml`，注册表从 classpath 读取描述，不在 Java 代码里维护模块列表。
- 模块只依赖 common 和 framework starter，不依赖 `nz-system` 或其他可选业务模块。
- 前端通过 `src/modules/<code>/manifest.ts` 声明组件前缀，中央注册表用 `import.meta.glob` 自动发现。
- 数据库变更使用独立 Flyway 版本。已执行的迁移不得直接删除，卸载已投产模块要追加前向清理迁移。

`nz-demo` 是当前验证样例。它的 CRUD、菜单权限、V7 迁移、测试和删除步骤见 [demo-module.md](demo-module.md)。

`nz-workflow` 同样遵循可移除模块边界，已交付流程分类、定义发布、实例执行、运行轨迹和任务中心。
实例保存定义与变量快照；当前待办、历史已办和抄送分别落在 `flow_task`、`flow_history_task` 和 `flow_task_copy`。
当前运行器支持顺序流、条件互斥分支和单任务转办，多人协作与并行执行边界见 [workflow.md](workflow.md)。

## 代码生成模块

`nz-generator` 是独立业务模块，不依赖 `nz-system`。它通过 `JdbcTemplate` 读取 PostgreSQL 元数据，通过资源模板生成后端、前端和菜单 SQL：

- 元数据仓库只负责表和字段读取，不保存生成配置。
- 模板渲染器负责类型映射、命名和文件内容，预览与 ZIP 下载复用同一结果。
- 控制器只暴露列表、字段、预览和下载接口，不直接写工作区或执行 SQL。
- 前端提供表筛选、参数配置、字段检查、代码预览和 ZIP 下载。
- 模块仅支持单主键表；复合主键、跨数据源和模块脚手架留给后续版本。

具体用法和模板目录见 [code-generator.md](code-generator.md)。

## 在线会话边界

在线用户管理属于 `nz-system`，但不让业务服务直接遍历 Sa-Token 静态状态：

- `OnlineSessionAccessor` 隔离令牌查询、会话读取和强制退出操作，便于测试和替换实现。
- 登录成功后由 `LoginController` 把租户、用户、部门、IP、登录时间和 User-Agent 写入服务端会话。
- `OnlineUserService` 负责租户边界；默认租户可查看全部会话，普通租户只能查看和退出本租户会话。
- 浏览器提交的租户信息不参与会话归属判断，权限以服务端会话和当前登录上下文为准。

## 实时通信边界

`nz-starter-realtime` 只负责连接、票据和消息传输，`nz-system` 负责当前用户、租户和菜单权限：

- SSE 与 WebSocket 使用相同的 `RealtimeMessage` 信封和 `RealtimePublisher` 发布端口。
- 浏览器先通过已鉴权 API 领取一次性票据，再连接不在 `/api` 下的传输端点；登录令牌不进入长连接 URL。
- `RealtimeService` 从 `LoginUserContext` 和 `TenantContextHolder` 固化连接身份，用户定向发布同时校验租户 ID。
- 票据、连接与统计保存在当前 JVM。多节点部署需要共享票据并增加跨节点消息总线，不能把节点内广播当成集群广播。

接口、代理配置和多节点限制见 [realtime-communication.md](realtime-communication.md)。


## 短信边界

`nz-starter-sms` 定义供应商扩展点和统一网关，`nz-system` 负责租户内的渠道、模板、发送记录和权限：

- starter 内置本地日志与通用 Webhook 两种 provider，业务模块也可以注册新的 `SmsProvider`。
- 渠道密钥与发送手机号复用字段加密能力，管理接口只返回密钥是否已配置和脱敏手机号。
- 模板在业务层渲染，发送记录在调用供应商前落为 `PENDING`，调用结束后更新最终状态。
- system 认证层提供账号密码与短信验证码两种客户端授权模式；短信入口按租户和手机号隔离，验证码只保存摘要并限制重发间隔、有效期和尝试次数。
- 登录前接口不挂菜单权限，租户和客户端的启用状态、`login_type` 是认证边界。
- 当前验证码存储为单节点内存实现；多节点部署必须替换为共享的原子存储。

配置、接口和扩展方式见 [sms-management.md](sms-management.md)。

## 第三方认证边界

`nz-starter-social` 只实现标准 OAuth2/OIDC 协议，不依赖用户、租户或菜单业务：

- PKCE verifier 和一次性 state 只保存在服务端，state 同时绑定授权用途、租户、客户端和用户。
- OIDC ID Token 必须通过签名、有效期、issuer 和 audience 校验；标准 OAuth2 通过 userinfo 获取稳定身份。
- `nz-system` 负责 `sys_social` 绑定关系、social 客户端授权类型、登录会话和当前用户解绑权限。
- 服务商令牌不落库。默认 state 存储是单节点内存实现，多实例生产部署必须提供共享实现。

配置、接口和部署限制见 [social-login.md](social-login.md)。

## 站内消息边界

站内消息属于 `nz-system` 业务域，复用租户和实时通信 starter，不把消息表或菜单逻辑下沉到 framework：

- `sys_message` 按接收用户逐行存储，使已读和删除状态互不影响。
- MyBatis 租户拦截器限制租户范围，服务层再次校验当前用户是接收人。
- 管理员发送只解析当前租户的启用用户，指定用户单次最多 500 个。
- 数据库收件箱是事实来源；`RealtimePublisher` 仅在事务提交后发送到达提示，推送失败不回滚消息。
- 前端正文按纯文本显示，跳转地址只接受站内绝对路径。

接口、权限和数据保留限制见 [message-center.md](message-center.md)。

## 个人中心边界

个人中心属于 `nz-system` 的当前用户业务域，复用用户、权限和文件服务：

- 接口只从登录会话读取用户 ID，不接受目标用户 ID。
- 联系方式继续使用加密类型处理器，并在当前租户范围检查重复。
- 密码修改必须校验旧 BCrypt 摘要，不能复用当前密码。
- 头像复用文件存储与安全校验，通过鉴权 Blob 请求展示，不暴露匿名文件地址。
- 前端使用受登录守卫保护的静态 `/profile` 路由，不依赖管理员菜单授权。

接口和安全说明见 [user-profile.md](user-profile.md)。

## 工程 CLI 边界

根目录 `nz` / `nz.cmd` 负责跨模块工程操作，实现在 `tools/nz-cli`：

- `doctor`、`migration check` 只读检查环境、迁移和 Maven 接入。
- `dev`、`build`、`verify` 统一调度 Maven Wrapper 与 pnpm，不复制构建逻辑。
- `module add` 生成模块骨架、前后端清单、菜单权限迁移和测试，并修改 Maven 聚合入口。
- `module enable/disable` 只改部署期开关，不删除代码和数据。
- `codegen` 调用运行中的 `nz-generator`，CLI 自身不连接业务数据库。
- `rename` 只替换受控文本文件内容，不重命名目录或 Java 包。

修改型命令要求 `--dry-run` 或 `--yes`。实际写入先在 `.nz-cli/backups` 保存原文件，`rollback` 根据操作清单恢复。CLI 不承载业务逻辑，也不绕过模块协议和 Flyway。
