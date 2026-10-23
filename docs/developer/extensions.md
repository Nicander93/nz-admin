# 扩展协议与业务实现

可复用机制放在框架，领域实现留在业务。增加能力前先找到扩展接口，避免在技术层查用户表、在其他模块直接注入具体业务实现。

## 找到正确协议

[能力目录](/developer/catalog)的“扩展协议”视图列出接口、所在模块、源码和指南；[全文索引](/reference/source-index)可以直接搜索类名。

| 任务 | 主要接口 | 实现责任 |
| --- | --- | --- |
| 解析当前身份 | AuthUserResolver | 按可信租户读取有效用户与授权 |
| 登记隔离表 | TenantTableRuleCustomizer | 登记普通租户表，补跨租户测试 |
| 配置归属范围 | DataScopeRuleCustomizer | 声明部门/所有者列，创建时可信赋值 |
| 定制插件 | MybatisPlusInterceptorCustomizer | 明确插件顺序与 SQL 行为 |
| 落操作日志 | OperationLogRecorder | 保存事件，不影响主业务响应 |
| 接短信供应商 | SmsProvider | 供应商调用、超时和失败语义 |
| 使用存储 | FileStorageService | 以项目文件协议访问存储，保护路径和凭据 |
| 发在线消息 | RealtimePublisher | 同时限定用户与租户，区分在线/历史 |
| 跨模块通知 | NzUserNotificationPublisher | 持久化接收人与已读状态 |
| 业务审批 | Launcher / Handler / ParticipantProvider | 单据归属、状态映射、幂等及有效办理人 |

并非所有接口都适合任意多实现：有的按 provider 类型选择，有的按 Bean 缺失提供默认实现，有的期望唯一业务解析器。接入前阅读其自动配置与调用方，不能只实现接口就假定自动生效。

## 例：业务模块登记表

业务配置类提供 TenantTableRuleCustomizer 与 DataScopeRuleCustomizer，框架收集这些 Bean 组合规则；框架不需要引用 biz_order 的 Service。查询/更新/删除得到统一条件，INSERT 仍由业务生成租户和归属。

详见[租户接入](/platform/security/tenant-isolation)和[数据范围](/platform/security/data-scope)。涉及复杂 JOIN、批量写入或 JDBC 时，验证真实 SQL 与拒绝结果。

## 例：审批回调

业务服务实现 NzWorkflowBusinessHandler，声明固定 businessType、名称、详情和 apply。Launcher 发起时校验用途与在途绑定；事件带可信租户、实例和序号，Handler 的更新条件防止旧实例重试覆盖新单据。

模块通过 common 协议协作，不反向依赖请假模块。完整提交事务和故障重试见[业务审批接入](/platform/workflow/business-integration)。

## 实现后的验证

检查 Bean 是否被条件装配、默认实现是否按设计退让、模块关闭后是否仍有调用方、异常是否回滚、身份上下文是否清理。真实短信/邮箱/存储账号单独验收，不把 mock 成功视为供应商已接通。
