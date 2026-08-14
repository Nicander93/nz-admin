# 生产部署

仓库提供一套可直接构建的 Docker Compose 交付环境。默认包含 PostgreSQL、后端和前端，Redis 与 MinIO 按需启用。

## 首次启动

进入仓库根目录后复制环境变量模板：

```bash
cp deploy/.env.example deploy/.env
```

至少替换以下值，禁止把示例密码用于生产：

- `NZ_DB_PASSWORD`：PostgreSQL 密码。
- `NZ_INITIAL_ADMIN_PASSWORD`：首次创建管理员时使用的密码。
- `NZ_FILE_CONFIG_KEY`：文件配置凭据加密密钥，建议使用 32 字节以上随机值并长期保管。
- `NZ_MINIO_ROOT_PASSWORD`：即使暂不启用 MinIO，也应在 Compose 解析配置前填写。

检查环境文件和 Compose 解析结果，再构建并启动：

```bash
./nz delivery check --env deploy/.env --compose
./nz delivery up --yes
./nz delivery smoke --url http://127.0.0.1
```

`delivery check` 会拒绝示例密码、过短密钥和缺失变量。`delivery up` 先执行 Compose 配置校验，再构建镜像；它根据 `NZ_REDIS_ENABLED` 和 `NZ_FILE_STORAGE_TYPE` 自动启用 `redis`、`storage` profile，不需要手工拼接 profile 参数。

启动顺序由健康检查控制：PostgreSQL 必须先就绪；启用 Redis 时后端还会等待 Redis；启用 S3 时后端会等待 MinIO 健康且 bucket 创建完成；后端的数据库、Flyway 和可选 Redis 就绪后再启动前端。可用性检查地址如下：

- 前端存活：`GET /healthz`
- 经前端代理检查后端就绪：`GET /health/ready`
- 后端原始就绪端点：`GET :8080/actuator/health/readiness`

首次成功登录后，将 `NZ_INITIALIZE_DATA` 改为 `false`。后续修改 `NZ_INITIAL_ADMIN_PASSWORD` 不会覆盖数据库中已有管理员密码。

## 可选 Redis 监控

在 `deploy/.env` 中设置：

```dotenv
NZ_REDIS_ENABLED=true
```

启动时会自动选择 Redis profile：

```bash
./nz delivery up --yes
```

未启用时，系统不会创建 Redis 监控提供器，基础监控和其他业务仍可运行。

## 可选 MinIO / S3 存储

在 `deploy/.env` 中设置随机的 `NZ_MINIO_ROOT_PASSWORD`，并修改：

```dotenv
NZ_FILE_STORAGE_TYPE=s3
NZ_S3_ENDPOINT=http://minio:9000
NZ_S3_BUCKET=nz-admin
```

启动时会自动选择 storage profile：

```bash
./nz delivery up --yes
```

`minio-init` 会等待 MinIO 健康后创建 bucket。管理员也可以在“系统管理 → 文件配置”维护 `local`、`oss` 或 `s3` 配置，并使用“测试连接”在启用前验证路径、bucket 与凭据。对应权限为 `system:fileconfig:test`。

## 日常运维

查看状态与日志：

```bash
./nz delivery ps
docker compose --env-file deploy/.env -f deploy/compose.yaml logs -f backend
```

停止服务但保留数据卷：

```bash
./nz delivery down --yes
```

PostgreSQL、后端端口和 MinIO 管理端口默认绑定 `127.0.0.1`，只有前端端口监听所有网卡。需要从其他主机直接访问时，分别修改 `NZ_POSTGRES_BIND`、`NZ_BACKEND_BIND` 或 `NZ_MINIO_BIND`，并在主机防火墙限制来源。所有容器日志默认轮转为 3 个 10 MB 文件，避免长期运行占满磁盘。

升级前先备份 PostgreSQL 和上传文件卷。Flyway 迁移只允许向前执行，生产配置已禁止 `clean`。如果部署环境不允许应用自动迁移，可按版本顺序执行 `nz-server/nz-app/src/main/resources/db/upgrade-p*.sql`。

回滚应用镜像时，不要回滚已经执行的数据库结构。应先确认旧版本能够兼容当前结构；否则从升级前备份恢复到一个新的数据库实例，再切换流量。

## 验收清单

- `./nz delivery check` 通过。
- 前后端镜像构建成功。
- PostgreSQL 健康，后端日志显示 Flyway 迁移完成。
- `/healthz` 与 `/health/ready` 返回成功。
- 使用 `default` 租户、`admin` 用户和首次启动密码完成登录。
- 文件配置页面能够保存配置并执行连接测试。
- 重启整套服务后数据库与上传文件仍然存在。

本切片只调整交付基础设施，不新增业务接口、页面、菜单或数据库结构，因此不需要新的 Flyway 版本和菜单权限。业务数据仍由现有 V1–V15 迁移和模块菜单负责。

CI 会解析基础、Redis 和 storage profile，并构建后端、前端镜像；生产部署仍需在目标主机执行冒烟检查。
