# 工作流模块

`nz-workflow` 是独立业务模块，当前已交付流程分类、定义发布、实例执行、运行轨迹以及待办、已办和抄送任务中心。

## 当前能力

- 租户隔离的分类树、条件列表和详情查询。
- 新增、修改、删除与 Excel 导出。
- 同级名称唯一，禁止选择自己或后代作为上级。
- 移动分类时同步维护所有后代的祖先链。
- 内置根分类不可删除；存在子分类或流程定义引用时不可删除。
- 后端模块清单、自装配、前端模块清单、菜单与按钮权限。
- 同一流程编码按版本管理，同时最多一个草稿和一个已发布版本。
- 创建、编辑、复制、发布、取消发布、激活、挂起、删除和 JSON 导入导出。
- 发布新版本时自动失效旧版本；已发布或已被实例使用的版本受删除保护。
- 模型 JSON 校验唯一开始/结束节点、办理人、连线引用和全图可达性。
- 分类删除检查已经接入真实的流程定义表。
- 从激活的已发布定义发起实例，保存定义快照和变量，后续发布不影响运行中的实例。
- 支持顺序流和基于实例变量的互斥条件分支，条件操作符包括 `EQ`、`NE`、`GT`、`GE`、`LT`、`LE` 和 `IN`。
- 支持 `user:<id>`、`role:<roleKey>` 和 `initiator` 三种办理人表达式，并在办理时校验当前用户。
- 支持同意、驳回、撤回、终止、挂起、激活和结束后删除；状态更新使用当前节点条件避免重复办理。
- 实例详情返回从发起到结束的事件轨迹，保留操作人、节点变化、意见和时间。
- 流程定义的取消发布和删除已经接入真实实例引用检查。
- 每个运行实例维护一条当前待办；办理后转入历史任务，并按下一节点生成新待办。
- 待办按具体用户或角色过滤，已办按操作人过滤，抄送按接收人过滤。
- 支持通过任务 ID 同意、驳回和转办；转办同步更新实例办理人并保留转办历史。
- 支持批量抄送、重复接收人去重、未读/已读状态和阅读时间。
- 支持任务委派、受托完成后归还原办理人，并保留委派和归还历史。
- 支持发起人或管理员催办、5 分钟频控、站内消息通知和催办轨迹。
- Warm-Flow 已接入项目租户、审计和登录用户上下文；预设的裸用户 ID、`user:<id>` 与 `role:<roleKey>` 会解析为当前租户内的有效用户，并过滤禁用账号。

分类接口前缀为 `/api/workflow/category`。权限包括：

- `workflow:category:list`
- `workflow:category:query`
- `workflow:category:add`
- `workflow:category:edit`
- `workflow:category:remove`
- `workflow:category:export`

定义接口前缀为 `/api/workflow/definition`。权限包括：

- `workflow:definition:list`
- `workflow:definition:query`
- `workflow:definition:add`
- `workflow:definition:edit`
- `workflow:definition:remove`
- `workflow:definition:publish`
- `workflow:definition:active`
- `workflow:definition:copy`
- `workflow:definition:import`
- `workflow:definition:export`

实例接口前缀为 `/api/workflow/instance`。权限包括：

- `workflow:instance:list`
- `workflow:instance:query`
- `workflow:instance:start`
- `workflow:instance:action`
- `workflow:instance:cancel`
- `workflow:instance:terminate`
- `workflow:instance:active`
- `workflow:instance:remove`
- `workflow:instance:urge`

任务接口前缀为 `/api/workflow/task`。权限包括：

- `workflow:task:list`
- `workflow:task:query`
- `workflow:task:action`
- `workflow:task:transfer`
- `workflow:task:delegate`
- `workflow:task:copy`
- `workflow:task:read`

## 模块边界

工作流代码位于 `nz-server/nz-module/nz-workflow`，只依赖 common 与 framework starter，不依赖 `nz-system`。`WorkflowDefinitionReferenceChecker` 检查分类引用；V25 后，`DatabaseWorkflowDefinitionUsageChecker` 通过 `nz_flow_instance_legacy` 检查现有运行时的定义版本引用。

前端分类、定义、实例和任务页面位于 `nz-web/src/views/workflow`，由 `src/modules/workflow/manifest.ts` 注册。后端可通过 `nz.modules.workflow.enabled=false` 关闭自动装配；若要从交付物中彻底移除，还需删除 `nz-app` 依赖和前端模块清单。

## 数据库

V19 创建 `flow_category`、默认“OA审批”根分类、工作流目录、分类菜单和按钮权限，并把菜单加入现有租户套餐。人工升级脚本是 `db/upgrade-p19-workflow-category.sql`。

V20 创建 `flow_definition`、版本唯一约束、内置请假草稿、定义菜单和发布/启停/导入导出权限。人工升级脚本是 `db/upgrade-p20-workflow-definition.sql`。

