# nz-common / nz-framework 测试计划

## 目标

为 `nz-common` 和 `nz-framework` 建立一套轻量但有效的测试保护网，优先覆盖公共工具、框架扩展、权限链路、数据权限、日志、文件、定时任务等高复用、高影响代码。

本计划不追求一次性补满覆盖率，而是优先保护最容易被业务模块间接依赖、改坏后影响范围最大的行为。

## 当前判断

已有基础：

- `nz-framework/nz-starter-test` 已提供测试支撑。
- `BaseMockitoUnitTest` 可用于纯单元测试。
- `BaseDbUnitTest` 可用于 H2 + MyBatis-Plus 相关测试。
- `BaseWebContextUnitTest` 可用于需要 `RequestContextHolder` 和 Sa-Token 登录态的轻量测试。
- `TestContextCleaner` 可统一清理 `RequestContextHolder`、MDC 和 Sa-Token 上下文，避免测试互相污染。
- `nz-framework/nz-starter-datascope` 已有 `DataScopeAspectTest`，可作为框架行为测试参考。
- `nz-module/nz-system` 已有部分 Service 单测，可作为业务层测试风格参考。

主要缺口：

- `nz-common` 暂未形成公共工具和基础模型的回归测试。
- `nz-framework` 中 starter、AOP、拦截器、自动配置、上下文工具等缺少系统性测试。
- 测试分层还不够明确，容易把纯单测写成笨重的 SpringBootTest。
- 缺少一份说明哪些代码值得测、怎么测、用什么命令验收的文档。

## 测试分层

### 纯单元测试

适用范围：

- 工具类。
- 枚举转换。
- 参数校验。
- 字符串、日期、JSON、树结构、分页等输入输出明确的逻辑。
- 不依赖 Spring 容器、不依赖数据库、不依赖网络和文件系统的代码。

推荐方式：

- 使用 JUnit 5。
- 需要 mock 时继承 `BaseMockitoUnitTest`。
- 不启动 Spring 上下文。
- 测试命名体现业务行为，例如 `shouldReturnEmptyListWhenSourceIsNull`。

### Spring 轻量上下文测试

适用范围：

- AOP 切面。
- 自动配置。
- 拦截器。
- 权限注解。
- 请求上下文。
- Bean 条件装配。

推荐方式：

- 使用最小 `@SpringBootTest(webEnvironment = NONE)`。
- 在测试内部声明 `@SpringBootConfiguration` 和局部 `@Configuration`。
- 只注册被测 Bean 和必要依赖。
- 外部依赖优先使用 `@MockBean`。
- 需要模拟当前请求或登录用户时，优先继承 `BaseWebContextUnitTest`，不要在每个测试里手写清理逻辑。

### 数据库相关测试

适用范围：

- MyBatis-Plus 扩展。
- 数据权限 SQL。
- Mapper 相关辅助逻辑。
- 需要验证事务、SQL、插件行为的代码。

推荐方式：

- 继承 `BaseDbUnitTest`。
- 使用 H2。
- 测试数据放在 `src/test/resources/sql/`。
- 每个测试后清理数据，避免顺序依赖。

## 覆盖范围

### P0：优先补齐

- [ ] `nz-common-core` 中输入输出稳定的工具类。
- [ ] 公共返回结构、分页结构、树结构转换逻辑。
- [ ] 异常类和错误码转换逻辑。
- [ ] `nz-starter-datascope` 数据权限切面核心路径。
- [ ] `nz-starter-log` 操作日志切面核心路径。
- [ ] `nz-starter-auth` 权限上下文、登录用户解析、权限注解相关逻辑。
- [ ] `nz-starter-web` 全局异常处理、请求参数处理、响应包装逻辑。

### P1：逐步补齐

- [ ] `nz-starter-mybatis` MyBatis-Plus 配置、分页、字段填充、插件行为。
- [ ] `nz-starter-file` 文件类型校验、路径安全校验、大小限制。
- [ ] `nz-starter-quartz` cron 校验、任务注册、暂停、恢复、并发策略。
- [ ] `nz-common-job` 任务调用参数解析和执行结果处理。
- [ ] starter 自动配置类的条件装配测试。

