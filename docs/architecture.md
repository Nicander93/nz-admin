# 后端模块与目录边界

## Maven 模块边界

`nz-server` 按职责分为四层：

- `nz-app`：启动层，只放启动类、运行配置、初始化脚本和启动期装配。
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
- `nz-starter-log`：操作日志注解、切面、采集与记录扩展点。
- `nz-starter-file`：文件存储抽象、本地/OSS 实现与安全校验。
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
- `FileStorageProperties`：`nz.file` 配置。

`nz-system` 只负责系统文件管理业务：

- `FileController`：系统文件接口。
- `FileService` / `FileServiceImpl`：上传、下载、删除、入库。
- `FileDO` / `FileMapper` / `FileQuery`：系统文件表与查询。

这样 OSS SDK 等存储实现细节只留在 `nz-starter-file`，业务模块通过抽象接口使用文件能力。
