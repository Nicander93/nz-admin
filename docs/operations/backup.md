# 备份、恢复与应用回滚

可用的备份需要证明能够恢复。数据库、上传文件、对象存储凭据与字段加密密钥是分别保存的资源，漏掉其中一类就可能无法恢复业务。

## 备份内容

| 内容 | 保存什么 |
| --- | --- |
| PostgreSQL | 业务数据、索引、约束、Flyway 历史 |
| 文件 | 本地上传卷或对象存储内容 |
| 密钥 | 文件配置主密钥、字段加密所需密钥 |
| 应用 | 提交或镜像版本、部署配置来源 |

密码通过受保护的客户端配置或交互输入，不写入公开命令记录。升级前记录迁移版本和关键表行数，保留备份校验信息。

## 恢复到独立数据库

配置 PGHOST、PGPORT、PGUSER 后执行：

```bash
pg_dump --format=custom --no-owner --dbname=nz_admin --file=nz-admin.dump
createdb nz_admin_restore
pg_restore --exit-on-error --no-owner --dbname=nz_admin_restore nz-admin.dump
psql --dbname=nz_admin_restore --command="SELECT version, success FROM flyway_schema_history ORDER BY installed_rank"
```

库名按环境替换，恢复目标必须是新库。不要直接覆盖正在运行的业务库，也不要让恢复演练应用错误连接生产 Redis/对象存储。

## 恢复后验收

比较关键表行数与数据摘要、索引/约束、迁移历史；恢复对应文件和密钥；启动匹配版本应用，验证登录、权限、文件读取和审批轨迹。恢复旧流程时同时保留 legacy 表与事件历史。

此前记录的 V1–V29 演练不能代替当前 V30 环境的恢复验收；新增表和业务快照应加入实际演练。参考[部署记录](/deployment)。

## 回滚应用

数据库迁移只向前，回滚镜像前先判断旧版本是否兼容当前结构。不能兼容时，将升级前备份恢复到另一个实例、完成验收后切换流量。文件和数据库需要使用一致的恢复时间范围。

审批事件属于至少一次投递，恢复后也要检查未完成事件与外部副作用幂等。不要清空事件表以减少积压，那会失去业务状态和审计依据。
