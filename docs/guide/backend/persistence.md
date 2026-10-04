# 持久化、分页与事务

业务数据访问同时承担三件事：筛选记录、限制访问范围、保证并发修改正确。不要只检查查询结果是否“看起来对”。

## Mapper 和 Service 边界

Mapper 继承 `BaseMapper<DO>`，聚焦 SQL。业务接口保持领域方法，实现类可继承 `ServiceImpl<Mapper, DO>`，不要求业务接口继承 `IService`。Controller 把输入交给 Service，不直接拼装多步持久化操作。

分页查询在租户、数据权限过滤后执行；先查全量再在 Java 内存过滤会导致总数、页数和数据泄露问题。JOIN、子查询及批量操作需要真实插件测试，不能只 mock Mapper。

## 可信字段由服务端写入

新业务表通常需要 `tenant_id`，以及按业务选择 `dept_id`、`owner_id`。创建请求不直接接收这些归属字段；由可信登录上下文生成。表登记与字段含义见[数据库表设计](/guide/database/)。

更新和删除检查受影响行数。之前查到记录，不代表执行写入时它仍属于当前范围或仍处在可修改状态。对于状态转换，把旧状态、租户和归属加入更新条件，并在未修改时返回明确业务拒绝。

## 多步修改保持一个事务

业务服务通过 Spring Bean 调用带 `@Transactional` 的方法，把业务状态、实例启动和数据库事件一起提交。请假服务的 `submit` 是实际范例：先锁定本人单据，再启动流程，最后更新申请状态。

直接在同一个对象内调用事务方法不能当成经过代理的外部调用。不要把数据库事务跨越缓慢的外部 HTTP 调用；事务无法撤销第三方已完成的操作。

## 什么时候需要锁

| 场景 | 当前参考做法 |
| --- | --- |
| 同一单据并发提交 | 锁业务记录，并限制已有在途实例 |
| 同编码定义并发建版本或发布 | PostgreSQL 事务锁串行化，唯一约束兜底 |
| 同一实例重复办理 | 实例锁加请求幂等 |
| 多节点投递事件 | `FOR UPDATE SKIP LOCKED` 竞争待投递记录 |

锁应保护一个具体业务不变量；仅在入口加 synchronized 不能跨节点生效。业务键、唯一约束与事务要一同设计。

## JDBC 的额外责任

MyBatis 插件不自动改写 `JdbcTemplate` 的 SQL。请假示例每条 JDBC 读写都显式限定可信租户，涉及本人操作时同时限定申请人；回调使用事件租户、当前实例和序号。新增 JDBC、导出或其他数据源操作时，单独验证隔离边界。

可参考[请假服务](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-demo/src/main/java/com/nz/admin/modules/demo/service/LeaveApplicationService.java)与[数据范围接入](/platform/security/data-scope)。
