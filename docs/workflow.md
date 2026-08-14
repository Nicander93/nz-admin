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

任务接口前缀为 `/api/workflow/task`。权限包括：

- `workflow:task:list`
- `workflow:task:query`
- `workflow:task:action`
- `workflow:task:transfer`
- `workflow:task:delegate`
- `workflow:task:copy`
- `workflow:task:read`

## 模块边界

工作流代码位于 `nz-server/nz-module/nz-workflow`，只依赖 common 与 framework starter，不依赖 `nz-system`。`WorkflowDefinitionReferenceChecker` 检查分类引用，`DatabaseWorkflowDefinitionUsageChecker` 使用 `flow_instance` 检查定义版本引用。

前端分类、定义、实例和任务页面位于 `nz-web/src/views/workflow`，由 `src/modules/workflow/manifest.ts` 注册。后端可通过 `nz.modules.workflow.enabled=false` 关闭自动装配；若要从交付物中彻底移除，还需删除 `nz-app` 依赖和前端模块清单。

## 数据库

V19 创建 `flow_category`、默认“OA审批”根分类、工作流目录、分类菜单和按钮权限，并把菜单加入现有租户套餐。人工升级脚本是 `db/upgrade-p19-workflow-category.sql`。

V20 创建 `flow_definition`、版本唯一约束、内置请假草稿、定义菜单和发布/启停/导入导出权限。人工升级脚本是 `db/upgrade-p20-workflow-definition.sql`。

V21 创建 `flow_instance` 和 `flow_instance_event`，加入实例菜单、按钮权限和租户套餐映射。人工升级脚本是 `db/upgrade-p21-workflow-instance.sql`。

V22 创建 `flow_task`、`flow_history_task` 和 `flow_task_copy`，回填 V21 存量运行实例，加入任务菜单、按钮权限和租户套餐映射。人工升级脚本是 `db/upgrade-p22-workflow-task.sql`。
V23 为当前任务增加原办理人与委派状态，历史任务增加 `DELEGATE`、`RESOLVE` 动作，并加入委派权限。原办理人委派后，受托人只能完成委派并归还任务；归还前不能通过、驳回或转办，实例办理接口也执行同一约束。人工升级脚本是 `db/upgrade-p23-workflow-task-delegate.sql`。


## 尚未完成

完整对齐 RuoYi-Vue-Plus 还需要加签/减签、催办、多人会签、并行网关、可视化设计器、流程图、业务状态回调和运行监控。V23 仍按单个当前任务执行；模型运行到并行网关时会明确拒绝，不会把单任务状态伪装成并行执行。

## 验证

```bash
cd nz-server
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw \
  -pl nz-module/nz-workflow,nz-app -am \
  -Dtest=WorkflowTaskServiceImplTest,WorkflowTaskLifecycleServiceTest,WorkflowInstanceServiceImplTest,WorkflowRuntimeResolverTest,WorkflowDefinitionServiceImplTest,WorkflowModelValidatorTest,WorkflowCategoryServiceImplTest,NzWorkflowModuleManifestTest,FlywayMigrationResourcesTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

cd ../nz-web
pnpm test
pnpm build
```
