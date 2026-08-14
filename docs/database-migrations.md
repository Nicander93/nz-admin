# 数据库迁移

nz-app 使用 Flyway 管理 PostgreSQL 数据库结构和基础菜单权限，迁移文件位于
src/main/resources/db/migration。

## 版本

- V1__baseline.sql：完整新库基线。
- V2__menus.sql：工作台和运行监控菜单。
- V3__client_module.sql：客户端管理表、菜单和权限。
- V4__job_module.sql：独立任务模块前端路由。
- V5__mail.sql：邮件测试菜单和权限。
- V6__file_config.sql：文件配置表、唯一生效约束、菜单和权限。
- V7__demo_module.sql：可删除 demo 模块的数据表、菜单和权限。
- V8__generator_module.sql：代码生成菜单和预览、下载权限。
- V9__tenant_management.sql：租户与套餐、现有表租户字段、租户内唯一约束和菜单权限。
- V10__field_encryption.sql：用户敏感字段加密和密钥版本。
- V11__s3_storage_delivery.sql：S3 兼容存储配置。
- V12__online_user_management.sql：在线用户路由、按钮权限和历史权限收敛。
- V13__realtime_communication.sql：SSE/WebSocket 控制台、菜单和测试发送权限。
- V14__sms_management.sql：短信渠道、模板、发送记录、默认日志渠道和管理权限。

- V15__sms_login.sql：手机号检索摘要、账号与短信登录客户端及授权类型约束。
- V16__social_login.sql：第三方账号绑定、social 登录客户端、菜单和按钮权限。
- V17__message_center.sql：逐用户站内消息、已读状态、菜单和按钮权限。
- V18__user_profile.sql：当前用户性别和受保护头像文件引用。
- V19__workflow_category.sql：租户流程分类、默认根分类、工作流菜单和按钮权限。
- V20__workflow_definition.sql：流程定义版本链、发布状态、模型 JSON、定义菜单和权限。
- V21__workflow_instance.sql：流程实例快照、当前节点、运行轨迹、实例菜单和权限。
- V22__workflow_task.sql：当前待办、历史已办、逐用户抄送、存量任务回填、任务菜单和权限。
- V23__workflow_task_delegate.sql：任务委派原办理人、受托归还状态、委派历史动作和按钮权限。
旧的 db/init.sql 与 db/upgrade-p*.sql 暂时保留用于人工部署兼容，内容必须与对应
Flyway 文件同步；新变更只应新增 Flyway 版本，不修改已经发布的版本。

## 新库与已有库

新库会从 V1 开始依次执行。已有非空数据库启用 baseline-on-migrate，首次启动时以
版本 1 建立基线记录，再执行后续幂等迁移。生产环境升级前仍应备份数据库，并先在
同结构副本验证。

Flyway 默认开启校验。若已发布迁移的校验和发生变化，应用会拒绝启动，此时应新增
更高版本迁移修正，不能直接改旧脚本或随意执行 repair。

## 验证

    cd nz-server
    JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw -pl nz-app -am test

项目根目录可以先执行结构检查：

    ./nz migration check

`module add` 会同时创建下一版 Flyway 文件和对应手工升级脚本，并更新迁移资源测试。
