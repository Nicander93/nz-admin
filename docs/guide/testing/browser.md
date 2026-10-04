# 浏览器与用户路径验收

浏览器测试适合证明“一个用户能完成一次操作”。服务端拒绝和事务回滚仍需后端测试，两者互相补充。

## 启动与账号

先启动可连接的后端，再运行：

```bash
cd nz-web
pnpm exec playwright install chromium
E2E_USERNAME=admin E2E_PASSWORD=admin123 pnpm e2e
```

E2E 默认启动本地 Vite，后端默认 8080。账号来自环境变量，租户默认 default；`E2E_BASE_URL` 和 `E2E_WORKERS` 可覆盖。新引擎用例需要后端 `NZ_WARM_FLOW_ENABLED=true`，测试侧设置 `E2E_WARM_FLOW_ENABLED=true`。

## 登录 fixture

authenticatedPage 在一个 worker 内复用登录的存储状态，各用例仍创建独立上下文。UI 登录只执行一次，避免触发真实登录频控。测试第二用户时创建独立上下文，不复用管理员身份。

## 审批闭环应检查什么

申请人保存草稿 → 提交 → 审批人读取快照并办理 → 业务状态同步。退回场景还要修改原因和日期、重新提交、核对新实例和原快照。

官方设计器运行在 iframe 中，测试用 frameLocator 定位。拖拽节点后明确点击节点打开属性面板，不假定 mouseup 在所有环境都会触发属性面板。保存后离开、重载并重新读取，再发布，避免只证明画布能显示。

## 文档网站验收

```bash
cd docs
pnpm build
cd ../nz-web
pnpm docs:e2e
```

文档测试不需要后端或账号，检查层级导航、深链刷新、搜索、主题与手机宽度。子路径部署须在构建和测试阶段传相同 `DOCS_BASE`。

## 失败时保留证据

查看失败截图和 trace，核对请求是否拒绝、定位器是否命中预期元素、保存内容是否真正持久化。不要用固定长等待掩盖错误；对明确结果等待，如属性面板可见、列表出现状态或后台同步完成。

配置与 trace 保存方式见[测试手册](/testing)，双节点脚本见[集群验收](/operations/cluster)。
