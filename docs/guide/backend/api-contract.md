# 接口与分页契约

前端请求封装依赖统一的 `R<T>`，分页组件依赖 `PageResult<T>`。新接口保持这两个契约，页面才能复用现有 hook 和错误处理。

## URL 和方法

业务资源通常使用 `/api/{模块}/{资源}`，例如 `/api/demo/item`。

| 操作 | 方法与路径 | 常见返回 |
| --- | --- | --- |
| 分页 | `GET /api/demo/item/page` | `R<PageResult<DemoItemVO>>` |
| 详情 | `GET /api/demo/item/{id}` | `R<DemoItemVO>` |
| 新增 | `POST /api/demo/item` | 新记录 ID |
| 修改 | `PUT /api/demo/item` | `R<Void>` |
| 删除 | `DELETE /api/demo/item/{id}` | `R<Void>` |

特殊动作可有独立路径，如请假提交、任务暂停；动作必须有明确业务语义与权限。不要为了统一 CRUD 把审批动作变成任意状态字段更新。

## 成功响应与分页参数

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [],
    "total": 0,
    "size": 10,
    "current": 1,
    "pages": 0
  }
}
```

请求参数使用 `pageNum`、`pageSize`；前端类型在 `src/api/types.ts`。后端可用 `PageResult.of(page, convertedRecords)` 保留总数和页码，同时转换 DO 为 VO。不要只转换 records 而丢掉分页元数据。

## 三类对象分别处理

- CreateRequest / UpdateRequest 接收允许客户端设置的字段。
- DO 表达存储字段，包括服务端管理的租户、归属和审计信息。
- VO 表达页面需要的响应，避免返回密码、凭据或框架内部状态。

新 CRUD 按这个边界编写。仓库部分历史接口仍返回 DO，不能据此假定所有字段都适合公开；新增接口应主动限制返回字段。

## 业务码与 HTTP 状态

`code == 200` 才是统一响应成功。当前业务异常通常通过 `R.fail` 表达，HTTP 200 并不一定代表业务成功。明确的传输层异常（例如实时连接鉴权失败）保留 HTTP 401/403 等状态。

API 自动化应同时断言 HTTP、业务码和实际数据。文件下载返回 Blob/文件流，不能套普通 JSON 解包；见[请求与下载](/guide/frontend/request)。

## 权限落点

Controller 使用项目的 `com.nz.admin.framework.auth.annotation.SaCheckPermission`，权限字符串与菜单按钮一致。Service 再验证目标记录归属、状态和可操作范围。完整示例参考 [DemoItemController](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-demo/src/main/java/com/nz/admin/modules/demo/controller/DemoItemController.java)。

继续阅读[参数校验](/guide/backend/validation)与[持久化](/guide/backend/persistence)。
