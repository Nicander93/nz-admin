# 开发指南

新增业务按“模块骨架 → 数据表 → API → 页面 → 权限 → 验收”推进。每一步都有现有约定和示例可参考。

## 1. 选择模块边界

业务规则放在 `nz-module`，可复用技术机制放在 `nz-framework/nz-starter-*`。公共模块只定义轻量协议，不引用具体业务实现。先阅读[架构说明](architecture.md)和[扩展模块体系](extension-module-system.md)。

用 CLI 查看待创建的文件：

```bash
./nz module add audit-center --title 审计中心 --parent-menu-id 1000 --menu-id 9300 --dry-run
```

核对代码、菜单 ID 和迁移版本后，改用 `--yes` 创建。CLI 保留修改备份，可按[项目 CLI](cli.md)回滚。模块开关修改后重启生效。

## 2. 建表与数据访问

新增连续 Flyway 迁移和同版本手工升级脚本，登记资源测试；不修改已执行的历史迁移。运行 `./nz migration check` 校验。[数据库迁移](database-migrations.md)说明升级顺序。

普通业务表要明确租户、部门和归属字段。表级规则分别接入[多租户](multi-tenancy.md)与[数据权限](data-permission.md)，验证列表、详情、更新和删除。框架的受管表审计能发现未注册的租户表。

## 3. 完成 API 与页面

[模块开发指南](module-development-guide.md)说明 DO、Query、VO、Mapper、Service 和 Controller 的落点；[CRUD 范式](crud-paradigm.md)提供纵向切片。[代码生成器](code-generator.md)可为 PostgreSQL 单主键表生成起点，仍需核对业务规则。

前端 API 放在 `src/api/{模块}`，页面状态放在页面同目录 `hooks.ts`，视图放在 `src/views`。[前端约定](frontend-coding-conventions.md)说明命名与请求封装。

菜单组件路径驱动动态路由，模块清单声明组件前缀。前端 `v-permission` 控制按钮显示，后端 `@SaCheckPermission` 负责授权，两个位置使用相同权限字符串。

## 4. 接入通用能力

按业务需要选择能力：

- 申请审批：实现业务启动和回调接口，参考[工作流](workflow.md)与 demo 请假。
- 文件上传：使用[文件配置与存储](file-configuration.md)，避免页面持有存储密钥。
- 通知提醒：参考[站内消息](message-center.md)、[邮件](mail.md)、[短信](sms-management.md)。
- 在线事件：通过[实时通信](realtime-communication.md)的发布端口发送；持久化历史与在线推送分别处理。
- 敏感字段：[字段加密](field-encryption.md)说明存储与密钥轮换边界。

## 5. 验证和同步文档

```bash
./nz migration check
./nz verify
cd docs
pnpm install --frozen-lockfile
pnpm build
```

权限、幂等和事务行为用服务端测试验证；核心用户路径按需用真实数据库与浏览器测试验收。方法见[测试指南](testing.md)。涉及业务行为时更新对应功能文档，涉及框架边界时更新[架构说明](architecture.md)。文档站严格检查站内链接，新增页面需同步侧栏和能力入口。

上线前阅读[部署指南](deployment.md)，核对迁移、配置、数据持久化和恢复方案。
