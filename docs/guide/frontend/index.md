# 前端开发

前端按“API 类型 → 页面状态 → 模板 → 菜单授权”接入业务。数据请求和状态转换集中在 hook，页面负责表达操作结果。

## 选择下一步

| 当前任务 | 指南 |
| --- | --- |
| 页面已经写好但菜单打不开 | [动态路由与模块清单](/guide/frontend/routing) |
| 新增分页列表与表单 | [CRUD 页面与 hooks](/guide/frontend/crud-page) |
| 调用接口、下载文件或处理重试 | [请求、下载与错误](/guide/frontend/request) |
| 核对命名和拆分规则 | [前端编码规范](/frontend-coding-conventions) |
| 验证实际页面行为 | [浏览器验收](/guide/testing/browser) |

## 最小目录

```text
src/api/<module>/<resource>.ts
src/views/<module>/<resource>/index.vue
src/views/<module>/<resource>/hooks.ts
src/modules/<module>/manifest.ts
```

小页面不需要预先拆出多层 components、composables 和 types。出现多个独立区域或真实复用需求后，再局部拆分。已有系统管理页面可作为样板，树表、分页 CRUD 和只读列表分别选同类参考。

## 权限与数据边界

`v-permission` 控制操作可见性，动态菜单控制入口，后端负责授权和数据隔离。页面不能通过筛选已下载的全量数据实现权限；未经授权的数据应在服务端查询时被排除。

页面交付时，用具有不同菜单、按钮与数据范围的普通用户检查，不只使用管理员。模块关闭后也应检查路由过滤。继续阅读[菜单与按钮配置](/platform/system/menus)。
