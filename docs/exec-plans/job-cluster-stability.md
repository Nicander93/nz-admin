# Quartz 作业集群稳定性执行计划

> 状态：待实施。本文是后续实施和验收路线，不代表 JDBC 集群、迁移或业务幂等已经完成。下列检查项均保持未完成。

## 当前代码与运行边界

- `nz-server/pom.xml` 继承 Spring Boot `3.5.15`；Spring Boot dependency BOM 将 `org.quartz-scheduler:quartz` 管理为 `2.5.2`，本轮已构建应用归档也实际包含 `BOOT-INF/lib/quartz-2.5.2.jar`，starter 未单独锁版本。实施时仍应核对实际解析版本，并使用该版本附带的 PostgreSQL DDL。
- `QuartzAutoConfiguration` 当前自行 `new StdSchedulerFactory().getScheduler()` 并安装 `SpringBeanJobFactory`。仓库未配置 `spring.quartz.job-store-type=jdbc`，`db/init.sql` 也没有 QRTZ 表；默认因此是进程内 RAM store。
- `QuartzJobService.scheduleAllJobs` 调用 `scheduler.clear()` 再加载业务任务。共享 JDBC store 后，任一节点启动执行此逻辑会清除其他节点依赖的调度数据，必须先移除此语义。
- `JobServiceImpl.runOnce` 直接调用 `JobExecuteService`，没有通过 Quartz `triggerJob` 分发；此路径绕过集群节点抢占以及 Quartz 的同 JobKey 并发控制。
- `JobInvocation` 只有任务标识和调用目标，没有稳定执行键或恢复上下文。`requestRecovery()` 与 `@DisallowConcurrentExecution` 可用于调度恢复/同一 JobKey 的并发约束，但业务副作用仍需幂等保护。
- `application.yml` 启用了 Flyway，迁移 CLI 要求 V2 及后续版本配有对应 `upgrade-pN-*.sql`，迁移资源测试也逐项登记 SQL。当前迁移最高为 V33，下一组版本拟为 `V34__quartz_jdbc_cluster.sql` 和 `upgrade-p34-quartz-jdbc-cluster.sql`；落地时以主分支实际最高版本重新核对。

## 目标设计

沿用现有 Quartz，将 PostgreSQL JDBC JobStore 作为多个应用实例共享的调度状态来源。每个节点用同一逻辑 `schedulerName`、同一数据库/schema、相同 JobStore/集群设置，设置 `instanceId=AUTO` 获得唯一实例 ID；所有节点启用 clustering，使用 PostgreSQL delegate 和定期同步的系统时钟。只能让全部节点一致地使用 JDBC clustered store，禁止 RAM 与 JDBC 节点同时执行同一组业务任务。

Quartz 的 JDBC 集群负责触发器争用和配置了 recovery 的作业恢复，不提供业务副作用的 exactly-once 保证。数据库事务、外部服务调用和 Quartz 状态之间没有原子提交；实现必须允许 at-least-once 尝试，并用持久执行账本/业务幂等键收敛重复效果。对账与 outbox 负责弥补应用库任务配置与 Quartz store 之间的提交窗口。

