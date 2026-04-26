# 仓库架构

## 定位

`nz-admin` 是一个类似 ruoyi-vue-plus 的前后端分离后台管理框架，基于 Spring Boot + Vue3 构建。

## 技术栈

### 后端 `nz-server`

- Java 21
- Spring Boot 3.3
- MyBatis-Plus（ORM + 分页）
- Sa-Token（认证鉴权）
- Hutool（工具库）
- PostgreSQL
- Lombok

### 前端 `nz-web`

- Vite 8
- Vue 3 + TypeScript 5.8
- Element Plus（UI 组件库）
- UnoCSS 66（原子化 CSS）
- Pinia（状态管理）
- Axios（HTTP 请求）
- vue-router
- xe-utils
- ESLint + Prettier

## 目录结构

```text
nz-admin/
├── nz-server/                                  # 后端（Maven 多模块）
│   ├── pom.xml                                 # 父 POM（版本管理 + 模块声明）
│   ├── nz-common/                              # 通用模块
│   │   └── src/.../com/nz/admin/common/        # R、BaseEntity、PageQuery
│   ├── nz-framework/                           # 框架父模块
│   │   ├── pom.xml                             # 聚合框架 starter
│   │   ├── nz-starter-web/                     # Web starter（CORS、全局异常、Swagger）
│   │   ├── nz-starter-mybatis/                 # MyBatis-Plus starter
│   │   └── nz-starter-sa-token/                # Sa-Token starter
│   ├── nz-module/                              # 业务模块根目录
│   │   ├── nz-system/                          # 系统管理模块
│   │   │   └── src/.../com/nz/admin/modules/system/ # entity / mapper / service
│   │   └── src/.../com/nz/admin/modules/       # 业务代码按领域建子包
│   └── nz-app/                                 # 启动模块
│       └── src/main/
│           ├── java/com/nz/admin/
│           │   ├── NzAdminApplication.java     # 启动类
│           │   ├── config/                     # 应用级配置（DataInitializer）
│           │   └── modules/system/controller/  # Controller
│           └── resources/
│               ├── application.yml
│               └── db/init.sql
├── nz-web/                                     # 前端
│   ├── src/
│   │   ├── api/                                # 接口请求
│   │   ├── layout/                             # 布局组件
│   │   ├── router/                             # 路由
│   │   ├── stores/                             # Pinia 状态
│   │   ├── styles/                             # 全局样式
│   │   └── views/                              # 页面
│   ├── vite.config.ts
│   └── package.json
└── .docs/                                      # 项目文档
```

## 后端模块依赖

```text
nz-common          ← mybatis-plus-extension, lombok
    ↑
nz-starter-web     ← nz-common, spring-boot-starter-web, validation, springdoc
nz-starter-mybatis ← mybatis-plus-starter
nz-starter-sa-token ← sa-token-starter, spring-webmvc
nz-system          ← nz-common, hutool-core
nz-module          ← nz-common
    ↑
nz-app             ← nz-starter-web, nz-starter-mybatis, nz-starter-sa-token,
                      nz-system, nz-module, hutool-all, postgresql
```

## 后端分层规则

严格分层：Controller → Service → Mapper，禁止跨层调用。

| 层 | 职责 | 禁止 |
|----|------|------|
| Controller | 接收请求、参数校验、调用 Service、组装响应 | 不可直接调用 Mapper |
| Service | 业务逻辑编排，纯接口 + 实现，不继承 IService/ServiceImpl | 不出现 LambdaQueryWrapper 等 ORM 概念 |
| Mapper | 数据访问，extends BaseMapper，自定义查询用 default 方法封装 | — |

## 后端约定

- 包结构：`com.nz.admin.modules.{模块名}.{entity|mapper|service|controller}`
- 统一响应体：`R<T>`，code=200 表示成功
- 分页查询使用 `PageQuery` + MyBatis-Plus `Page`
- 密码使用 BCrypt 加密（hutool）
- API 路径统一以 `/api/` 开头
- Sa-Token 拦截 `/api/**`，放行 `/api/auth/login`
- 系统管理相关模块放 `nz-module/nz-system`（artifactId 仍是 `nz-system`），业务模块放 `nz-module`
- Web 通用能力（CORS、全局异常、Swagger）放 `nz-starter-web`
- MyBatis-Plus 配置放 `nz-starter-mybatis`
- Sa-Token 拦截与鉴权异常处理放 `nz-starter-sa-token`
- Controller 和应用级配置（DataInitializer 等）放 `nz-app`
- Swagger 默认地址为 `/swagger-ui/index.html`

## 前端约定

- 组件按需引入 Element Plus（unplugin-auto-import + unplugin-vue-components）
- 请求封装在 `src/api/request.ts`，自动注入 token、统一错误处理
- Vite dev server 代理 `/api` 到后端 `http://localhost:8080`
- 路由守卫基于 localStorage 中的 token 判断登录状态

## 构建与运行

### 后端

```bash
cd nz-server
# 先执行 nz-app/src/main/resources/db/init.sql 创建表
mvn spring-boot:run -pl nz-app
# 首次启动会自动创建 admin 账号（admin / admin123）
```

### 前端

```bash
cd nz-web
npm install
npm run dev
```

## 文档同步要求

- 结构变化：更新本文件
- 工作方式变化：更新 `harness-engineering.md`
- 较大任务：在 `exec-plans/` 记录计划
