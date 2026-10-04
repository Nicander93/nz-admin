# 定时任务与日志排查

定时任务模块提供 Cron 校验、暂停、恢复和立即执行；日志记录操作与执行结果。任务创建成功后，还要确认调度器实际注册并执行。

## 配置与验证任务

1. 确认 job 模块启用，当前用户拥有任务菜单及操作权限。
2. 使用项目允许的调用目标配置任务，校验 Cron；不要直接暴露任意类/方法执行给普通用户。
3. 保存后查看任务状态，再使用“立即执行”验证实际业务结果。
4. 检查任务执行日志和后端日志，随后确认定时触发。
5. 暂停或修改后重新核对下一次触发，避免仅以页面状态判断。

任务 API 前缀为 `/api/system/job`。Cron 校验使用 query 权限，立即执行、暂停、恢复使用 edit 权限。新增操作仍要按功能单独判断是否应授予用户。

## 三类日志

| 类型 | 用来回答 |
| --- | --- |
| 登录日志 | 谁在什么时间尝试登录，结果如何 |
| 操作日志 | 哪个用户调用了被记录的业务操作 |
| 任务日志与后端运行日志 | 调度是否执行，执行失败在哪里 |

新业务关键操作使用 `@Log(title = "业务名称", businessType = ...)`，不要记录密码、令牌或原始密钥。接口异常日志可以帮助定位系统错误，但不能替代业务审计表和审批轨迹。

## 创建成功却没有执行

先确认启用状态、Cron、调用目标以及后端调度注册日志，再查看任务表中的租户归属。当前新增任务的调度注册失败会记录后端错误，因此不能仅检查 HTTP 成功。

多节点共享 Redis 和登录态，不自动等于定时任务全局只执行一次。当前双节点验收覆盖会话、审批与实时状态，未把分布式调度验收包含其中；关键任务应另行验证调度存储、竞争和业务幂等。

参考：[JobController](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-job/src/main/java/com/nz/admin/modules/job/controller/job/JobController.java)、[JobServiceImpl](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-job/src/main/java/com/nz/admin/modules/job/service/job/JobServiceImpl.java)。
