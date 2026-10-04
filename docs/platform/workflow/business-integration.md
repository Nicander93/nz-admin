# 业务 SPI、状态同步与重试

新业务只依赖公共审批协议，不依赖工作流模块实现。业务入口先判断单据归属与状态，再由 Launcher 发起；引擎事件通过 Handler 更新业务。

## 两个公共接口

`NzWorkflowBusinessLauncher` 提供可选定义和启动方法：

```java
launcher.available("leave");
launcher.start("leave", businessId, flowCode, Map.of("days", days));
```

businessType 由服务端业务实现固定，不能直接使用客户端传入值。先校验业务记录属于当前用户和租户，且处于可提交状态。

`NzWorkflowBusinessHandler` 的实现提供：

| 方法 | 责任 |
| --- | --- |
| businessType | 稳定用途标识；例如 leave |
| businessName | 管理端显示名称 |
| detail | 返回审批所需业务字段，避免敏感字段全量公开 |
| apply(Event) | 按事件更新业务状态，保证幂等 |

Handler 注册为业务模块 Bean；未装配的业务用途不会成为合法选项。detail 由运行时在参与人检查后读取；历史事件保留单独业务快照。

## 提交事务

业务方法锁定本人单据，调用 start，再写入 PENDING 与实例 ID。引擎业务键按租户、用途和单据绑定，防止不同请求键重复启动同一在途业务。Controller 的请求幂等负责相同键重试，不能代替业务键约束。

请假范例使用同一 PostgreSQL 数据源，启动和单据状态在同一事务提交。跨库业务要单独设计一致性策略，不默认获得同样保证。

## 回调事件携带什么

Event 包含 tenantId、businessId、instanceId、sequence、flowStatus、nodeType、actor 和 operation。后台回调没有申请人的 HTTP 登录上下文，必须使用事件的可信租户。

更新条件同时限定单据 ID、租户、当前实例以及 `last_sequence < event.sequence`。只按 businessId 更新会让旧实例重试覆盖重新提交的业务状态。

## 至少一次投递

`nz_workflow_event` 随引擎变更落库。两个节点通过 SKIP LOCKED 竞争，前序未成功时不投递后序；处理器在独立事务中执行，失败回滚，事件按延迟重试。

这不是“只投递一次”的保证。处理器要幂等，外部邮件或第三方 API 也需要接收方幂等与超时。状态映射由业务实现决定，不能把所有引擎数字状态当成统一业务枚举。

## 参考和验收

直接阅读[LeaveApplicationService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-demo/src/main/java/com/nz/admin/modules/demo/service/LeaveApplicationService.java)。至少验证重复提交、普通用户归属、回调失败回滚、旧事件不覆盖新实例，以及退回修改后保留历史快照。故障注入用独立测试库，见[后端测试](/guide/testing/backend)。
