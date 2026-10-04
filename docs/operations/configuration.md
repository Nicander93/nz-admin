# 生产配置清单

生产配置以 `deploy/.env.example`、Compose 和后端 prod YAML 为准。复制示例只是起点，不能把 replace-* 占位当成有效凭据。

## 关键配置

| 配置 | 要求 |
| --- | --- |
| NZ_DB_NAME / USERNAME / PASSWORD | 使用专属库和权限，替换密码 |
| NZ_INITIAL_ADMIN_PASSWORD | 首次创建管理员前设置；不会覆盖已有密码 |
| NZ_INITIALIZE_DATA | 首次成功初始化后关闭 |
| NZ_FILE_CONFIG_KEY | 长期保存，用于已存文件配置凭据解密 |
| NZ_FIELD_ENCRYPTION_ENABLED / KEY | 启用敏感字段加密时配置，保留旧密钥读取策略 |
| NZ_FILE_STORAGE_TYPE | 单节点 local 或共享对象存储 |
| NZ_WARM_FLOW_ENABLED | V30 后按需启用 |
| NZ_CLUSTER_ENABLED / REDIS_ENABLED | 多节点同时启用 |
| NZ_CACHE_KEY_PREFIX | 同集群一致，不同环境分开 |

Redis 的 host、port 等属性在应用 YAML 中也有环境变量入口；自建集群应核对实际连接和数据库编号。不要因为 NZ_REDIS_ENABLED 为 true 就认为所有节点一定连接同一 Redis。

## 交付检查

```bash
cp deploy/.env.example deploy/.env
./nz delivery check --env deploy/.env --compose
```

编辑 `.env` 完成所有必要值后再执行检查。通过后按[生产部署](/deployment)启动；生产 secrets 不提交到 Git。

## 端口与存储

PostgreSQL、后端和 MinIO 管理端口默认绑定本机。需要远程访问时按实际网关与防火墙调整，避免把数据库端口当成业务公开入口。前端代理 `/api` 和 `/realtime`，自定义代理时保留实时连接规则。

本地文件卷与数据库分开备份。多节点不使用互不共享的本地上传目录；切换文件配置不会自动搬迁历史文件，旧文件按原 storageType 访问。

## 密钥丢失或轮换

数据库备份不包含外部保存的加密密钥。NZ_FILE_CONFIG_KEY 丢失会导致已有对象存储 secret 无法解密；字段加密密钥也要随数据恢复。轮换前确认对应机制、旧数据和回退路径，不简单覆盖环境值。

具体文件配置见[存储手册](/file-configuration)，字段密钥见[字段加密](/field-encryption)，恢复演练见[备份恢复](/operations/backup)。
