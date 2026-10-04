---
layout: home
title: 项目文档
hero:
  name: NZ Admin
  text: 从后台基础能力走到自己的业务
  tagline: Java / Vue 后台脚手架。把认证、租户、数据权限、流程与交付接好，让开发工作落在业务本身。
  image:
    src: /logo.svg
    alt: NZ Admin
  actions:
    - theme: brand
      text: 开始使用
      link: /getting-started
    - theme: alt
      text: 查看项目能力
      link: /capabilities
    - theme: alt
      text: 阅读开发指南
      link: /developer-guide
features:
  - title: 业务开发有路径
    details: CLI 创建模块，生成器提供 CRUD 起点；分层、接口和前端约定帮助新模块融入现有项目。
    link: /developer-guide
    linkText: 创建第一个业务模块
  - title: 权限落到数据
    details: 角色菜单和按钮权限、多租户隔离，以及五种数据范围。新增业务表需要明确接入隔离规则。
    link: /data-permission
    linkText: 理解数据访问边界
  - title: 审批连上业务
    details: 复用 Warm-Flow 官方经典设计器，流程中心与请假示例演示提交、办理、回调和业务状态同步。
    link: /workflow
    linkText: 接入业务审批
  - title: 验证覆盖运行环境
    details: Maven、Vitest、真实 PostgreSQL / Redis 和 Chromium 链路，结合迁移检查与 Compose 交付。
    link: /testing
    linkText: 运行测试与验收
---

## 从哪里开始

初次使用先读[项目介绍](overview.md)和[快速开始](getting-started.md)。准备添加业务时，沿[开发指南](developer-guide.md)进入模块、CRUD、数据权限和测试文档。部署时从[生产部署](deployment.md)核对数据库、密钥、文件存储和升级步骤。

项目能力与当前限制集中在[能力总览](capabilities.md)，具体操作以对应指南为准。文档与源码在同一个仓库维护；[文档站维护指南](documentation-site.md)说明构建、校验和部署方式。

## 专题入口

- 开发：[后端接口与事务](/guide/backend/)、[前端页面与路由](/guide/frontend/)、[表设计与迁移](/guide/database/)、[测试验收](/guide/testing/)。
- 使用：[系统管理](/platform/system/)、[认证授权与隔离](/platform/security/)、[流程设计与业务审批](/platform/workflow/)。
- 维护：[生产配置](/operations/configuration)、[集群验证](/operations/cluster)、[备份恢复](/operations/backup)、[按症状排错](/operations/troubleshooting)。

每个专题有独立目录与子页面；正文顶部的阅读路径可返回上级。对参考项目的结构与能力差异，见[RuoYi-Plus 文档对照](/overview/ruoyi-comparison)。
