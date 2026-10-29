# 定时任务与日志排查

任务页面提供新增、编辑、删除、暂停、恢复、立即执行，以及调度同步记录、执行账本和任务日志。保存配置、同步到 Quartz、任务执行完成是三个不同阶段。

本轮已加入 PostgreSQL JDBC 调度、事务性同步队列与持久执行账本。本地已验证两个独立 JVM 的 Cron 争用、手动请求去重，以及执行中强杀节点后的原键接管；下游效果采用受控 HTTP 幂等服务验证。全部验收范围与生产部署边界见[测试矩阵](/guide/testing/coverage-matrix)。

## 从创建到执行

1. 启用 job 模块，在业务 Bean 的 public 无参方法上添加 `@JobTask`，调用目标使用 `beanName.methodName`。
2. 在页面填写名称、分组、Cron、调用目标和错过触发策略。保存返回 `jobId`、`revision` 和 `scheduleState`。
3. 等待同步状态变为 `SYNCED`；`PENDING` 表示定义已持久保存，`ERROR` 表示同步达到重试上限，均不代表已注册成功。
4. 点击“立即执行”获得持久回执，再在执行记录中查看终态。`PENDING` 仅表示已排队。
5. 查看与 `executionId`、`executionKey` 关联的任务日志，并核对实际业务结果。

暂停会停止后续定时执行，已进入 `RUNNING` 的尝试仍可按原事务完成。暂停中的任务允许有权限的用户手动执行。编辑、删除和租户失效会使尚未执行业务的旧配置尝试跳过；不会强制中断已经运行的业务方法。

## 状态与排障入口

| 状态来源 | 状态 | 含义与处理 |
| --- | --- | --- |
| 任务定义 | 启用、暂停 | 管理员期望的后续调度状态 |
| 调度同步 | `PENDING`、`SYNCED`、`ERROR` | 同一事务保存定义和同步意图；失败退避重试，修复原因后可人工重试 |
| 执行账本 | `PENDING`、`RUNNING` | 等待调度或已获得当前尝试执行权 |
| 执行账本 | `SUCCEEDED`、`FAILED`、`SKIPPED` | 业务与成功日志提交、尝试失败、定义/租户已失效 |

删除任务后，同步记录仍可保留 `DELETE` 意图。应从“调度同步记录”检查清除状态；不能仅因任务列表已消失就认为 Quartz 已清除。

`GET /api/system/job/scheduler` 返回当前节点是否运行、是否持久化/集群、逻辑 scheduler 名称和实例 ID。多个节点应共享逻辑名称，同时有不同实例 ID。

## 方法声明与业务幂等

```java
@Component("billingTask")
public class BillingTask {
    @JobTask
    public void refreshSummary() {
        // 在当前可信租户下执行业务。
    }
}
```

`@JobTask` 默认不允许失联后自动恢复。只有业务已能承受同一逻辑键重复尝试时，才声明 `@JobTask(recoverable = true)`。标记本身不会使邮件、HTTP、文件等外部效果自动去重。

业务可通过 `JobRunContextHolder.requireCurrent()` 获取租户、定义版本、执行键、计划时刻、节点、尝试次数和恢复标记。当前数据源中参与同一事务的业务写入、成功日志与执行成功状态一起提交；主动使用其他数据源或独立事务的写入不在此原子边界内。

详细规则见[执行键、事务与安全重试](/platform/system/jobs/execution)。

## 权限与租户

API 前缀为 `/api/system/job`。列表及调度状态使用 `system:job:list`，详情与 Cron 校验使用 `system:job:query`；新增、修改、删除分别使用 add/edit/remove。立即执行、暂停、恢复及受控重试使用 edit。

执行记录和日志列表使用 `system:joblog:list`，详情使用 query，删除日志使用 remove。V34 将这三个按钮加入任务菜单；只有已包含任务菜单的套餐获得相应功能范围，普通角色仍需显式授权。

创建使用当前可信租户，更新不允许改变归属。定义、同步记录、执行账本和日志均按当前租户查询；对其他租户的详情、重试和删除请求会拒绝。任务线程通过 `JobTenantProvider` 重验租户有效性，不能沿用工作线程中残留的租户。

## 生产启动

`JobSchedulerInitializer` 独立于基础数据初始化，`NZ_INITIALIZE_DATA=false` 不会跳过任务恢复。初始化和周期对账只管理 `NZ_ADMIN` 命名空间，不清空共享调度器。

多副本启用 `NZ_JOB_CLUSTER_ENABLED=true`，所有节点使用同一 PostgreSQL schema 和 scheduler name。单机可显式采用 RAM 调度；RAM 与 JDBC 节点不能同时执行同一组业务任务。共享 Redis 会话不承担任务抢占。

升级、健康检查和故障恢复见[任务集群部署](/operations/job-cluster)。

## 日志各自回答什么

登录日志记录登录尝试；操作日志记录管理员调用的接口；执行账本记录一次逻辑执行的状态；任务日志记录每次尝试及节点。它们不能互相替代。

排查“保存成功却没执行”时，依次检查定义状态、同步记录错误、节点调度元数据、执行账本、尝试日志和真实业务结果。不要手工修改 `QRTZ_*` 表来绕过状态机。
