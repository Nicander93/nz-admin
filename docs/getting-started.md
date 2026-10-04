# 快速开始

目标是在本地登录控制台，并完成一次修改后的验证。

第一次配置环境可依次阅读[环境准备](/start/environment)、[配置生效方式](/start/configuration)和[首次登录验收](/start/first-login)。本页保留快速路径。

## 准备环境

| 工具 | 要求 |
| --- | --- |
| JDK | Java 17 |
| Node.js | 22.13 或以上 |
| pnpm | 9，仓库声明版本为 9.15.9 |
| PostgreSQL | 可连接的空库；CI 使用 PostgreSQL 16 |
| Redis | 基础单节点启动可不启用；集群和对应集成测试需要 |

克隆项目并检查环境：

```bash
git clone https://github.com/Nicander93/nz-admin.git
cd nz-admin
./nz doctor
```

Windows 可使用 `.\nz.cmd doctor`。检查失败时先按输出修复对应工具或配置，命令说明见[项目 CLI](cli.md)。

## 配置独立开发数据库

创建空的 `nz-server` 数据库。开发配置位于 `nz-server/nz-app/src/main/resources/application-dev.yml`，默认连接 `localhost:5432`、用户 `postgres`；将连接信息改为自己的开发库。

后端首次启动会执行 Flyway V1–V30。已有库先备份，再按[数据库迁移](database-migrations.md)升级；不要重新导入 `init.sql` 覆盖现有数据。

## 启动后端与前端

后端终端：

```bash
cd nz-server
./mvnw -pl nz-app -am install -DskipTests
./mvnw -pl nz-app spring-boot:run
```

前端终端：

```bash
cd nz-web
pnpm install --frozen-lockfile
pnpm dev
```

打开前端终端显示的地址。开发环境默认租户编码是 `default`，账号 `admin`，初始密码 `admin123`。这些是本地初始化值，生产初始化方式见[部署指南](deployment.md)。

后端 readiness 地址为 `http://localhost:8080/actuator/health/readiness`；Swagger UI 为 `http://localhost:8080/swagger-ui/index.html`。

## 体验业务审批

后端启动环境设置 `NZ_WARM_FLOW_ENABLED=true` 后重启。以有设计权限的账号打开“工作流程 → 流程中心”，创建业务用途为“请假申请”的流程，配置审批人并发布；申请人从“示例模块 → 请假申请”保存草稿和提交。审批人从流程中心待办办理。

用户需要对应菜单和按钮权限。完整行为与当前限制见[工作流指南](workflow.md)。

## 验证一次修改

```bash
./nz migration check
./nz verify
```

`verify` 运行后端测试和前端测试/构建；真实 PostgreSQL、Redis 和浏览器验证需要额外环境，见[测试与验收](testing.md)。

准备新增业务时，从[开发指南](developer-guide.md)继续。只想查看本站，可进入 `docs` 执行 `pnpm install --frozen-lockfile` 和 `pnpm dev`。
