# 定时任务与日志排查

定时任务模块提供 Cron 校验、暂停、恢复和立即执行；日志记录操作与执行结果。任务创建成功后，还要确认调度器实际注册并执行。

## 配置与验证任务

1. 确认 job 模块启用，当前用户拥有任务菜单及操作权限。
2. 在业务 Bean 的 public 无参方法上添加 `@JobTask`，再用 `beanName.methodName` 配置调用目标。新增、修改和实际执行时都会核对标记，不开放任意 Bean 方法。
3. 保存后查看任务状态，再使用“立即执行”验证实际业务结果。
4. 检查任务执行日志和后端日志，随后确认定时触发。
5. 暂停或修改后重新核对下一次触发，避免仅以页面状态判断。

任务 API 前缀为 `/api/system/job`。Cron 校验使用 query 权限，立即执行、暂停、恢复使用 edit 权限。新增操作仍要按功能单独判断是否应授予用户。

## 生产启动与租户边界

每次启动由独立的 `JobSchedulerInitializer` 恢复调度，与 `NZ_INITIALIZE_DATA` 无关。`initialize-data=false` 不会再跳过任务恢复；基础初始化仍只负责新环境的管理员和示例数据。

租户业务通过 `JobTenantProvider` 提供启用且未过期的租户；job 模块按可信租户上下文分页加载启用任务。每次触发还会再次检查租户状态，停用或过期租户的旧触发器不再执行业务。

任务写入请求不包含租户与审计列。创建时使用当前可信租户，更新不允许迁移归属；操作不存在或其他租户的任务返回失败。不能把前端隐藏 tenantId 当成隔离边界。

```java
@Component("billingTask")
public class BillingTask {
    @JobTask
    public void refreshSummary() {
        // 当前线程已恢复该任务的可信租户；业务写入仍需幂等。
    }
}
```

已有自定义任务升级时，给允许执行的方法补上 `com.nz.admin.common.job.JobTask` 标记。基线 `demoTask.run` 已提供无副作用的带标记实现；未标记的活跃任务会导致恢复失败，应先补标记或暂停该任务再升级。暂停记录不会在启动时执行。

任务执行扩展统一传入 `JobInvocation`（任务 ID、名称、分组和调用目标）给 `JobExecuteService.executeOrThrow`。任务 ID 必须为正数；旧的字符串执行入口已移除，避免任务执行后因日志缺少任务 ID 而失败。

## 三类日志

| 类型 | 用来回答 |
| --- | --- |
| 登录日志 | 谁在什么时间尝试登录，结果如何 |
| 操作日志 | 哪个用户调用了被记录的业务操作 |
| 任务日志与后端运行日志 | 调度是否执行，执行失败在哪里 |

新业务关键操作使用 `@Log(title = "业务名称", businessType = ...)`，不要记录密码、令牌或原始密钥。接口异常日志可以帮助定位系统错误，但不能替代业务审计表和审批轨迹。

## 创建成功却没有执行

先确认启用状态、Cron、调用目标以及后端调度注册日志，再查看任务表中的租户归属。任务写入会校验 Cron、执行目标与状态；注册失败会返回失败并回滚数据库写入。Quartz 与数据库不属于同一事务，回滚时会补偿原调度；若调度器本身不可用，需恢复服务并重启，从数据库重新装配。外部业务副作用仍需幂等保护。

当前默认使用 Quartz RAM 存储，启动时从业务表重建各进程自己的调度。多节点共享 Redis 和登录态，不会协调这些内存触发器，因此同一业务任务可能被多个节点分别执行。

PostgreSQL JDBC 调度集群、任务变更 outbox、执行账本和稳定幂等键仍待实施。现有双节点验收覆盖会话、审批与实时状态，不能作为分布式调度保证；单节点重启恢复也不能证明节点崩溃接管或业务副作用只发生一次。

参考：[JobController](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-job/src/main/java/com/nz/admin/modules/job/controller/job/JobController.java)、[JobServiceImpl](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-job/src/main/java/com/nz/admin/modules/job/service/job/JobServiceImpl.java)。
