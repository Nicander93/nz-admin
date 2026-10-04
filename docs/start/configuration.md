# 配置文件与生效方式

先分清“应用配置”和“Compose 配置”。修改后端 YAML 会影响应用；修改 `deploy/.env` 只有在对应变量被传给容器时才影响容器。前端页面上的系统参数也不能替代这两类配置。

## 配置放在哪里

| 位置 | 管什么 | 修改后 |
| --- | --- | --- |
| `application.yml` | 共用配置、模块开关、框架能力 | 重启后端 |
| `application-dev.yml` | 本地开发数据库、开发日志 | dev profile 下重启 |
| `application-prod.yml` | 生产运行配置 | prod profile 下重启 |
| `deploy/.env` | Compose 服务、端口、生产变量 | 按交付流程重新应用配置 |
| 系统参数页面 | 代码明确读取的业务参数 | 取决于对应读取实现 |

默认 profile 来自 `SPRING_PROFILES_ACTIVE`，未提供时为 dev。Java 启动参数也可明确覆盖配置，例如 `--server.port=8081`，适合在同机验证第二节点。

## 常用开关

| 环境变量 | 默认值 | 含义 |
| --- | --- | --- |
| `NZ_WARM_FLOW_ENABLED` | false | 开启新流程引擎；先完成 V30 迁移 |
| `NZ_CLUSTER_ENABLED` | false | 使用共享集群状态 |
| `NZ_REDIS_ENABLED` | false | Redis 监控与 readiness；集群需要同时开启 |
| `NZ_CACHE_KEY_PREFIX` | nz-admin | Redis 状态命名空间；同一集群一致 |
| `NZ_TENANT_VALIDATE_TABLES` | true | 启动时检查租户表登记 |
| `NZ_INITIALIZE_DATA` | true | 系统初始化；生产首次完成后改为 false |

`nz.modules.demo.enabled`、`nz.modules.generator.enabled` 等是后端模块属性。修改属性后重启，不支持在线卸载。不要假定所有模块属性都有同名 `NZ_*` 快捷变量；以 YAML 中的变量占位和模块自动配置为准。

## 本地通过环境变量启用审批

```bash
export NZ_WARM_FLOW_ENABLED=true
cd nz-server
./mvnw -pl nz-app spring-boot:run
```

Windows PowerShell 使用 `$env:NZ_WARM_FLOW_ENABLED = 'true'`，然后在该终端启动后端。已经运行的进程不会读取新终端的变量。

## 配置没有生效时

1. 确认请求实际到达哪一个端口、进程或容器。
2. 确认当前 profile，避免改了 dev 却运行 prod。
3. 确认变量已传给启动进程，且名称在 YAML 中存在。
4. 重启后检查 readiness 和目标功能，不输出整份含密钥的环境配置。

生产密码、文件配置密钥、对象存储和双节点要求见[配置清单](/operations/configuration)。
