# Service / DAO 方法与领域模型命名规约

## A）Service / DAO 层方法命名

| 语义 | 方法名前缀 | 说明 |
|------|------------|------|
| 获取单个对象 | `get` | 如 `getById`、`getUser` |
| 获取多个对象 | `list` | 如 `listByDeptId` |
| 获取统计值 | `count` | 如 `countActiveUsers` |
| 插入 | `save` / `insert` | 二选一或按项目既有风格统一 |
| 删除 | `remove` / `delete` | 二选一或按项目既有风格统一 |
| 修改 | `update` | 如 `updateStatus` |

## B）领域模型命名

| 类型 | 命名 | 说明 |
|------|------|------|
| 数据对象（与表对应） | `xxxDO` | `xxx` 为数据表名（或表名对应的业务简称，全项目统一即可） |
| 数据传输对象 | `xxxDTO` | `xxx` 为业务领域相关名称 |
| 展示对象 | `xxxVO` | `xxx` 一般为页面或接口展示含义 |
| POJO 统称 | — | DO / DTO / BO / VO 等已能表达含义时，**禁止**再命名为 `xxxPOJO` |

## C）`nz-system` 实体包与目录（`entity`）

业务模块 `nz-module/nz-system` 下，与表或查询、展示相关的类型按子包放置，根路径均为：

`src/main/java/com/nz/admin/modules/system/entity/`

| 子目录 | Java 包名 | 放什么 |
|--------|------------|--------|
| `dataobject/` | `com.nz.admin.modules.system.entity.dataobject` | 全部 `*DO`（与表一一对应的持久化对象），其下再按业务域分子包（如 `user`、`log`） |
| `query/` | `com.nz.admin.modules.system.entity.query` | 全部 `*Query`（列表/分页查询入参等），其下再按业务域分子包 |
| `vo/` | `com.nz.admin.modules.system.entity.vo` | 全部 `*VO`（接口或页面展示用对象），其下再按业务域分子包 |

**说明：** Java 语言中 `do` 为保留字，**包名不能使用 `do`**；持久化对象目录使用 **`dataobject`**，与类名后缀 `*DO` 对应。

其他业务模块若引入同类结构，可沿用同一套 `entity.dataobject` / `entity.query` / `entity.vo` 命名，便于跨模块对齐。

