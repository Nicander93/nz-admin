# 开发环境准备

这一步的结果是：命令行能找到 Java、Node 和 pnpm，后端能连接一个独立 PostgreSQL 开发库。先完成这些，再启动应用，排错范围会小很多。

## 工具版本与检查

| 工具 | 仓库要求 | 确认方法 |
| --- | --- | --- |
| JDK | 17 | `java -version`；同时核对 `JAVA_HOME` |
| Node.js | 22.13 或以上 | `node --version` |
| pnpm | 9.15.9 | `pnpm --version` |
| Maven | 使用仓库 Wrapper | 在 `nz-server` 执行 `./mvnw -v` |
| PostgreSQL | 可连接的独立开发库；CI 使用 16 | 使用同一主机、端口和账号连接数据库 |

仓库根目录运行 `./nz doctor`；Windows PowerShell 使用 `.\nz.cmd doctor`。IDE 的 Java SDK、Maven Runner 和终端 `JAVA_HOME` 应指向同一 JDK。不要把其他项目的 `node_modules` 复制到当前项目。

## 先确认数据库连接

开发默认库名为 `nz-server`，配置在 `nz-server/nz-app/src/main/resources/application-dev.yml`。用 PostgreSQL 客户端创建空库后，填入自己的用户名和密码。

```bash
psql --host=127.0.0.1 --port=5432 --username=postgres --dbname=nz-server
```

这条命令只检查连接；密码通过客户端提示输入。应用第一次启动会运行 Flyway。已有业务库不能按空库初始化，应先走[迁移与升级](/guide/database/migrations)。

## Windows、WSL 和 Docker 的端口

| 后端在哪里运行 | 数据库在哪里 | 要确认什么 |
| --- | --- | --- |
| WSL | 同一 WSL | 数据库监听地址与端口 |
| WSL 或 Windows | Docker | `docker port <容器名>` 的宿主机发布端口 |
| Windows | WSL | 当前机器是否支持 localhost 转发，以及实际监听地址 |

`5432/tcp` 只代表容器内端口。如果发布为 `5433:5432`，宿主机运行的后端应连接 5433。修复连接应先核对端口和凭据，不要删除数据卷重新建库。

## 安装与启动

```bash
cd nz-server
./mvnw -pl nz-app -am install -DskipTests
./mvnw -pl nz-app spring-boot:run
```

另开终端启动前端：

```bash
cd nz-web
pnpm install --frozen-lockfile
pnpm dev
```

后端默认监听 8080；前端以 Vite 实际打印的地址为准。Redis 在基础单节点开发中可不启用；集群验证需要共享 Redis。继续阅读[配置生效方式](/start/configuration)和[首次登录](/start/first-login)。
