# 模块开发指南

面向在 `nz-admin` 中**新增一个带菜单、权限、CRUD 的业务模块**的最小流程。细节范式见 [crud-paradigm.md](crud-paradigm.md)。

## 1. 后端（nz-server）

1. **库表**：在 `nz-app/src/main/resources/db/init.sql`（或迁移脚本）中建表；本地可重复执行时注意幂等。若已有数据库且缺少工作台/运行监控菜单，可执行 `nz-app/src/main/resources/db/upgrade-p2-menus.sql`。
2. **实体**：在 `nz-module/nz-system`（或新业务模块）下按 [.docs/naming-convention-service-dao-and-model.md](../.docs/naming-convention-service-dao-and-model.md) 放置 `*DO`、`*Query`、`*VO`。
3. **Mapper**：`mapper/{域}/XxxMapper.java`，继承 `BaseMapper<XxxDO>`，复杂 SQL 用 default 方法或 XML。
4. **Service**：接口 + `XxxServiceImpl`，不继承 MyBatis-Plus `ServiceImpl`，业务层不出现 `LambdaQueryWrapper`。
5. **Controller**：`@RequestMapping("/api/{模块}/{资源}")`，方法语义与 [.docs/api-convention.md](../.docs/api-convention.md) 对齐，返回 `R<T>`。
6. **权限**：在 `SaCheckPermission` 上使用与菜单/按钮一致的 perm 字符串。
7. **种子菜单**：`init.sql` 中 `sys_menu` 插入目录/菜单/按钮；`sys_role_menu` 为管理员角色关联新菜单 id（若使用全量 CROSS JOIN 可省略单条）。
8. **验证**：`cd nz-server && ./mvnw -pl nz-app -am compile -DskipTests`（全量测试按仓库约定再开）。

## 2. 前端（nz-web）

1. **API**：`src/api/{模块}/{资源}.ts`，使用 `src/api/request.ts` 封装；类型可放在同文件或 `src/api/types.ts`。
2. **页面**：`src/views/.../index.vue` + `hooks.ts`，hook 命名 `useXxxCrud` 或与能力匹配的名称。
3. **路由**：动态路由由菜单 `component` 字段驱动（如 `system/user/index` → `@/views/system/user/index.vue`），一般**不必**改 `router/index.ts` 静态表；若需固定占位再在静态 children 中增加。
4. **按钮权限**：模板使用 `v-permission="'system:xxx:yyy'"`，与后端 `SaCheckPermission` 一致。
5. **验证**：`cd nz-web && pnpm test && pnpm build`（或 CI 等价命令）。

## 3. 权限标识命名

- 菜单级列表权限：`system:{资源}:list`（与菜单 `perm` 字段一致，用于路由可见性）。
- 按钮：`system:{资源}:query|add|edit|remove` 等动词后缀；特殊操作单独命名，如 `system:user:resetPwd`。
- 工作台、监控等能力：`system:workbench:view`、`system:monitor:query`。

## 4. 菜单配置

- **目录**：`type = 'M'`，`path` 为前端路由前缀（如 `/system`），`component` 为空。
- **菜单**：`type = 'C'`，`path` 为相对父级的段（如 `user`），`component` 为视图路径（如 `system/user/index`），**不要**带 `@/` 前缀。
- **按钮**：`type = 'F'`，`perm` 必填，`path`/`component` 通常为空。
- **排序**：`sort` 越小越靠前；根菜单排序决定侧栏顺序与「首个可访问路由」解析顺序。

## 5. 测试与文档

- 框架与分层测试思路见 [exec-plans/common-framework-test-plan.md](exec-plans/common-framework-test-plan.md)。
- 行为或结构变更时更新 [.docs/architecture.md](../.docs/architecture.md) 或本指南的引用关系；执行计划在 [exec-plans/ruoyi-alignment-plan.md](exec-plans/ruoyi-alignment-plan.md) 勾选。

## 6. UI 与可访问性

工作台、监控等聚合页遵循 [.docs/nz-web-ui-design-guide.md](../.docs/nz-web-ui-design-guide.md)：主题变量、卡片圆角、`app-page` 容器，避免硬编码主色。
