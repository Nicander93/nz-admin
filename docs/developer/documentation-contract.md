# 源码与文档同步契约

文档目录容易随着重构过期。本项目把可自动确认的结构从源码提取，把阅读入口和能力边界保留为明确的人工说明；构建负责核对二者。

## 自动提取什么

- 从根 Maven reactor 递归读取模块 POM、描述和显式仓库内依赖。
- 读取业务 module.yaml、前端 manifest 与自动配置 imports，验证引用的源文件存在。
- 列出真实公开接口和测试源文件入口，关联手工维护的专题指南。
- 生成 docs/data/source-catalog.json 和 docs/reference/source-index.md；前者用于筛选，后者进入本地全文搜索。

输出没有当前时间，采用稳定排序与规范换行，可重复生成。源码输入有摘要，重要声明/接口改变后，CI 会发现快照过期。

## 修改流程

```bash
cd docs
pnpm catalog:update
pnpm catalog:check
pnpm test:catalog
pnpm build
cd ../nz-web
pnpm docs:typecheck
```

`catalog:update` 更新生成文件，`catalog:check` 只读核对；build 先做 check，不在 CI 悄悄改文件。改模块结构时同步 scripts/catalog.config.mjs 的阅读入口与能力说明，检查生成差异再提交。

## 哪些问题会被拒绝

common 反向依赖框架/业务、Starter 依赖业务、业务依赖启动层、生产依赖环、未在清单声明的跨业务依赖、必需模块或 Starter 无依赖支撑、重复业务编码/前端前缀、无效自动配置源文件、指南缺页以及陈旧输出。

requiredStarters 检查的是默认 reactor 显式生产依赖可达性，不是运行时 Bean 健康；Maven profiles、继承后的完整有效 POM 和外部版本冲突仍要由 Maven 检查。测试依赖、provided/system 依赖与生产依赖分开显示；前两类不用于满足必需 Starter。清单检查遵循当前运行时支持的标量与分行列表格式，拒绝运行时无法同样解释的写法。

## 哪些事情仍需人和行为测试

源文件存在不能证明功能正确；接口名也不能证明供应商已经连通。能力状态和限制由实现者维护，必须有对应测试和源码评审。测试源文件数量不代表执行数量或通过结果。

新 API、SQL 权限、并发、回调与浏览器操作仍按[验收分层](/guide/testing/)验证。本站不把无证据的路线图包装成已交付，也不把静态索引当成在线应用状态。
