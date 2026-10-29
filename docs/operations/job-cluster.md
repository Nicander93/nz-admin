# 任务集群部署与恢复

当前持久调度支持 PostgreSQL，表结构随 V34/p34 交付。本地两个独立 JVM 的强杀接管已实测，生产网络、负载和恢复演练仍需独立验收，范围见[测试矩阵](/guide/testing/coverage-matrix)。

## 同一组节点必须一致

| 配置 | 默认值 | 部署约束 |
| --- | --- | --- |
| `NZ_JOB_CLUSTER_ENABLED` | 跟随全局 cluster 开关 | 多副本为 true，启用 JDBC clustered store |
| `NZ_JOB_SCHEDULER_NAME` | nz-admin | 同一组节点相同；不同应用组使用不同名称 |
| `NZ_JOB_THREAD_COUNT` | 4 | 与业务耗时、连接池和并发承受能力共同估算 |
| `NZ_JOB_CLUSTER_CHECKIN_INTERVAL` | 5s | 节点存活检查，不是业务超时 |
| `NZ_JOB_RECONCILE_INTERVAL` | 30s | 周期核对业务定义和 Quartz 配置 |
| `NZ_JOB_EXECUTION_LEASE` | 60s | 至少 5s；活跃业务行锁阻止超期扫描抢走正在运行的事务 |
| `NZ_JOB_MAX_DISPATCH_ATTEMPTS` | 6 | 同步/派发失败的最大自动尝试次数 |

同组必须共享 PostgreSQL 数据库/schema、应用代码及任务 Bean。实例 ID 自动生成并应各不相同；同步各节点时钟。Quartz 使用应用 DataSource 与 PostgreSQL delegate，不在每个节点启动时自动重建表。

调度线程默认最多空闲 1000ms 后再次查询触发器，以承接其他节点写入或本地唤醒时尚未提交的事务。可通过 Spring 标准配置 `spring.quartz.properties.org.quartz.scheduler.idleWaitTime` 覆盖；调大将增加新触发器的可见等待，调小会增加空闲查询频率。

连接池需要同时承载 HTTP、调度存储、执行 claim、长业务事务和后台对账。不要把连接池大小直接设为 Quartz worker 数而忽略其他请求；根据压力验证调整。

## 从旧 RAM 调度升级

1. 备份数据库及应用配置，确认恢复流程和备份归属。
2. 停止全部旧实例，确认没有旧 RAM 调度进程继续执行任务。
3. 通过 Flyway 执行 V34，或由 DBA 执行等价 `upgrade-p34-quartz-jdbc-cluster.sql` 并按仓库手动迁移流程维护版本记录。不要同时重复走两条升级路径。
4. 配置所有新节点 JDBC 集群开关、共享 schema 和相同 scheduler name；先启动一个节点检查同步状态和一次实际业务执行。
5. 启动其他节点，核对实例 ID、集群存活记录、读写权限与健康状态。

不能让旧 RAM 与新 JDBC 节点并行执行同一组业务任务。V34 会保留旧 misfire 实际语义并加入任务日志权限；普通业务角色需要按套餐与角色菜单显式授权。

回滚不能仅替换成旧 RAM 应用后同时保留 JDBC 执行节点。先停止整组调度，核对已运行与外部副作用，再按已验证的备份/应用兼容方案恢复；不要通过删除 `QRTZ_*` 表模拟回滚。

## 看哪里判断可用

- `/actuator/health/readiness` 包含 job scheduler 状态；要求集群时应同时满足持久化、clustered 和 started。
- `/api/system/job/scheduler` 查看本节点实际 store、名称和实例 ID。
- 页面“调度同步记录”查看 `PENDING/ERROR` 积压、重试次数及最近错误；删除记录的清除失败也从这里重试。
- 执行记录区分等待、运行、成功、失败和跳过，并通过原执行键串起不同节点的尝试日志。

当前入口提供状态查询与人工重试。生产告警阈值、监控采集、保留期清理、备份恢复和数据库中断演练仍需作为运维验收交付，不能将健康接口本身当成全套监控。

## 故障处理

数据库或 Quartz 临时不可用时，已提交定义和同步意图仍保留；达到自动重试上限后进入 ERROR。先修复连接、权限或任务配置，再重试原同步记录。不要反复创建同名任务绕开失败记录。

节点失联后，允许恢复的执行使用原键再次尝试；未声明可恢复的任务会记录失败。人工处理前核对下游结果，避免把“重新执行”当成无副作用的查询。外部调用一致性见[执行键与事务](/platform/system/jobs/execution)。
