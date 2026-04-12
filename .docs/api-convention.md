# API 接口命名规范

参考阿里巴巴 Java 开发手册中的 RESTful API 设计约定，结合本项目实际情况整理。

## URL 路径规则

- 基础路径：`/api/{模块}/{资源}`，例如 `/api/system/user`
- 资源名使用**小写单数**名词
- 路径中不出现动词（动作语义由 HTTP Method 承担），特殊操作除外

## HTTP Method 与操作映射

| 操作 | Method | URL 示例 | 说明 |
|------|--------|----------|------|
| 分页查询 | GET | `/api/system/user/page` | 必须带分页参数（pageNum, pageSize），返回 `Page<T>` |
| 列表查询 | GET | `/api/system/user/list` | 不分页，返回全量数组 `List<T>`；适用于下拉选择等场景 |
| 详情 | GET | `/api/system/user/{id}` | 按主键获取单条记录 |
| 新增 | POST | `/api/system/user` | RequestBody 传入实体 |
| 修改 | PUT | `/api/system/user` | RequestBody 传入实体（含 id） |
| 删除 | DELETE | `/api/system/user/{id}` | 路径传入主键 |

### page 与 list 的区分

- **page**：分页查询，请求参数中必须包含 `pageNum` 和 `pageSize`，响应体为 MyBatis-Plus 的 `Page<T>` 结构（含 `records`、`total`、`current`、`size`、`pages`）
- **list**：列表查询，不携带分页参数，响应体为 `List<T>` 数组，仅用于数据量可控的场景（如字典、下拉选项）

## 统一响应结构

所有接口统一使用 `R<T>` 包装返回值：

```json
{
  "code": 200,
  "msg": "success",
  "data": T
}
```

分页接口的 `data` 字段为 `Page<T>`：

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

## 前端类型对应

| 后端类型 | 前端类型 | 说明 |
|---------|---------|------|
| `R<T>` | `R<T>` | 统一响应包装 |
| `Page<T>` | `PageResult<T>` | 分页数据结构 |

前端类型定义位于 `src/api/types.ts`。

## 前端函数命名约定

| 操作 | 函数命名模式 | 示例 |
|------|------------|------|
| 分页查询 | `page{资源}s` | `pageUsers(params)` |
| 列表查询 | `list{资源}s` | `listUsers(params)` |
| 详情 | `get{资源}` | `getUser(id)` |
| 新增 | `add{资源}` | `addUser(data)` |
| 修改 | `update{资源}` | `updateUser(data)` |
| 删除 | `delete{资源}` | `deleteUser(id)` |
