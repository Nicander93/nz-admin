# 多租户

多租户能力默认开启。升级前的数据全部归入默认租户，现有管理员仍可使用原账号登录。

## 登录

登录页需要填写三个字段：

- 租户编码：默认租户使用 `default`。
- 用户名：默认管理员为 `admin`。
- 密码：默认密码为 `admin123`。

前端只提交租户编码。后端先从 `sys_tenant` 解析租户 ID，再在该租户范围内查询用户，并把租户 ID 写入 Sa-Token 的服务端会话。后续请求不接受客户端传入的租户 ID 或租户请求头，避免伪造租户身份。

租户被停用或超过到期时间后，登录会被拒绝。

## 管理流程

租户和套餐管理只允许默认租户操作。

1. 在“系统管理 / 租户套餐”新增套餐并勾选可用菜单。
2. 在“系统管理 / 租户管理”新增租户，选择套餐并填写管理员账号和密码。
3. 系统在一个事务内创建租户、根部门、租户管理员角色、管理员账号和基础参数。
4. 租户管理员使用自己的租户编码登录。

租户的“删除”操作采用停用语义，不物理删除业务数据。默认租户不能停用。套餐仍被租户使用时不能删除。

修改套餐菜单后，租户管理员角色会同步更新；权限读取还会与当前套餐实时求交集，因此普通角色残留的旧菜单不会继续生效。给角色分配菜单时也不能选择套餐范围外的菜单。

## 数据隔离

`nz-starter-tenant` 提供四项机制：

- `TenantContextHolder`：保存当前线程的可信租户 ID。
- `TenantContextFilter`：从 Sa-Token 服务端会话恢复租户上下文，并在请求结束后清理。
- `TenantTaskDecorator`：把租户上下文传递到异步日志等任务。
- `TenantLineInnerInterceptor`：在分页之前给受管 SQL 增加 `tenant_id` 条件。

当前隔离表由 `nz.tenant.included-tables` 控制，默认覆盖用户、角色、部门、岗位、字典、参数、公告、日志、任务、文件、文件配置和 demo 数据。以下表是全局表，不参与行级隔离：

- `sys_tenant`
- `sys_tenant_package`
- `sys_tenant_package_menu`
- `sys_menu`
- `sys_client`

新增租户业务表时，需要同时完成三件事：表中增加 `tenant_id BIGINT NOT NULL`，把表名加入 `included-tables`，并补跨租户不可见测试。不能只依赖前端隐藏或 Controller 权限。

## 配置

默认配置位于 `nz-app/src/main/resources/application.yml`：

```yaml
nz:
  tenant:
    enabled: true
    default-tenant-id: 1
```

关闭 `nz.tenant.enabled` 只会停止 MyBatis 行级拦截和请求上下文过滤，不会删除租户字段或租户数据。生产环境不建议关闭。

## 数据库升级

Flyway 使用 `V9__tenant_management.sql` 完成升级：

- 创建租户、套餐和套餐菜单表。
- 给现有受管表增加 `tenant_id`，默认值为 1。
- 把用户名、角色标识、岗位编码、字典类型和参数键改为租户内唯一。
- 创建默认租户、默认套餐、菜单和按钮权限。

不能直接使用 Flyway 的场景可执行 `db/upgrade-p9-tenant-management.sql`。升级前先备份数据库，并在同结构副本上验证。

## 验证

```bash
./nz migration check
./nz verify
```

针对后端可单独运行：

```bash
cd nz-server
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw -pl nz-framework/nz-starter-tenant,nz-module/nz-system,nz-app -am test
```
