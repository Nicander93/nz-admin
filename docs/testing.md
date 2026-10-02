# 通用测试支持

测试工具集中管理，业务断言仍放在所属模块。不会在工具包中启动整个应用或硬编码业务数据。

## 后端

测试依赖 `nz-framework/nz-starter-test`（scope 为 test），复用现有 `BaseMockitoUnitTest`、`BaseDbUnitTest` 和上下文清理。数据库单测由模块维护自己的 schema 和清理脚本。

真实 HTTP 测试使用 `ApiTestClient`：

```java
var api = new ApiTestClient(restTemplate).login("default", "admin", "admin123");
var data = api.ok(HttpMethod.GET, "/api/system/user/page", null);
```

`ok` 断言 HTTP 成功和统一业务码并返回 data；`exchange` 返回完整响应，适合断言业务拒绝。登录返回新的客户端实例，不修改共享客户端身份。

```bash
cd nz-server
./mvnw test
```

数据权限 starter 集成测试使用 H2 和真实 MyBatis 插件，覆盖分页、详情、JOIN、更新、批量删除、空范围及租户隔离。system 单测覆盖多角色并集和部门树。

`DataScopePostgresTest` 必须使用独立 PostgreSQL 测试库，执行真实 Flyway 迁移和登录后的 HTTP 权限验证。没有 `NZ_TEST_PG_URL` 时自动跳过。测试会写入固定测试 ID 并设置测试库 admin 密码，不能连接业务数据库。

```bash
export NZ_TEST_PG_URL=jdbc:postgresql://localhost:5432/nz_test
export NZ_TEST_PG_USERNAME=postgres
export NZ_TEST_PG_PASSWORD=postgres
./mvnw -pl nz-app -am test -Dtest=DataScopePostgresTest -Dsurefire.failIfNoSpecifiedTests=false
```

## 前端

`nz-web/packages/nz-test` 是 workspace 包 `@nz/test`，当前为仓库内部复用，不发布 npm：

- `@nz/test/unit`：统一响应、分页数据及 Vue hook 宿主；调用方负责 mount/unmount。
- `@nz/test/config`：Vitest 和 Playwright 配置工厂，可覆盖浏览器地址及启动命令。
- `@nz/test/e2e`：UI/API 登录、authenticatedPage fixture 和 expect。

业务测试可以导入 `pageResponse`，避免重复构造统一分页协议。mock 和业务断言保留在测试文件中。

```bash
cd nz-web
pnpm install --frozen-lockfile
pnpm test
pnpm build
pnpm exec playwright install chromium
E2E_USERNAME=admin E2E_PASSWORD=admin123 pnpm e2e
```

E2E 默认启动本地 Vite，需先启动后端（默认 8080），账号通过环境变量提供；租户编码默认 default。`E2E_BASE_URL`、`E2E_WORKERS` 可覆盖地址和并发。

CI 的 e2e job 使用独立 PostgreSQL，先验证迁移和真实 HTTP 数据权限，再启动后端运行四个浏览器冒烟测试。失败时上传 trace、截图和后端日志。新增核心操作应按需补浏览器行为测试，不能仅用冒烟测试代替数据权限服务端验证。
