# 动态路由与模块清单

新增业务页面通常不用手工添加静态路由。后端菜单声明路径和组件，前端通过模块清单判断页面所属模块，再加载对应 Vue 文件。

## 菜单到页面的映射

```text
菜单 component: demo/item/index
页面文件: src/views/demo/item/index.vue
模块清单 componentPrefix: demo
```

component 使用视图路径，不带 `@/` 或文件系统绝对路径。菜单 path 决定浏览器 URL，component 决定加载哪个文件，两者职责不同。子菜单相对 path 会与父路径拼接；绝对 path 按当前路由工具处理为完整路径。

## 模块清单

```ts
import type { FrontendModuleManifest } from '@/core/modules/types'

const manifest: FrontendModuleManifest = {
  code: 'demo',
  title: '示例模块',
  componentPrefix: 'demo',
}

export default manifest
```

新业务清单放在 `src/modules/<code>/manifest.ts`，由注册表发现。后端同 code 的模块要提供自动配置和 `META-INF/nz/module.yaml`。不要只新增前端清单却没有后端可用模块。

## 登录后发生什么

路由守卫读取令牌，初始化用户信息、菜单与启用模块列表。`buildDynamicChildrenRoutes` 过滤禁用模块和找不到组件的菜单，再向 Root 添加动态子路由。首次加载会重新解析目标 URL，所以登录后直接打开深层业务地址也需要正确的菜单数据。

登录页和 OAuth 回调是公开静态入口。多数业务页走动态菜单；少量已有固定页面在静态路由中注册，不能把这类历史例外当成新增业务的默认方式。

## 菜单存在但页面打不开

按这个顺序查：模块是否启用 → 租户套餐是否包含 → 角色是否分配 → component 拼写与大小写 → 文件是否进入 Vite glob。控制台的“未找到组件”通常指向路径问题。

修改权限后重新登录检查，避免旧的已装载路由影响判断。后端接口仍须授权，即使通过浏览器手工输入地址也不能扩大访问范围。

实现参考：[路由守卫](https://github.com/Nicander93/nz-admin/blob/master/nz-web/src/router/index.ts)、[路由工具](https://github.com/Nicander93/nz-admin/blob/master/nz-web/src/utils/routeHelper.ts)。继续见[菜单操作](/platform/system/menus)。
