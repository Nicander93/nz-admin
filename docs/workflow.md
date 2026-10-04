# 工作流模块

`nz-workflow` 是独立业务模块。新业务入口使用官方经典设计器与流程中心；旧运行时保留分类、定义、实例、任务和抄送接口。下文分别说明，避免把旧功能列表当成新引擎的完全对应能力。

操作和新业务接入可先从[工作流分层指南](/platform/workflow/)阅读。

## 旧运行时（legacy）能力

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

V25 将现有运行时的三张同名表改为 `nz_flow_*_legacy`，保留全部存量数据和原有接口行为；同时创建 Warm-Flow 1.8.9 的七张标准表。新运行时默认通过 `warm-flow.enabled=false` 关闭；租户、审计和办理人解析桥接已经完成，新入口的业务 API、实例参与人权限和任务办理人权限已接入；升级至 V30 后可显式启用。人工升级脚本是 `db/upgrade-p25-warm-flow-foundation.sql`。

## 当前范围

新引擎使用 Warm-Flow 1.8.9 官方经典设计器，提供定义草稿、拖拽设计、保存重载、发布与版本列表，以及个人流程中心、请假审批业务示例和受控高级操作。旧业务入口仍使用 legacy 单任务运行时，不支持并行网关。

仿钉钉模型、动态表单设计器、任意监听器/SpEL、跳回开始节点、全局流程运营监控暂未开放。UI 资源包含上游能力不表示对应服务端能力已经开放；不支持的保存配置会明确拒绝。

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

## 官方设计器与流程中心

升级至 V30 后设置 `NZ_WARM_FLOW_ENABLED=true`。菜单“工作流程 → 流程中心”提供我的待办、我的申请、我的已办、审批详情与轨迹；用户通过列表进入实例，不需要填写内部 ID。

具有 `workflow:engine:design` 的用户可以创建定义，在“业务用途”中选通用流程或已经装配的业务用途，然后使用官方经典画布设计、保存和发布。新建默认三节点流程，审批人默认为当前设计人员，发布前应通过节点办理人选择器确认实际用户/角色。办理人只从当前租户的有效用户、角色中选择。

设计器资源来自 `warm-flow-plugin-vue3-ui:1.8.9`，没有复制或修改其编译资源，也没有装配上游标准保存控制器。只对 `/api/workflow/designer/ui/**` 静态资源放行；配置、模型查询、保存、办理人查询均要求设计权限。认证信息按官方协议从同源存储读取，不放入 URL。宿主只接受同源、当前 iframe 发出的消息，退出和卸载会清理设计器认证副本。开源声明位于 `licenses/`。

服务端白名单转换节点、办理人、比例、内置条件与坐标，不接受客户端租户、审计字段、实例或执行监听器。旧模型缺少坐标时补齐画布位置与文本位置。只覆盖未发布、未被实例引用的草稿；已发布和历史版本通过“创建新版本”修改。PostgreSQL 事务锁按租户与流程编码串行化创建、保存、发布；实例行锁串行化办理。在途实例保留原定义与业务快照。

| 接口 | 权限与边界 |
| --- | --- |
| `GET /api/workflow/designer/definitions`、`business-types` | design 权限，本租户定义和已装配用途 |
| `POST /api/workflow/designer/definitions` | design 权限，必须带幂等键；可通过 sourceId 创建新版本 |
| `GET /api/workflow/designer/warm-flow/query-def/{id}` | design 权限与本租户对象校验 |
| `POST /api/workflow/designer/warm-flow/save-json` | design 权限，只允许安全字段与可编辑草稿 |
| `POST /api/workflow/engine/definitions/{id}/publish` | design 权限，历史版本不能重新发布 |
| `GET /api/workflow/engine/center/{category}` | query 权限，category 为 pending/applications/completed，限定本人 |
| `GET /api/workflow/engine/instances/{id}` | query 权限，还须为发起人、待办人或历史办理人 |
| `POST /api/workflow/engine/tasks/{id}/action` | action 权限与任务办理人校验，必须带幂等键 |
| `POST /api/workflow/engine/tasks/{id}/manage` | action 权限，原生转办、委派、加减签、指定节点退回或终止 |
| `POST /api/workflow/engine/instances/{id}/revoke` | start 权限与申请人身份，结束当前实例并记录撤回状态 |

既有声明式导入/读取与通用启动 API 保留。审批节点 `permissionFlag` 使用 `user:<id>` 或 `role:<roleKey>`，多个用逗号分隔；官方协议用 `@@`，适配层转换。`nodeRatio` 为 0–100：0 或签、100 会签、中间值为通过比例。加签沿用节点比例；减签必须保留至少一名现有办理人。条件只开放内置比较，例如 `ge@@amount|100`。

委派完成会归还原办理人，不代表节点最终通过。指定退回只能选择本实例已经到达的审批节点，不能跳到未来节点或开始节点。定义中存在 REJECT 连线时按连线退回；没有 REJECT 连线时结束本次实例并标记退回，业务申请恢复可编辑状态。撤回/终止后不留审批待办，业务重新提交创建新实例。旧实例与业务快照继续保留。

## 请假业务与可靠回调

“示例模块 → 请假申请”支持保存草稿、修改、提交与查看审批详情。只能选择管理员声明为“请假申请”用途的已发布定义；通用流程与其他用途不会出现在选项中，后端启动时也再次校验。申请人只能读写自己的单据，审批人从流程中心读取经参与人校验的申请原因、日期和审批快照。审批中的单据不能修改。

业务模块仅依赖公共 `NzWorkflowBusinessLauncher`、`NzWorkflowBusinessHandler` SPI，不依赖工作流实现。新增业务实现 `businessType/businessName/detail/apply`，通过业务入口校验单据归属和提交状态，再调用启动接口。不要把客户端传入的业务类型直接交给引擎。业务键锁阻止不同幂等键重复启动在途审批，单据状态和流程启动在同一数据源事务中提交。

`nz_workflow_business` 保存当前业务绑定，`nz_workflow_event` 与每次流程操作一起落库，保存实例序号、业务快照和回调状态。两个节点通过 `FOR UPDATE SKIP LOCKED` 竞争事件，前序未成功时不投递后序；默认每秒轮询，失败按指数延迟重试。处理器运行在独立事务，失败回滚业务修改，事件保留。处理方必须按实例与序号幂等；请假示例通过当前实例和 `last_sequence` 去重。回调属于至少一次投递，外部服务也应支持幂等，并设置调用超时。

流程详情展示业务状态正在同步或待重试，并自动刷新同步结果。事件中的错误信息保留在数据库供运维排查；不会把内部异常暴露给普通申请人。已投递事件同时保存历史业务快照，不自动清理。业务关联实例不能通过通用删除接口单独删除，避免单据失去审批轨迹。

V30 的 Flyway 脚本是 `db/migration/V30__workflow_business_integration.sql`，人工升级脚本是 `db/upgrade-p30-workflow-business-integration.sql`。不要同时执行两条路径。

## 真实验收入口

`WarmFlowPostgresTest` 验证官方协议、租户/办理人权限、业务用途限制、普通申请人提交、回调故障重试、退回后重新提交和高级操作。`nz-web/tests/e2e/workflow/engine.spec.ts` 使用 Chromium 验证官方画布拖拽、保存重载、发布及两个用户会话的请假闭环。`tools/tests/workflow-cluster.mjs` 对两个真实后端进程验证共享登录、并发版本/发布、重复提交、跨节点审批和回调、实时票据与消息以及退出撤销。CI 的 e2e job 启动两个后端节点执行相同验收。
