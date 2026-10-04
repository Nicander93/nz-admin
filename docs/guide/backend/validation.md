# 参数校验与异常处理

校验先回答两个问题：输入格式是否正确，以及当前业务状态是否允许执行。格式由请求对象与 Bean Validation 检查；业务规则由 Service 检查。

## 请求体校验

请假草稿的实际请求对象使用以下约束：

```java
public record Draft(
    @NotBlank @Size(max = 1000) String reason,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_-]{0,63}") String flowCode
) {}
```

Controller 参数加 `@Valid @RequestBody` 才会触发这些约束。注解来自 `jakarta.validation`。日期类型使用 `LocalDate`，前端按 `YYYY-MM-DD` 提交，不必把纯日期转换成带时区时间戳。

## Service 检查业务关系

单字段合法，不代表组合合法。请假仍要检查结束日期不早于开始日期、跨度不超过允许范围、单据属于当前申请人、审批中不能修改，以及退回后不能任意切换流程。

归属和权限不能通过 `@NotNull` 代替；传入一个有效 ID 也可能指向其他租户或其他人的记录。校验失败抛出 `BusinessException`，让统一异常处理输出可理解的原因。

## 统一错误处理

| 情况 | 当前处理 |
| --- | --- |
| 请求体或绑定校验失败 | 业务码 400，返回字段错误信息 |
| 业务规则拒绝 | 使用 BusinessException 的码和消息 |
| 明确 HTTP 错误 | ResponseStatusException 保留实际 HTTP 状态 |
| 未分类系统异常 | 记录日志，返回统一系统错误 |

不要在每个 Controller catch 所有异常并返回成功；这会掩盖事务失败，也使调用方无法重试。日志记录详细原因，面向用户的响应不包含堆栈、数据库凭据或 SQL 内部细节。

## 应补的测试

对请求对象测试空值、长度和非法格式；对 Service 测试跨用户、跨租户、非法状态与边界日期。失败用例还要断言没有生成新流程、没有修改单据，而不只是断言抛出异常。

实现参考：[请假入口](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-demo/src/main/java/com/nz/admin/modules/demo/controller/LeaveApplicationController.java)、[全局异常处理](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-web/src/main/java/com/nz/admin/framework/web/exception/GlobalExceptionHandler.java)。下一步见[后端测试](/guide/testing/backend)。
