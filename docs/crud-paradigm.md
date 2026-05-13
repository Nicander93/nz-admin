# 前后端 CRUD 范式

本文与 [frontend-coding-conventions.md](frontend-coding-conventions.md)、[.docs/api-convention.md](../.docs/api-convention.md) 一致，描述本仓库**推荐**的增删改查写法与少量例外。

## 后端

- **分层**：`Controller` → `Service` → `Mapper`，禁止跨层；约定见 [.docs/architecture.md](../.docs/architecture.md)。
- **URL**：`/api/{模块}/{资源}`，资源名小写单数；标准 CRUD 见 [.docs/api-convention.md](../.docs/api-convention.md) 表格（`GET .../page`、`GET .../list`、`GET .../{id}`、`POST` / `PUT` / `DELETE`）。
- **响应**：统一 `R<T>`，`code == 200` 表示成功；分页体为 MyBatis-Plus `Page<T>`（与前端 `PageResult` 对应）。
- **特例**：定时任务（cron 校验、暂停、恢复、立即执行）、文件上传、在线用户列表等允许偏离「纯 REST 动词表」，须在模块内保持命名可读，并在本文或模块开发指南中点名说明。

## 前端

### 页面目录

`nz-web/src/views/{域}/{资源}/index.vue` + `hooks.ts`，见 [frontend-coding-conventions.md](frontend-coding-conventions.md)。

### Hook 返回结构

默认对外暴露三段：

- `table`：列表/树、分页、查询条件、`loadData` / `refresh`、loading 等。
- `form`：弹窗表单、`openAdd` / `openEdit` / `close`、`submit` 相关状态。
- `actions`：`submit`、`remove` 等与用户动作相关的方法。

**复杂页**：可在三段之外追加业务段，例如用户管理的 `dept`、`role`、`posts`，但**不要**用另一套名字替代 `table`/`form`/`actions`。

**只读列表页**（无表单新增/编辑）：可不返回 `form`，模板只解构 `table` 与 `actions`；参考 `nz-web/src/views/system/file/`。

### 实现路径

- **分页 CRUD**：优先 `useCrud`（`@/utils/CRUD`）组装 `table` / `form` / `actions`。
- **树表**：可用 `useForm` + 自建 `table.treeData` + `buildTree`，参考 `nz-web/src/views/system/menu/hooks.ts`、`nz-web/src/views/system/dept/hooks.ts`。

### API 层

- 按资源拆文件，如 `nz-web/src/api/system/user.ts`。
- 函数命名与 [.docs/api-convention.md](../.docs/api-convention.md) 中「前端函数命名约定」表一致（`pageUsers`、`listDepts` 等）。
- `nz-web/src/api/system/index.ts` 按需 `export *` 或具名导出，新增资源时记得补 barrel，避免同目录导入风格分裂。

## 代表页面（样板）

| 类型 | 路径 |
|------|------|
| 分页 CRUD | `nz-web/src/views/system/notice/`、`nz-web/src/views/system/role/` |
| 树表 | `nz-web/src/views/system/menu/`、`nz-web/src/views/system/dept/` |
| 复杂权限与多子业务 | `nz-web/src/views/system/user/` |
| 只读列表 | `nz-web/src/views/system/file/` |

后端对应 Controller 位于 `nz-server/nz-module/nz-system/.../controller/` 下与资源同名的子包。
