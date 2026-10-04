# CRUD 页面与 hooks

一个普通管理页面只需明确列表状态、表单状态和用户动作。仓库统一使用 `table`、`form`、`actions`，让相邻页面容易读懂。

## 选对样板

| 页面类型 | 参考目录 | 复用方式 |
| --- | --- | --- |
| 分页列表与弹窗 | `src/views/system/notice/`、`role/` | useCrud |
| 部门或菜单树 | `src/views/system/dept/`、`menu/` | useForm + 树数据 |
| 只读文件列表 | `src/views/system/file/` | table + actions |
| 多区域权限配置 | `src/views/system/user/` | 保留业务区域，统一主要返回结构 |

先运行同类页面，再复制它的 API 适配与状态结构。不要只复制模板而遗漏查询重置、提交刷新、删除确认和 loading。

## hook 的职责

`hooks.ts` 调用 API，保存查询条件、分页和表单模型，并把 CRUD 工具的状态适配为页面对象。现有 notice hook 的对外结构如下：

```ts
return {
  table: tableView,
  form: formView,
  actions: actionsView,
}
```

模板通常调用 `form.openAdd`、`form.openEdit`、`actions.submit` 和 `actions.remove`。注意工具内部的 `toAdd/toEdit` 与页面对外的 `openAdd/openEdit` 经过适配，不能猜测签名。

## 编辑与提交

打开编辑时复制当前记录到表单，避免输入立即修改列表行。需要完整详情的页面先查详情；提交时只发送 DTO 接受的字段。关闭弹窗不代表请求成功，保存成功后才提示、关窗与刷新。

查询条件改变时考虑重置页码，删除最后一行后检查当前页是否仍有效。纯日期保持 `YYYY-MM-DD`，日期回填与提交值必须一致；请假浏览器验收覆盖了退回修改后的日期回填。

## 模板与权限

```vue
<el-button v-permission="'demo:item:add'" @click="form.openAdd">
  新增
</el-button>
```

UI 确认框放在页面事件适配中，业务请求保留在 hook。没有表单的只读页可不返回 form，不需要为了三段结构创建空对象。

实现参考：[notice hook](https://github.com/Nicander93/nz-admin/blob/master/nz-web/src/views/system/notice/hooks.ts)、[CRUD 工具入口](https://github.com/Nicander93/nz-admin/blob/master/nz-web/src/utils/CRUD.ts)。测试时检查 API 参数、失败保留输入、成功刷新和权限按钮，见[测试分层](/guide/testing/)。
