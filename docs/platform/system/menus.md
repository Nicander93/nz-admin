# 菜单、按钮与页面接入

菜单配置同时决定导航层级、动态路由和权限标识。一个新页面至少要有菜单 C；受控操作另外用按钮 F 声明权限。

## 三种菜单类型

| type | 类型 | path / component | perm |
| --- | --- | --- | --- |
| M | 目录 | path 为目录前缀，component 为空 | 按目录需要配置 |
| C | 页面 | path 为页面段，component 为视图文件路径 | 页面列表权限 |
| F | 按钮 | 通常为空 | 操作权限必填 |

例如 demo/item 页面 component 为 `demo/item/index`，对应 `src/views/demo/item/index.vue`。父目录 `/demo` 加子页面 `item` 形成 `/demo/item`。sort 越小越靠前；菜单隐藏和停用是不同概念，应核对实际字段与前端结果。

## 接入一组权限

页面列表：`demo:item:list`；详情、新增、修改、删除分别使用 query、add、edit、remove 后缀。后端使用项目 SaCheckPermission，前端使用相同字符串：

```vue
<el-button v-permission="'demo:item:edit'">修改</el-button>
```

数组权限的当前前端指令取“任一满足”。不要把它误解成所有权限同时满足；后端组合权限也要明确 AND/OR 语义。隐藏按钮不是接口授权。

## 菜单进入数据库后还要授权

新迁移加入目录、页面和按钮，并按需求更新角色菜单与租户套餐。只给默认管理员分配，不代表其他租户套餐自动获得；套餐限制还会与角色权限求交集。

生成器的菜单 SQL是起点，纳入正式版本迁移后再交付。CLI 创建模块时可以指定父菜单和菜单 ID；先 dry-run，检查冲突后写入。

## 页面缺失的排查顺序

模块已装配且启用 → 套餐包含菜单 → 角色已分配 → component 对应真实文件 → 前端清单前缀正确。修改后重新登录，再用该用户直接调用接口检查授权。

实现机制见[动态路由](/guide/frontend/routing)，模块生成流程见[模块开发](/module-development-guide)。
