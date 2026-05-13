# 前端编码规范

本文档约定 `nz-web` 前端页面开发的基本风格，优先服务于系统管理类 CRUD 页面。规范的目标是让页面结构清晰、命名稳定、改动局部，避免为了抽象而过早增加目录和文件。

## 基本原则

- 页面代码优先保持轻量、直观，先按当前业务复杂度组织代码。
- 不因为“可能复用”提前拆分组件、目录或工具函数。
- 同类页面保持一致的命名、返回结构和数据流。
- 组合逻辑优先放在页面目录下的 `hooks.ts`。
- 页面模板只做渲染和简单事件绑定，数据加载、提交、删除等页面动作放入 `hooks.ts`。

## 页面目录

普通页面优先使用下面的轻量结构：

```text
views/system/menu/
├── index.vue
└── hooks.ts
```

约定如下：

- `index.vue` 是页面入口，负责页面布局、表格、弹窗和事件绑定。
- `hooks.ts` 是页面组合逻辑入口，负责状态、接口调用、表单提交、删除刷新等。
- 页面不复杂时，不新增 `components/`、`composables/`、`constants.ts`、`types.ts` 等子目录或文件。
- 当页面明显变复杂时，才按需拆分，拆分应优先解决真实问题，例如模板过长、弹窗字段过多、多个页面确实复用同一组件。

建议拆分信号：

- `index.vue` 长期超过约 250 行，且主要复杂度来自弹窗表单或表格列。
- 同一段 UI 在两个以上页面出现，并且字段和交互基本一致。
- `hooks.ts` 中出现多个彼此独立的业务区域，继续放在一起会影响阅读。

## 文件命名

- 页面入口固定命名为 `index.vue`。
- 页面组合逻辑固定命名为 `hooks.ts`。
- API 文件按业务名命名，例如 `api/system/menu.ts`。
- 只有在确实需要拆分组件时，组件使用 PascalCase，例如 `MenuFormDialog.vue`。
- 不推荐在简单页面里新建 `useMenuCrud.ts`、`composables/useMenuCrud.ts` 等文件，避免目录层级过重。

## hooks 返回结构

页面级 hook 返回给模板的对象固定分为三段：

- `table`：表格或树表状态。
- `form`：表单弹窗状态。
- `actions`：页面动作。

推荐结构：

```ts
export function useMenuCrud() {
  const table = {
    loading,
    data,
    loadData,
    toggleExpand,
  }

  const form = {
    model,
    visible,
    title,
    openAdd,
    openEdit,
    close,
  }

  const actions = {
    submit,
    remove,
  }

  return {
    table,
    form,
    actions,
  }
}
```

各段职责：

- `table` 放列表、分页、树数据、查询条件、加载状态、刷新和展开收起等表格相关能力。
- `form` 放表单模型、弹窗可见性、弹窗标题、表单模式、打开和关闭方法。
- `actions` 放用户触发的业务动作，例如提交、删除、启用、停用、分配权限。

**只读列表页**（无新增/编辑弹窗）：可不返回 `form`，仅返回 `table` 与 `actions`（如文件列表页）；对外命名仍优先使用 `table`/`actions`，避免再引入一套别名。

命名约定：

- 普通表格页面统一使用 `table`，树表页面也优先使用 `table`，不要另起 `menuTree`、`deptTree` 等业务化返回名。
- 表单统一使用 `form`，不要混用 `formState`、`formView`、`dialog`。
- 页面动作统一使用 `actions`，不要混用 `methods`、`handlers`、`operate`。

## 变量和函数命名

组合函数：

- 页面 CRUD 组合函数命名为 `useXxxCrud`，例如 `useMenuCrud`、`useRoleCrud`。
- 非 CRUD 页面按能力命名，例如 `useLoginForm`、`useDictOptions`。

状态变量：

- 使用名词或名词短语，例如 `loading`、`tableData`、`treeData`、`queryParams`、`selectedIds`。
- 布尔值使用 `is`、`has`、`can` 前缀，例如 `isExpand`、`hasSelection`、`canSubmit`。
- 表单模型在 hook 内可以叫 `formModel`，返回给页面时统一归入 `form.model`。

函数：

- 数据加载使用 `loadData`、`loadTree`、`refresh`，同一页面内只保留一种主刷新入口。
- 打开弹窗使用 `openAdd`、`openEdit`。
- 提交表单使用 `submit` 或 `submitForm`，返回给模板时优先叫 `actions.submit`。
- 删除动作使用 `remove` 或 `removeById`，返回给模板时优先叫 `actions.remove`。
- `onXxx`、`handleXxx` 优先保留在 `index.vue` 中，用于模板事件适配，例如二次确认后再调用 `actions.remove`。

## 类型和常量

- API 返回类型优先定义在对应 `api` 文件中，例如 `SysMenu`。
- 页面独有类型优先留在 `hooks.ts`，只有被多个文件使用时再考虑单独抽出。
- 业务枚举值应尽量集中表达，避免在模板里散落魔法值。

例如菜单类型、状态、显示隐藏这类值，优先在 `hooks.ts` 顶部或后续共享常量中定义清楚：

```ts
const MENU_TYPE = {
  DIR: 'M',
  MENU: 'C',
  BUTTON: 'F',
} as const

const STATUS = {
  ENABLED: 0,
  DISABLED: 1,
} as const
```

简单页面可以先放在 `hooks.ts` 顶部；当两个以上页面共用时，再抽到合适的共享位置。

## index.vue 职责

`index.vue` 应保持页面编排角色：

- 引入页面 hook。
- 绑定表格、表单、弹窗。
- 处理模板事件适配，例如删除确认。
- 保留少量与 UI 直接相关的简单判断。

不建议在 `index.vue` 中放：

- 接口请求。
- 复杂数据转换。
- 大段业务规则判断。
- 多处重复的字典映射。

## hooks.ts 职责

`hooks.ts` 负责页面状态和业务动作：

- 初始化表格、树表和表单状态。
- 调用 API 加载数据。
- 调用公共 CRUD 工具。
- 处理提交成功后的提示、关窗、刷新。
- 封装删除、启停用、分配等页面动作。

`hooks.ts` 中可以保留少量纯函数，例如树排序、默认表单构造。若纯函数跨页面复用，再移动到 `utils` 或更合适的位置。

## 注释

- 注释说明业务意图，不重复解释代码字面动作。
- 页面 hook 顶部建议保留一句说明，例如“菜单页面的 CRUD 逻辑”。
- 对容易误解的业务规则补充短注释，例如根节点约定、状态值约定。
- 不为了形式给每个变量和函数都写注释。

## 推荐实践

- 优先复用项目已有的 `useCrud`、`useForm`、API 层和工具函数。
- 新增页面时先模仿同类页面结构，再按实际复杂度微调。
- 对外暴露给模板的名字要稳定，内部实现可以逐步调整。
- 小页面不要过度拆分，大页面不要硬塞在一个文件里。
- 发现同类页面写法分叉时，优先统一新代码，旧代码可在相关需求中顺手收敛。
