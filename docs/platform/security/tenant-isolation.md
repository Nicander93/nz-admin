# 租户管理与新表隔离

租户套餐限制可以提供哪些功能，角色在套餐范围内再授权；业务表的租户条件负责数据隔离。只建立菜单套餐，不会自动保护新表。

## 新建一个租户

默认租户管理员先建立套餐并勾选菜单，再创建租户、选择套餐并填写租户管理员。系统在事务中创建租户、组织根、管理员角色与账号。租户管理员使用自己的租户编码登录。

套餐更新后权限与套餐实时求交集，普通角色保留的旧菜单不能继续越过套餐边界。租户停用采用保留数据的语义；默认租户不能停用。完整管理规则见[多租户手册](/multi-tenancy)。

## 新业务表的登记

普通业务表使用 `tenant_id BIGINT NOT NULL`，模块可注册：

```java
@Bean
public TenantTableRuleCustomizer orderTenantTables() {
    return tables -> tables.add("biz_order");
}
```

接口来自 `com.nz.admin.framework.tenant.core`。配置类应进入模块扫描；生成器也可输出 AccessConfiguration 完成登记。创建字段由可信上下文填入，DTO 不让客户端指定其他租户。

## 启动审计与例外

`NZ_TENANT_VALIDATE_TABLES=true` 检查当前 schema 中的租户表是否已登记。合法的专用隔离表进入 externally-managed-tables，同时提供实际隔离实现；名单本身不产生过滤条件。

Warm-Flow 标准表和业务绑定/事件使用字符串租户规则；demo_leave 是普通数值租户表。关闭可选模块不会删除迁移创建的表，默认登记保留这些已知表，不能通过关闭审计掩盖未知表。

## 直接 SQL 和异步任务

MyBatis-Plus 受管查询会添加租户条件；JdbcTemplate、手写外部查询或其他数据源需要显式限定可信租户。后台事件必须使用事件携带的租户，不能自动使用默认租户。

至少验证 A 租户不能读、改、删 B 的记录，复杂 JOIN 与批量操作也要覆盖。功能数据范围继续独立生效，见[数据权限](/platform/security/data-scope)。
