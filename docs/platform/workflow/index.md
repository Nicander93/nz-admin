# 工作流使用与接入

新业务使用 Warm-Flow 官方经典设计器和流程中心。管理员配置流程，申请人从业务单据发起，审批人从待办办理；页面不要求用户填写内部定义或实例 ID。

## 按角色阅读

| 角色或任务 | 页面 |
| --- | --- |
| 管理员画流程并发布 | [设计器与版本](/platform/workflow/designer) |
| 申请人和审批人操作 | [审批中心与请假](/platform/workflow/approval-center) |
| 开发者接入新业务 | [业务 SPI 与可靠回调](/platform/workflow/business-integration) |
| 流程启动或状态异常 | [工作流排错](/platform/workflow/troubleshooting) |
| 查看历史接口与完整协议 | [工作流技术手册](/workflow) |

## 前置条件

先升级至 V30，开启 `NZ_WARM_FLOW_ENABLED=true`，并分配流程菜单与对应 design/query/action/start 权限。请假示例还需要 demo 模块及 demo:leave 权限。

## 当前支持范围

经典画布、顺序/分支/并行/会签、定义版本、个人待办/申请/已办、转办、委派、指定已访问审批节点退回、加减签、撤回和终止。业务通过数据库事件同步状态，多节点竞争消费并失败重试。

旧接口和旧在途实例保留 legacy 运行时，不支持自动搬迁。当前未接入仿钉钉模型、通用动态表单、任意执行监听器和全局流程运营监控。上游资源中出现某个选项，不代表当前适配服务端接受它。