V21 创建 `flow_instance` 和 `flow_instance_event`，加入实例菜单、按钮权限和租户套餐映射。人工升级脚本是 `db/upgrade-p21-workflow-instance.sql`。

V22 创建 `flow_task`、`flow_history_task` 和 `flow_task_copy`，回填 V21 存量运行实例，加入任务菜单、按钮权限和租户套餐映射。人工升级脚本是 `db/upgrade-p22-workflow-task.sql`。
V23 为当前任务增加原办理人与委派状态，历史任务增加 `DELEGATE`、`RESOLVE` 动作，并加入委派权限。原办理人委派后，受托人只能完成委派并归还任务；归还前不能通过、驳回或转办，实例办理接口也执行同一约束。人工升级脚本是 `db/upgrade-p23-workflow-task-delegate.sql`。
V24 增加实例催办权限，发起人或管理员可向当前用户或角色办理人发送站内消息，并写入 `URGE` 事件。人工升级脚本是 `db/upgrade-p24-workflow-instance-urge.sql`。

V25 将现有运行时的三张同名表改为 `nz_flow_*_legacy`，保留全部存量数据和原有接口行为；同时创建 Warm-Flow 1.8.9 的七张标准表。新运行时默认通过 `warm-flow.enabled=false` 关闭；租户、审计和办理人解析桥接已经完成，新入口的业务 API、实例参与人权限和任务办理人权限已接入；V29 升级完成后可显式启用。人工升级脚本是 `db/upgrade-p25-warm-flow-foundation.sql`。

## 尚未完成

旧业务入口仍使用单当前任务运行时，遇到并行网关会拒绝。新工作台提供节点/连线编辑、网关预览和声明式模型导入；新引擎已验证顺序、并行及多人会签。自由拖拽画布、加减签管理、退回任意节点、业务状态回调和全局运行监控尚未交付，不能视为与完整 RuoYi-Vue-Plus 功能对齐。

## 验证

```bash
cd nz-server
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw \
  -pl nz-module/nz-workflow,nz-app -am \
  -Dtest=WorkflowTaskServiceImplTest,WorkflowTaskLifecycleServiceTest,WorkflowInstanceServiceImplTest,WorkflowRuntimeResolverTest,WorkflowDefinitionServiceImplTest,WorkflowModelValidatorTest,WorkflowCategoryServiceImplTest,NzWorkflowModuleManifestTest,NzWarmFlowTenantHandlerTest,NzWarmFlowPermissionHandlerTest,NzWarmFlowDataFillHandlerTest,SystemWorkflowAssigneeResolverTest,FlywayMigrationResourcesTest,WarmFlowFoundationMigrationTest,NzAdminApplicationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

cd ../nz-web
pnpm test
pnpm build
```

## 新引擎工作台与迁移

完成 V27–V29 后设置 `NZ_WARM_FLOW_ENABLED=true`。菜单“工作流程 → 新引擎工作台”可以编辑节点、连线、办理人与网关，或导入同一声明式 JSON。读取已有定义后，重新导入会创建新版本；发布新版本不改写在途实例。旧流程页面继续办理旧实例，不能把旧模型 JSON 原样提交给新入口。

| 接口 | 权限与边界 |
| --- | --- |
| `GET /api/workflow/engine/capabilities` | `workflow:engine:query`，返回是否启用 |
| `POST /api/workflow/engine/definitions` | `workflow:engine:design`，导入新版本 |
| `GET /api/workflow/engine/definitions/{id}` | `workflow:engine:design`，读取本租户模型 |
| `POST /api/workflow/engine/definitions/{id}/publish` | `workflow:engine:design`，只发布本租户定义 |
| `POST /api/workflow/engine/instances` | `workflow:engine:start`，必须带 `Idempotency-Key` |
| `GET /api/workflow/engine/instances/{id}` | `workflow:engine:query`，还须为发起人、待办人或历史办理人 |
| `POST /api/workflow/engine/tasks/{id}/action` | `workflow:engine:action`，还须为该任务办理人，必须带幂等键 |
| `DELETE /api/workflow/engine/instances/{id}` | design 权限与发起人身份，且实例没有待办任务 |
| `DELETE /api/workflow/engine/definitions/{id}` | design 权限，只删除没有实例引用的本租户定义 |

审批节点 `permissionFlag` 使用 `user:<id>` 或 `role:<roleKey>`，多个用逗号分隔。`nodeRatio` 为 0–100 的数字字符串：0 是或签、100 是会签，中间值是票签通过比例；默认 0。条件只开放内置比较，例如 `ge@@amount|100`。节点类型 0 开始、1 审批、2 结束、3 互斥网关、4 并行网关、5 包容网关。导入要求唯一开始/结束、全部节点可达且能到达结束；ID、租户、审计字段和执行监听器由服务端管理。

定义和实例 ID 以字符串交付，避免浏览器损失 Snowflake 大整数精度。新引擎表的 SQL 租户过滤与接口对象权限共同生效，同流程编码可在不同租户分别发布。
