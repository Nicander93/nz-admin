# 后端单测与真实数据库验证

业务单测放在所属模块，测试公共工具来自 `nz-starter-test`。选择 BaseMockitoUnitTest 验证分支，选择 BaseDbUnitTest 验证模块 schema 与实际数据访问。

## 日常与聚焦测试

```bash
cd nz-server
./mvnw test
```

聚焦应用权限测试时，用 Maven 选测试类，并让未包含该类的上游模块正常通过：

```bash
./mvnw -pl nz-app -am test   -Dtest=DataScopePostgresTest   -Dsurefire.failIfNoSpecifiedTests=false
```

上例还需要下面的真实数据库环境。不要把“没有运行目标测试但构建成功”记作验证通过。

## 真实 PostgreSQL

```bash
export NZ_TEST_PG_URL=jdbc:postgresql://127.0.0.1:5432/nz_test
export NZ_TEST_PG_USERNAME=postgres
export NZ_TEST_PG_PASSWORD=your-test-password
export NZ_TEST_REDIS_PORT=6379
```

必须使用独立测试库与测试 Redis。应用测试会写固定测试 ID，并设置测试库管理员密码，不能连接开发共享库或业务生产库。DataScopePostgresTest 未提供数据库 URL 时自动跳过；测试报告应检查 skip 数量。

## HTTP 断言

```java
var api = new ApiTestClient(restTemplate)
        .login("default", "admin", "admin123");
var data = api.ok(HttpMethod.GET, "/api/system/user/page", null);
```

`ok` 同时断言 HTTP 成功与业务成功码，返回 data。用 exchange 检查失败响应；登录返回新客户端身份，不修改共享实例。覆盖跨租户、无关全量角色不扩大编辑范围、按钮权限和写入影响，而不只断言列表长度。

## 工作流与集群状态

WarmFlowPostgresTest 验证设计器白名单、用途绑定、回调失败重试及高级办理动作。故障注入使用独立数据库，避免另一实际节点抢先消费事件。Warm-Flow 的静态 Spring 上下文不能在同一 JVM 任意切换 H2 / PostgreSQL，应用集成测试按类使用独立进程。

Redis 测试使用专属前缀，不关闭生产频控来迁就测试。更多类名和命令见[通用测试手册](/testing)。
