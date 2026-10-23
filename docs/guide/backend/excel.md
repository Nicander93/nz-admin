# Excel 数据交换

`nz-starter-excel` 基于 EasyExcel 提供模板和响应写出能力。Demo 条目用它演示一条可复用的导入导出链路：先预检、展示逐行问题、用户确认后原子写入；导出则沿用列表筛选、租户隔离和数据范围。

## 示例条目接口

| 接口 | 权限 | 用途 |
| --- | --- | --- |
| `GET /api/demo/item/import/template` | `demo:item:import` | 下载 `.xlsx` 模板 |
| `POST /api/demo/item/import/preview` | `demo:item:import` | multipart 字段 `file`，只解析和校验，不写数据库 |
| `POST /api/demo/item/import` | `demo:item:import` | JSON 有效行数组；必须传 `Idempotency-Key` |
| `GET /api/demo/item/export` | `demo:item:export` | 按 `name`、`category`、`status` 筛选下载 |

V31 会为默认管理员角色和租户套餐添加导入、导出按钮权限。没有对应权限的账号不能调用接口。部署已有数据库时先执行 V31 迁移。

预检返回标准 `R` 响应，`data` 含总行数、有效行数、有效数据和逐行错误：

```json
{
  "total": 2,
  "validCount": 1,
  "rows": [{ "name": "示例条目", "category": "general", "status": 0, "sort": 10, "remark": "备注" }],
  "errors": [{ "rowNum": 3, "message": "状态值不正确" }]
}
```

错误行号按 Excel 中的实际行号计算（表头为第 1 行），中间空白行不会导致偏移。状态或排序不是整数时，会标出该行和列，再继续预检后面的行；有任意错误时页面不允许确认导入。模板要求“条目名称、分类、状态（0启用，1停用）、排序（0-999）、备注”五列表头。文件最大 10 MB，一次最多 1000 行；空文件、空数据、缺表头、非法 xlsx 和字段错误会被拒绝或逐行报告。

## 导入安全与事务边界

预检结果只是给操作者看的反馈，不能作为服务端授权凭据。确认接口只接受名称、分类、状态、排序和备注；请求中的 `id`、租户和 owner 不属于导入模型。服务端对整个 JSON 数组重新执行 Bean Validation，并从当前登录上下文写入 owner。租户列由现有租户填充组件写入。

任意一行无效时整批不写入。全部通过后，所有插入在一个数据库事务中完成；任一 SQL 插入失败，整批回滚。确认接口要求 `Idempotency-Key`：网络超时重试同一批数据时复用原键；成功后清除键，下一批生成新键。修改了数据就使用新的请求键。

这里采用只新增的语义，不用上传的 ID 覆盖已有记录。若业务需要“更新或新增”，应另行设计带冲突预览、更新权限和可审计字段的接口，不能悄悄改变本接口的含义。

## 导出范围和上限

导出使用和条目列表相同的 MyBatis 查询条件，并继续经过租户与数据范围拦截器。响应是标准 `.xlsx` Blob。单次导出最多 5000 行，超过时服务端要求进一步筛选，避免将任意全表载入内存。文本型字段对以 `= + - @`、制表符或回车开头的内容增加文本前缀，防止表格软件把用户内容当成公式执行。

新增模块中的 Excel 接口时，建议将 EasyExcel 读写逻辑放在服务层，把导入行、导出行定义成独立模型；不要把实体直接作为导入对象。权限应分别控制模板/预检/写入与导出。对大文件使用严格的大小、行数上限，并保留 Apache POI 默认 ZIP 安全保护，不要调低压缩比阈值来接受可疑压缩包。

## 验证

- `DemoItemExcelImportTest` 用真实 EasyExcel 工作簿检查读取、逐行约束错误、非法文件和行数上限。
- `DemoItemExcelPostgresTest` 在显式配置的独立 PostgreSQL 测试库上走真实 HTTP 安全、幂等、事务和数据范围路径。
- Playwright `tests/e2e/demo/item-excel.spec.ts` 覆盖上传错误文件、有效预览、确认导入、模板下载和筛选导出。