### P2：稳定后增强

- [ ] 测试覆盖率统计。
- [ ] CI 中加入后端测试命令。
- [ ] 对关键 starter 建立契约测试，避免后续业务模块使用方式被破坏。
- [ ] 补充测试编写规范到模块开发文档。

## 不优先测试的内容

- 只有 Lombok 注解的 POJO。
- 简单常量类。
- 没有分支逻辑的配置属性类。
- 只直接转调第三方库、没有项目规则的薄封装。
- 已由 Spring Boot 或第三方框架充分保证的默认行为。

## 推荐实施顺序

1. 先跑通已有测试，确认当前基线。
2. 给 `nz-common-core` 补 3 到 5 个纯单测，建立最轻测试模板。
3. 给 `nz-starter-log`、`nz-starter-web`、`nz-starter-auth` 各补一个关键行为测试。
4. 扩充 `nz-starter-datascope`，覆盖空参数、管理员、部门、自身、组合权限等路径。
5. 给 `nz-starter-mybatis` 和 `nz-starter-quartz` 补 H2 或轻量 Spring 测试。
6. 将稳定写法沉淀到 `docs/` 中，后续新增框架能力必须同步补测试。

## 编写约定

- 测试文件放在对应模块的 `src/test/java`。
- 测试资源放在对应模块的 `src/test/resources`。
- 纯单测类名使用 `XxxTest`。
- 涉及 Spring 上下文的测试仍使用 `XxxTest`，但测试类内部声明最小测试配置。
- 一个测试只验证一个主要行为，不把多个无关断言塞进同一个用例。
- 测试数据尽量就近定义，复杂 SQL 或 JSON 再放资源文件。
- 优先断言业务结果，不优先断言实现细节。
- 对异常场景使用 `assertThrows`，并断言关键错误码或消息。

## 示例写法

纯单元测试：

```java
class XxxUtilTest extends BaseMockitoUnitTest {

    @Test
    void shouldReturnDefaultValueWhenInputIsBlank() {
        String result = XxxUtil.normalize("");

        assertEquals("default", result);
    }
}
```

轻量 Spring 测试：

```java
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = XxxAspectTest.Application.class
)
@Import(XxxAspectTest.TestConfiguration.class)
class XxxAspectTest {
}
```

需要 Web 上下文的轻量测试：

```java
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = XxxAspectTest.Application.class
)
class XxxAspectTest extends BaseWebContextUnitTest {

    @Test
    void shouldUseCurrentLoginUser() {
        mockLogin(1L);

        // 执行业务断言
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class
    })
    static class Application {
    }

    @Configuration
    static class TestConfiguration {
        // 只注册当前测试需要的 Bean
    }
}
```

## 验收命令

按模块执行：

```bash
cd nz-server
./mvnw test -pl nz-framework/nz-starter-test -am
./mvnw test -pl nz-common/nz-common-core
./mvnw test -pl nz-framework/nz-starter-datascope
./mvnw test -pl nz-framework/nz-starter-web
```

带依赖执行：

```bash
cd nz-server
./mvnw test -pl nz-framework/nz-starter-datascope -am
./mvnw test -pl nz-framework -am
```

全量后端测试：

```bash
cd nz-server
./mvnw test
```

## 验收标准

- P0 模块至少有关键路径测试。
- 新增测试可以在本地通过 Maven Wrapper 执行。
- 测试不依赖真实 MySQL、Redis、OSS、外部网络。
- 失败信息能定位到具体规则或行为。
- 不为了覆盖率测试 Lombok、常量、空配置类。
- 框架模块新增能力时，同步补充对应测试或说明不测原因。

## 风险与注意事项

- Spring 上下文测试不要默认启动完整应用，避免测试慢和环境依赖重。
- Sa-Token、RequestContextHolder 等上下文类测试后要清理状态，优先复用 `BaseWebContextUnitTest` 或 `TestContextCleaner`，不要各自手写一套。
- 数据库测试要保证每个用例之间无顺序依赖。
- 如果测试暴露已有设计耦合，不急于大改，可先用小测试锁住当前行为，再单独规划重构。
- 文档和测试注释保留原编码；新测试文件优先使用 UTF-8。

