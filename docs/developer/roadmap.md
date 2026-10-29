# 功能对标与持续推进

对标以 RuoYi-Vue-Plus 5.X 的成熟后台能力和 plus-doc 的专题分类为参考；6.X 扩展目录用于观察方向。NZ Admin 继续以 Java 17、PostgreSQL、Vue3 和可装配单体为当前基线，不为目录齐全直接堆入所有组件。

参考：[上游项目说明](https://github.com/dromara/RuoYi-Vue-Plus/blob/5.X/README.md)、[5.X 文档](https://github.com/dromara/plus-doc/blob/master/5.X/ruoyi-vue-plus/_sidebar.md)、[6.X 文档](https://github.com/dromara/plus-doc/blob/master/6.X/ruoyi-vue-plus/_sidebar.md)。

## 已形成的基础

系统管理、角色菜单按钮、租户套餐与记录隔离、五种功能数据范围、字段加密、文件配置、通知与登录扩展、CRUD 生成、模块 CLI 和交付环境。官方经典设计器、流程中心与业务回调连接审批与单据；验证覆盖真实数据库、浏览器和两节点状态。

具体启用条件和限制以[能力总览](/capabilities)与[可筛选目录](/developer/catalog)为准。

## 本轮增强

| 能力 | 交付重点 | 开发者入口 |
| --- | --- | --- |
| Excel 批量交换 | 模板、逐行预检、确认时重新校验与原子新增、功能数据范围导出 | [Excel 指南](/guide/backend/excel) |
| 架构与模块透明度 | POM、清单、自动配置、前端路径、公开接口与指南一同查找 | [源码目录](/developer/catalog) |
| 文档持续正确 | 结构契约、源文件摘要、静态全文索引和 CI 防漂移 | [同步契约](/developer/documentation-contract) |

## 下一阶段按业务价值选择

1. **数据管理更完整**：更多业务接入导入导出；大任务可考虑异步处理、进度、审计和可恢复重试。先明确规模与一致性，当前同步小批量导入不等于大数据平台。
2. **流程运营**：回调积压监控、受权重试、实例运营视图。先保留租户、参与人和历史快照边界，不直接开放底表修改。
3. **分布式任务**：本轮沿用 Quartz，接入 PostgreSQL JDBC store、配置 outbox 和执行账本；真实两进程竞争/硬崩溃及浏览器回归验收进行中。完成标准包含业务效果与原执行键收敛，共享 Redis 不能代替调度验证。
4. **按需扩展**：动态数据源、通用表单、可观测平台或基于文档的问答。先确定业务场景、运维成本和许可，再做独立切片。

## 每项能力的完成标准

API、界面、权限/租户/对象边界、迁移、测试与文档一起交付。报告区分本地、真实外部服务、CI 和生产目标环境；有方案、可编译与用户完整完成操作是不同阶段。