Spring Boot 官方说明 Quartz 默认采用内存 JobStore，并支持通过 DataSource 和 `spring.quartz.job-store-type=jdbc` 启用 JDBC store；本项目当前直接 new factory，因此实施需明确迁移至 Boot 自动配置，或提供等价的 Spring `SchedulerFactoryBean` 配置，确保现有 DataSource、Spring JobFactory、监听器和关闭生命周期都被正确接入。Quartz 官方文档要求集群节点共享配置和表、各节点实例 ID 唯一、时钟同步；恢复作业可能再次执行原工作。参见 [Spring Boot 3.5 Quartz 文档](https://docs.spring.io/spring-boot/3.5/reference/io/quartz.html)、[Quartz 2.5 集群教程](https://www.quartz-scheduler.org/documentation/quartz-2.5.x/tutorials/tutorial-lesson-11.html)、[Quartz JDBC JobStore 配置](https://www.quartz-scheduler.org/documentation/quartz-2.5.x/configuration/ConfigJobStoreTX.html) 和 [Quartz 恢复与幂等最佳实践](https://www.quartz-scheduler.org/documentation/quartz-2.5.x/best-practices.html)。

## 分阶段执行

### 1. 调度工厂与配置

- [ ] 选择统一的 Spring Quartz 装配入口，保留 `SpringBeanJobFactory` 的依赖注入行为，并移除绕过 Spring 生命周期的直接 `StdSchedulerFactory` 实例化。
- [ ] 增加可配置的 `NZ_JOB_CLUSTER_ENABLED` 开关及 `spring.quartz` 配置：JDBC JobStore、稳定 scheduler name、自动唯一 instance ID、cluster 开启、PostgreSQL delegate、表前缀和 misfire/检查间隔。生产多副本 profile 中强制启用 JDBC 集群；单机开发可显式使用 RAM，但不得与集群节点共用 Scheduler 表。
- [ ] 复用应用 DataSource 和凭据，不新增独立硬编码凭据。评估连接池容量（Quartz worker、业务事务与 Scheduler API 调用）；设置 Quartz 执行线程池、优雅停机与健康状态。
- [ ] 核对 2.5.2 的 schema 和 PostgreSQL delegate；为新环境初始化及旧环境升级统一使用版本化 SQL，关闭每节点启动时自动建表。不得直接编辑 QRTZ 表以实现业务操作。
- [ ] 明确从现有 RAM 调度切换的停机顺序和回滚办法：先停旧实例并确认无旧调度进程，再迁移/建表、启动全部 JDBC 配置节点；禁止新旧 store 模式同时对外执行。

### 2. Flyway 与手动升级

- [ ] 增加 `V34__quartz_jdbc_cluster.sql`，内容来自 Quartz 2.5.2 官方 PostgreSQL schema，并包括事务性调度 outbox/执行账本所需应用表。按项目实际字段约定补主键、索引、租户字段和保留策略。
- [ ] 增加与 V34 等价的 `upgrade-p34-quartz-jdbc-cluster.sql`，登记 `FlywayMigrationResourcesTest`，更新 CLI/README 所需的迁移版本说明。保持 Flyway 和手动升级路径的对象、索引、默认值一致。
- [ ] 新装路径与升级路径都验证：QRTZ 表在正确 schema 建立；序列/主键与 PostgreSQL 类型兼容；多次启动/重复执行迁移安全；应用账号具备所需锁表、读写和建表权限边界。
- [ ] 手动升级先备份并确认所有应用实例停止；DBA 执行 p34 后核对对象与权限，再设置 JDBC 集群配置并启动单节点 smoke 环境。确认回滚只回滚应用版本；已写入的调度状态不得靠手工删表“回滚”。生产执行前由运维完成备份/恢复演练。

### 3. 共享 store 安全对账

- [ ] 删除启动流程中的全局 `scheduler.clear()`。建立明确的任务拥有权范围：仅管理本系统约定的 `JobKey` group/name namespace，不触碰 Quartz 插件或其他模块的任务。
- [ ] 把 `sys_job` 作为业务定义的权威来源。启动时幂等 reconcile 每个任务的 JobDetail/Trigger：不存在则创建，配置变化则更新，停用/删除则只对本应用已登记的 key 停止或移除；重复执行没有副作用，多个节点并发 reconcile 可安全重试。
- [ ] 所有 CRUD、启停、删除操作改为事务性 outbox：在同一业务数据库事务内写 `sys_job` 与 outbox event。后台 worker 用 PostgreSQL `FOR UPDATE SKIP LOCKED` 抢占，幂等应用 Quartz 操作，提交成功后标记完成；失败有退避重试、最大尝试数、告警/人工重放入口。
- [ ] 定义崩溃修复：若业务事务已提交而 Quartz 操作尚未完成，outbox 重试补发；若 Quartz 已变更但 outbox 完成标记未提交，再次应用同一版本事件仍安全。周期性 reconcile 修复历史漂移并输出差异审计，不能清空共享 store。
- [ ] 修正 misfire 策略映射，分别表达项目支持的“立即补触发”“跳过过期触发”等语义；验证 Quartz 的实际 smart policy/恢复行为，避免两个 UI 选项映射到相同策略。

### 4. 执行上下文与幂等

- [ ] 扩充 `JobInvocation` 的可选执行上下文（execution key、触发来源、计划触发时刻、恢复标记/尝试信息），同时保留旧构造方式和现有 `@JobTask` 无参方法调用兼容。调用目标继续通过注册/白名单验证，不开放任意反射调用。
- [ ] 引入持久 `job_execution` 账本：至少包含 tenant、job、稳定 execution key、来源、计划触发时间、状态、租约/持有节点、开始/完成时间和错误摘要，并以 `(tenant_id, execution_key)` 唯一约束实现原子 claim。账本状态转换需支持运行超时租约过期后的恢复、完成终态和受控重试；日志表用于审计，不作为并发锁。
- [ ] 定时触发的幂等键由租户、job ID 和计划触发时间确定；Quartz recovery 必须沿用原 scheduled fire time 生成相同键。实际执行前 claim；已完成键直接返回；运行中键由租约/claim 规则裁决；失败重试仍使用同一逻辑键。
- [ ] 手动触发走 `QuartzSchedulerManager.runOnce`/`Scheduler.triggerJob`，加入 requestId/execution key 的 JobDataMap，避免 HTTP 线程直接跑任务。`Idempotency-Key` 可用于请求重放识别，但需要与长任务执行账本明确协作：相同 key 回放同一次执行状态，不同 key 创建不同手动执行。为兼容现有客户端，确定无 key 时是否生成一次性键，并在 API 文档说明调用方重试语义。
- [ ] 明确事务边界：同一 PostgreSQL 内的账本 claim 与受控业务写入尽量同事务；邮件、HTTP、文件或其他外部副作用不可由本地账本单独保证 exactly-once。需要通过下游幂等键或另一层事务 outbox 承接；无法承接的任务须声明 at-least-once 风险并避免启用 Quartz recovery。
- [ ] 所有执行上下文还原并验证租户。任务定义、outbox、执行账本、日志查询均按 tenant 隔离；系统级任务使用显式 system tenant/mode，不继承线程残留租户。每次触发时再次校验租户存在、启用和授权状态，停用/过期租户不能依赖旧 JobDataMap 继续运行。
- [ ] 对 JobLog 增加 execution key、trigger source/计划时间、实际执行节点、恢复/重试标记等可关联字段；统一关联账本记录并设置脱敏/保留策略。调整任务日志 API 和前端展示，使支持人员能够按 execution key 跟踪一次逻辑执行的多次尝试。

### 5. API 与管理页面

- [ ] 补齐创建、修改、暂停、恢复、删除、手动触发和执行日志行为契约；每个写操作返回可观察的业务状态/outbox 处理结果，UI 能区分“配置已保存，调度同步中”和“调度已生效”。
- [ ] 手动触发接口验证租户与任务权限，接受并记录幂等键；同一 key 的并发请求只能生成一条逻辑执行，重放返回相同 execution ID/状态，不同 key 可以表示有意的两次运行。
- [ ] UI 刷新后从 API/数据库读到实际状态，支持执行记录筛选、错误详情、恢复/重试标识。前端不得通过本地乐观状态伪造调度已经成功。

## 验收路线

所有以下验收目前均未执行，不能以 RAM 单节点测试或两个 Spring context 替代真实双 JVM 集群。

### 自动化与数据库验收

- [ ] 迁移资源/CLI 检查覆盖 V34、p34 与最高迁移版本同步；在干净 PostgreSQL 验证 Flyway 新装，在有 V33 数据库验证升级，并比较手动脚本结果。
- [ ] PostgreSQL 集成测试使用独立 Scheduler 与独立 JVM/进程连接同一数据库，确认持久 job store、稳定 scheduler name、不同 instanceId、所有节点 clustered，且每个计划 fire 仅有一个节点取得触发器。
- [ ] 两节点同时冷启动、滚动重启和并发 reconcile：已有任务不丢失、不重复创建、不改写其他命名空间任务；创建/更新/禁用/删除最终与 `sys_job` 一致；整个用例中不得调用全局 `clear()`。
- [ ] 注入 DB/Quartz 暂时不可用、提交后进程退出、outbox claim 后硬杀、Quartz 变更后确认前硬杀；验证事件最终重试收敛、告警和周期对账能修复状态。
- [ ] 双 JVM 抢占用持久 marker 验证单个计划时刻只产生一条业务效果，并记录实际执行节点。执行过程中硬杀持有节点，等待另一节点接管 recovery；验证执行尝试可重复、execution key 相同、账本/业务副作用仅按幂等规则提交一次。此项证明业务幂等恢复链，不把单节点重启当成集群证明。
- [ ] 并发执行测试覆盖 `@DisallowConcurrentExecution`、不同 JobKey 并发、相同 key 的手动请求重放、不同 key 的两次手动执行；检查账本锁、失败租约回收和重试策略。
- [ ] 多租户测试覆盖租户 A/B 调度、日志/账本筛选、越权触发、切换请求上下文、租户停用/过期、系统级任务；不得有跨租户任务执行或数据可见性。
- [ ] 覆盖 Quartz misfire 的每个公开策略、时区与 DST 边界（若产品承诺支持）、优雅关闭、节点时钟偏移监测、连接池容量和健康/就绪状态。

### 浏览器 E2E 与运维验收

- [ ] 使用真实前端与后端、PostgreSQL、Redis、两个独立后端 JVM 执行浏览器 E2E；通过 UI 创建/修改/暂停/恢复/删除任务，并直接读取数据库确认定义与状态，刷新页面确认持久状态。
- [ ] E2E 手动执行任务并检查 execution ID、幂等 key、执行节点和日志；同一 key 重放只对应一次逻辑执行，不同 key 形成两条执行；验证失败/恢复状态可以在 UI 追踪。
- [ ] 浏览器验证租户隔离和权限拒绝；交叉确认 API 响应、数据库 tenant_id 和页面列表/日志过滤一致。
- [ ] 完成部署文档：共享 PostgreSQL schema、连接池、时钟同步、节点命名/instance ID、环境变量、V34/p34 升级步骤、备份恢复、健康检查、告警、扩缩容、任务超时与执行键排障。
- [ ] 在两个独立 JVM 进程（也可使用两个容器或主机）完成真实硬崩溃演练，记录进程被强制终止后的接管、重复尝试、业务幂等结果、恢复时间和日志证据；分别验收无副作用任务与支持幂等键的外部副作用任务。

## 完成定义

- [ ] 实际 resolved Quartz 版本、DDL 来源与执行环境均留有可复核记录；Flyway、手动升级和新装路径一致。
- [ ] 两个独立 JVM 共用 PostgreSQL JDBC store，通过抢占与硬崩溃恢复验收；没有节点启动清空共享调度数据的路径。
- [ ] 任务配置 outbox、周期 reconcile、执行账本、稳定幂等键和租户隔离都有自动化回归覆盖。
- [ ] API、管理 UI、部署/升级文档及真实浏览器 E2E 完整覆盖 CRUD、重复请求、失败重试和故障恢复。
- [ ] 验收报告区分“单节点恢复”“双 JVM 抢占”“硬崩溃接管”“业务副作用幂等”四种证据；任何未执行项明确标记，不得宣称集群稳定性已完成。
