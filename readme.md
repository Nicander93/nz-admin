# nz-admin

`nz-admin` 是一个轻量的前后端分离后台脚手架，核心能力对标 RuoYi，保留用户、角色、菜单、权限、字典、参数、通知、文件、日志、定时任务等常用模块。

## 技术栈

- 后端：Spring Boot、MyBatis-Plus、Sa-Token、Hutool、PostgreSQL。
- 前端：Vue 3、TypeScript、Vite、Pinia、Element Plus、UnoCSS、ESLint、Prettier。

## 快速启动

### 1. 初始化数据库

创建 PostgreSQL 数据库后执行初始化脚本：

```bash
psql -h localhost -U postgres -d nz-server -f nz-server/nz-app/src/main/resources/db/init.sql
```

默认账号：

- 用户名：`admin`
- 密码：`admin123`

### 2. 启动后端

```bash
cd nz-server
./mvnw clean compile -DskipTests
./mvnw -pl nz-app spring-boot:run
```

开发环境默认读取 `nz-server/nz-app/src/main/resources/application-dev.yml`。如需修改数据库连接，请调整该文件。

### 3. 启动前端

```bash
cd nz-web
pnpm install
pnpm dev
```

浏览器打开前端控制台输出的地址，使用默认账号登录。

### 4. 生产配置

生产环境数据库连接从环境变量读取：

- `NZ_DATASOURCE_URL`
- `NZ_DATASOURCE_USERNAME`
- `NZ_DATASOURCE_PASSWORD`

### 接口文档（Springdoc）

后端启动后，在浏览器打开：

- Swagger UI：`http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

Sa-Token 仅拦截 `/api/**`（登录接口除外），上述路径无需登录即可访问。

### 文件存储

`application.yml` / 环境配置中 `nz.file` 常用项：

- `storage-type`：`local`（默认）或 `oss`
- `base-path`：本地存储根目录
- `max-file-size-bytes`：业务层单文件大小上限（与 `spring.servlet.multipart` 配合，建议 multipart 略大）

使用 OSS 时需配置 `nz.file.oss.endpoint`、`access-key-id`、`access-key-secret`、`bucket-name`，可选 `domain` 作为访问域名。

## 文档

- [P2 脚手架阶段说明](docs/p2-phase-overview.md)
- [CRUD 范式](docs/crud-paradigm.md)
- [模块开发指南](docs/module-development-guide.md)

## 开发规范

- 遵循 Alibaba 后端开发规范。
- 公共工具优先使用 Hutool、Spring、MyBatis-Plus 等已有依赖。
- 后端模块保持 `common`、`framework`、`module`、`app` 分层。
- 前端业务逻辑优先放在页面同目录 `hooks.ts`。

## 前端目录约定
- `nz-web/src/api` 按后端业务域拆分接口文件，`src/api/system/index.ts` 统一聚合导出各模块 API 和公共类型。
- 分页接口命名优先使用 `pageXxx`，普通列表接口使用 `listXxx`；为兼容旧调用可以保留别名，但新增代码应使用统一命名。
- `nz-web/src/views` 按路由和业务模块组织页面，页面私有逻辑放在同目录 `hooks.ts` 中。
- 全局 hooks 目录仅在存在跨页面复用逻辑时创建；空目录不保留。
