# 后端开发

后端开发的起点是一条可验证的业务操作：谁可以调用、允许修改哪条记录、成功后返回什么。确定这三件事，再组织 Controller、Service 和 Mapper。

## 选择下一步

| 当前任务 | 指南 | 完成后应得到 |
| --- | --- | --- |
| 创建新的业务领域 | [模块开发](/module-development-guide) | 独立模块、清单、菜单迁移和页面入口 |
| 定义接口和分页 | [接口与分页契约](/guide/backend/api-contract) | 前后端一致的响应和查询参数 |
| 校验输入与报告错误 | [参数校验与异常](/guide/backend/validation) | 清楚的拒绝原因，避免无效数据入库 |
| 编写事务和 SQL | [持久化与事务](/guide/backend/persistence) | 有租户与归属边界的数据操作 |
| 处理连点、并发和重试 | [限流、防重与幂等](/guide/backend/protection) | 对应保护机制与并发测试 |

## 代码放置

`nz-app` 管启动和迁移，`nz-module` 管业务，`nz-framework/nz-starter-*` 管可复用机制，`nz-common` 管轻量公共对象与协议。框架不依赖业务实现；需要调用业务时定义协议，由业务模块提供实现。

例如工作流通过公共 `NzWorkflowBusinessHandler` 回调业务，不直接引用请假服务。相同原则适用于文件、通知和用户解析扩展。完整边界见[架构](/architecture)。

## 先复用已有实现

简单 CRUD 可从 [demo](/demo-module) 或[代码生成器](/code-generator)开始。生成代码需要补业务状态检查、租户与数据范围登记、写入影响行数检查和测试。复杂审批按[业务接入](/platform/workflow/business-integration)接通单据与流程，不让页面直接编排内部实例操作。

交付一个后端功能时，同时检查接口权限、对象归属、事务失败回滚和重复请求；方法返回成功不等于数据库行为正确。验证入口见[后端测试](/guide/testing/backend)。
