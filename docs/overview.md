# 项目介绍

NZ Admin 是前后端分离的后台脚手架，为需要持续扩展业务的 Java / Vue 项目提供系统管理、权限、通用框架组件和交付起点。

项目参考 RuoYi 系列的常见后台能力，结合自己的模块边界接入。工作流复用 Warm-Flow 官方设计器与引擎；项目介绍和开发指南以本仓库实现为准。

## 适合什么项目

- 需要用户、角色、菜单、部门、字典、参数和日志的管理后台。
- 需要按租户、部门或本人限定业务数据访问的应用。
- 希望新业务模块能独立装配，并沿用统一 API、测试和数据库迁移的团队。
- 需要把业务申请、审批任务和业务状态串起来的系统。

## 技术组成

| 部分 | 组成 |
| --- | --- |
| 后端 | Java 17、Spring Boot、MyBatis-Plus、Sa-Token、Hutool、Maven Wrapper |
| 前端 | Vue 3、TypeScript、Vite、Pinia、Element Plus、UnoCSS、pnpm |
| 数据与状态 | PostgreSQL、Flyway；集群模式使用 Redis 共享状态 |
| 工作流 | Warm-Flow 1.8.9 与同版本官方经典设计器 |
| 验证与交付 | JUnit、Vitest、Playwright、GitHub Actions、Docker Compose |

具体依赖版本由 `pom.xml` 和 `package.json` 管理。环境要求与启动步骤见[快速开始](getting-started.md)。

## 目录入口

```text
nz-admin/
├─ nz-server/       后端 Maven 多模块
│  ├─ nz-app/       启动、配置、迁移
│  ├─ nz-common/    轻量公共协议
│  ├─ nz-framework/ 框架 starter
│  └─ nz-module/    系统与可选业务模块
├─ nz-web/          管理控制台
├─ docs/            文档源与文档站
├─ tools/           项目 CLI、验收脚本
└─ deploy/          镜像、Compose、代理配置
```

框架层保持通用机制，业务模块负责领域规则；依赖方向和扩展点见[架构说明](architecture.md)。

## 能力和扩展边界

[能力总览](capabilities.md)区分已提供的能力、启用条件与限制。动态数据源、通用表单设计器和 Warm-Flow 仿钉钉模型尚未接入。请假是业务接入示例，实际业务仍需自己的表单、状态规则与处理器。

接下来可以[启动项目](getting-started.md)，或直接阅读[开发指南](developer-guide.md)。

## 深入使用与开发

先从[能力总览](/capabilities)选任务，再进入二级专题和三级操作指南。开发者可沿[后端](/guide/backend/)、[前端](/guide/frontend/)、[数据库](/guide/database/)和[测试](/guide/testing/)阅读；管理员可按[系统管理](/platform/system/)、[权限](/platform/security/)和[审批](/platform/workflow/)操作。

本站参考成熟框架的文档组织，按 nz-admin 的实际实现编写。[RuoYi-Vue-Plus 对照](/overview/ruoyi-comparison)列出参考结构与当前差异。
