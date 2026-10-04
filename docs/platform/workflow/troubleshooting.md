# 工作流启动与状态排错

先确定问题在定义、身份、任务办理还是业务回调。不同阶段检查不同记录，不直接修改流程表让页面“恢复正常”。

## 常见症状

| 症状 | 首先检查 |
| --- | --- |
| 新引擎尚未启用 | V30 已执行，当前进程 NZ_WARM_FLOW_ENABLED 为 true |
| 看不到定义管理 | design 权限，工作流模块和套餐 |
| 请假流程选项为空 | 定义已发布、当前租户、用途为 leave、Handler 已装配 |
| 保存模型被拒绝 | 草稿可编辑，模型是经典模式，字段与条件在白名单内 |
| 待办不能办理 | 实际任务办理人、用户/角色启用状态、是否已被另一请求完成 |
| 审批通过但单据仍待审 | 数据库事件投递与业务状态映射 |
| 退回后出现新实例 | 无退回连线的预期行为，核对旧轨迹仍保留 |

## 回调积压的只读检查

运维账号可在正确数据库按租户和单据查询：

```sql
SELECT instance_id, event_sequence, delivered, attempts,
       next_attempt, last_error
FROM nz_workflow_event
WHERE tenant_id = '1' AND business_type = 'leave'
  AND business_id = 'replace-with-business-id'
ORDER BY instance_id, event_sequence;
```

替换为实际可信租户与单据 ID。检查未 delivered 的前序事件、attempts 和 last_error，再核对处理器是否存在、业务记录当前实例是否匹配。不要输出 business_snapshot 到公开日志。

失败事件默认自动重试。先修复真实失败原因；当前没有面向普通用户的任意事件重放管理台，不直接删除事件或改 delivered 来跳过错误。

## 版本与租户

定义与实例访问都限定租户。相同编码可以存在于不同租户；排查时不能只以 flowCode 查记录。新版本只影响后续启动，旧实例保留旧定义和审批快照。

并发发布与提交在数据库锁下串行处理。若观察到多当前版本或重复实例，应保留请求键、租户、编码、版本和日志，使用真实集成测试重现，不先删除“多余”记录。

## 浏览器问题

检查设计器资源请求、iframe 同源和认证副本、保存 API 返回。静态画布加载不代表已登录。反向代理需保留 `/api`、静态资源与实时连接的正确路径；不要把设计 API 全部放行解决 401。

机制细节见[设计器](/platform/workflow/designer)和[可靠回调](/platform/workflow/business-integration)。
