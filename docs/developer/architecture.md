# 架构全景与依赖边界

NZ Admin 是可组合的单体工程。Maven 决定编译时有哪些模块，Spring 自动配置决定运行时启用哪些业务，前端再结合模块状态与授权菜单加载页面。这三层不能用一条依赖箭头代替。

## 编译时：应用直接装配所需能力

```text
nz-app ────────────> nz-module 中的业务模块
   │                         │
   ├──────────────> nz-framework 中的 Starter
   │                         │
   └──────────────> nz-common 中的对象与协议
```

应用可以直接依赖业务、Starter 和 common；业务按需要依赖 Starter/common；Starter 可以组合其他 Starter，但不能依赖业务；common 不反向引用框架或业务。图中的箭头是允许的依赖方向，具体边以[源码目录](/developer/catalog)为准。

| 层 | 放什么 | 避免放什么 |
| --- | --- | --- |
| nz-app | 启动、配置、Flyway、装配 | 独立业务领域规则 |
| nz-common | 返回、分页、模块/任务/通知/审批轻协议 | 对具体业务表和 Service 的调用 |
| nz-framework | 自动配置、通用注解、技术机制与扩展点 | 用户表、单据状态等领域实现 |
| nz-module | DO、Mapper、Service、Controller、业务校验 | 被框架反向引用的实现 |
| nz-web | API、模块 manifest、页面 hook 和视图 | 客户端决定租户或越权过滤 |

system 对 job 有明确业务依赖，清单要求 job 启用；这不是“所有业务模块永远互不依赖”。可选示例与其他模块保持通过公共协议协作，删除或关闭前要检查真实依赖。

## 运行时：按清单与开关装配

```text
应用 classpath
  → 业务 AutoConfiguration.imports
  → nz.modules.<code>.enabled 条件
  → 扫描本模块 Controller / Service / Mapper
  → classpath module.yaml + 当前启用状态
  → 登录后的前端模块状态 + 套餐/角色菜单
  → manifest 的组件前缀 + Vite 页面映射
  → 动态路由与按钮
```

应用主扫描仅覆盖启动侧包，业务扫描由自身自动配置承担。POM 中有 JAR 不等于模块已启用；关闭模块后，数据库历史表仍保留，租户表登记也需要识别这些已知表。

## 请求的数据边界

请求先恢复可信登录与租户，再校验接口功能权限；MyBatis 受管 SQL 添加租户与功能相关角色范围；Service 判断单据归属、状态和并发条件。事务、行数检查与幂等保证失败时不会留下部分成功。

JDBC 不会自动经过 MyBatis 插件；后台回调也没有申请人的 HTTP 上下文。两者必须使用明确的可信租户与对象身份。见[租户接入](/platform/security/tenant-isolation)、[数据范围](/platform/security/data-scope)。

## 扩展与可靠业务协作

公共协议由调用方依赖，业务模块提供实现。例如流程模块不直接调用请假实现，而通过业务 Handler 获取详情和处理有序事件。文件、短信、日志和实时发布也各有接口边界，见[扩展协议](/developer/extensions)。

数据库事务事件提供至少一次投递与失败重试，处理方按实例/序号幂等；外部副作用仍需接收方保护。共享 Redis 解决在线状态，不代替离线消息队列或分布式调度。

## 哪些约束能自动发现

`pnpm catalog:check` 检查源码目录是否过期，同时拒绝 common/Starter 反向依赖、生产依赖环、未声明的跨业务依赖、requiredStarters 无生产依赖支撑、前后端清单不一致和指南缺页。

它分析默认 reactor 显式依赖，不替代 Maven 的完整解析、Spring 条件装配测试或安全集成测试。详细维护规则见[文档契约](/developer/documentation-contract)。
