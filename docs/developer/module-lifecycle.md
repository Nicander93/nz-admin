# 模块从声明到运行的生命周期

一个模块可编译、有元数据、实际启用和用户可访问，是四种不同状态。按下面的链路接入，才能同时处理安装、授权、关闭与升级。

## 1. 加入构建

模块放在 `nz-module/nz-<name>`，加入聚合 POM 和应用依赖。CLI 可以创建可编译骨架：

```bash
./nz module add audit-center --title 审计中心 --parent-menu-id 1000 --menu-id 9300 --dry-run
```

核对 ID、文件和迁移后使用 --yes。不要直接让 Starter 引用业务 Service；跨业务调用先考虑轻协议与已声明依赖。

## 2. 声明元数据和自动配置

`META-INF/nz/module.yaml` 包含 code、name、version、description、requiredModules、requiredStarters、defaultEnabled 和 frontendModule。Spring 的 AutoConfiguration.imports 引用本模块入口，由开关控制扫描。

运行注册器采用受限清单格式，使用简单标量和块列表。requiredModules 的缺失/关闭在启动时校验；requiredStarters 目前由文档构建检查 POM 生产依赖可达性，不宣称存在运行时 Starter 健康探测。

## 3. 前端、菜单和访问边界

前端清单 code 与后端 frontendModule 一致，componentPrefix 指向页面归属。菜单迁移声明路径、组件和按钮权限，再给角色及租户套餐授权。注册表发现清单后，路由守卫按实际模块状态过滤。

新表同时登记租户与数据范围；创建字段从登录上下文生成。代码生成只给出起点，不能替代业务对象与状态校验。具体方法见[模块指南](/module-development-guide)和[动态路由](/guide/frontend/routing)。

## 4. 同步源码目录

新增模块在 docs/scripts/catalog.config.mjs 配置阅读入口，补充指南，然后运行：

```bash
cd docs
pnpm catalog:update
pnpm catalog:check
pnpm build
```

自动目录会提取依赖、自动配置入口、公开接口和清单，不需要手工抄一份模块列表。检查生成差异是否准确，再提交到同一变更。

## 5. 启停与卸载

开关改变后重启。关闭后验证后端接口不可用、前端路由过滤，以及保留表通过租户审计。彻底移除还要处理应用依赖、前端清单、菜单和有依赖的模块，不能只删目录。

已部署数据库保留历史迁移。业务数据清理与架构卸载是不同操作，需要按业务和备份方案执行；不要修改已执行迁移来伪装模块从未存在。
