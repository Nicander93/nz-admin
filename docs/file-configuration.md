# 文件配置与生命周期

文件管理位于“系统管理 / 文件管理”，配置位于“系统管理 / 文件配置”。每个租户独立选择当前配置；新文件保存上传时的位置和密文凭据快照。修改目录、bucket、前缀或启用另一条配置，只影响后续上传。旧文件的下载和删除继续使用原快照。

## 配置与接入

新建配置默认不启用，同一租户只能有一个生效配置。可以先测试未启用配置的连接，再启用；编辑生效配置后，该租户下一次上传使用新参数。文件服务不修改共享的 `FileStorageProperties`，请求参数也不能覆盖当前租户的存储选择。

没有数据库配置时，使用部署环境的默认配置，为每个文件创建独立快照。多节点使用本地存储时，需要共享同一个上传目录；目录各自独立的节点不能相互读取文件。

本地配置需要 `basePath`，对象存储需要 endpoint、bucket 和完整凭据；S3/MinIO 可设置 region。单文件大小必须大于 0，扩展名与 MIME 仍受部署白名单限制。

接口前缀为 `/api/system/file-config`，权限包括 `system:fileconfig:list/query/add/edit/remove/test`。生效配置不能直接删除；停用配置删除后，已上传文件仍保留自己的快照。

## 上传与失败恢复

上传先校验文件并分配目标位置，再独立提交 `UPLOADING` 记录，然后持有该记录的行锁写入对象。在当前业务事务中将记录更新为 `READY`；外层事务回滚时会删除对象并清理记录。

进程在写入中途退出时，数据库仍保留目标位置。恢复任务清理超过 30 分钟的未完成上传；它使用 `FOR UPDATE SKIP LOCKED` 跳过仍被活跃事务锁住的记录。网络上传会占用数据库事务和行锁，接入大文件业务前需要评估连接池和上传耗时。

批量上传逐项处理，响应包含成功项与失败项，以及失败项在原请求中的索引。页面保留失败文件供重试，不会把部分成功显示为全部成功。

删除先提交 `DELETE_PENDING` 状态，再删除物理对象和数据库记录。存储或数据库故障时保留待清理记录，页面可重试删除；后台每分钟重试到期记录。缺失的对象允许重复删除，待清理文件不能继续下载。通过 `nz.file.cleanup-interval-ms` 可以调整扫描间隔。

## 下载与权限

下载按文件 ID 校验当前身份、租户和权限。前端通过携带 Authorization 的请求获取附件，本地、OSS 和 MinIO 均由应用返回文件流，避免浏览器直接打开受保护 URL 或跟随对象存储跨域重定向。

文件权限为：

- `system:file:list`：文件列表。
- `system:file:upload`：单文件与批量上传。
- `system:file:query`：下载与查询当前存储类型；兼容已有 `system:file:download/storage-type` 授权。
- `system:file:remove`：删除及重试；兼容已有 `system:file:delete` 授权。

文件管理是租户内由权限控制的管理功能；业务需要“仅上传者可见”时，应在业务文件关联表中增加所有权检查。

## 扩展存储适配器

业务文件按快照创建策略实例。需要替换内置策略时，提供名为 `localFileStorageAdapterFactory`、`ossFileStorageAdapterFactory` 或 `s3FileStorageAdapterFactory` 的 `FileStorageAdapterFactory` Bean，按传入的独立配置创建实例。原来的 `*FileStorageService` Bean 仍供直接注入使用；业务文件服务使用工厂扩展点。

可恢复上传需要实现 `prepareUpload` 和 `writeUpload`：前者只分配位置，后者写入该位置；`delete` 必须允许目标已不存在。框架不应依赖业务表，也不应在策略中修改全局配置。

## 升级与历史文件绑定

Flyway V33 对齐早期 `sys_file` 的列名和数字存储类型，并增加生命周期与配置快照字段。使用 Flyway 的部署自动迁移；手工升级部署执行 `db/upgrade-p33-file-storage-lifecycle.sql`。V32 身份版本升级仍须先执行。

升级前的文件没有可信配置快照。已有记录无法还原过去使用过的密钥或 bucket，迁移不会猜测当前配置就是原配置。管理员在文件列表选择“绑定原配置”，确认原目录或对象存储后再绑定；绑定需要同时具有 `system:fileconfig:edit` 和 `system:file:upload`。接口为 `PUT /api/system/file/{id}/storage-binding/{configId}`，只允许绑定本租户、相同存储类型且尚未绑定的记录，并记录操作日志。

批量历史数据应先核对原配置和样本文件，再分批绑定。不要删除仍需绑定的原配置或原存储中的文件。

## 密钥与验证范围

AccessKey Secret 使用 AES-GCM 加密，主密钥来自 `NZ_FILE_CONFIG_KEY`。持久化快照不包含主密钥，文件接口不返回快照。没有主密钥时本地存储仍可使用，保存对象存储凭据会失败。

主密钥同时用于历史快照。轮换需要重加密配置表和所有历史快照，单独重新保存当前配置不能完成轮换；丢失旧密钥将无法访问依赖它的历史对象。

回归入口是 `FileLifecyclePostgresTest`、`FileStorageFactoryTest`、`FileConfigServiceImplTest` 和存储适配器测试。真实 PostgreSQL 测试需设置 `NZ_TEST_PG_URL`、`NZ_TEST_PG_USERNAME` 和 `NZ_TEST_PG_PASSWORD`。本地文件、模拟对象存储与真实对象存储的验证结果应分别记录。
