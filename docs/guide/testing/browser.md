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

## 用户、角色和菜单闭环

`system/access-control.spec.ts` 使用专用随机命名的菜单、角色和用户，通过真实页面创建和授权；另开普通用户上下文，验证只有自身数据、无导航菜单时的个人中心落点，以及禁用/恢复角色和菜单后，同一令牌的真实接口拒绝与恢复。清理只针对本用例创建的精确记录，不停用共享系统菜单。

浏览器和双节点脚本使用真实生产频控。`loginByUi` 与脚本的 `loginByHttp` 只有在登录返回 `429` 且存在有效 `Retry-After` 时才等待并重试一次；其他错误直接失败。不通过关闭防重、提高限额或清空共享状态让浏览器测试通过。

## 浏览器测试的类型门禁

`pnpm e2e:typecheck` 检查现有应用 E2E、通用测试夹具和 Playwright 配置，使用工作区直接声明的 Node 类型。CI 在启动浏览器前检查这些文件，避免只做转译而遗漏类型错误。

文件管理回归位于 `tests/e2e/system/file-lifecycle.spec.ts`。它使用真实后端上传、校验下载内容、删除，并验证批量部分失败；数据只按本用例生成的唯一文件名清理。精准执行使用 `pnpm exec playwright test tests/e2e/system/file-lifecycle.spec.ts`，不要在文件路径前额外传入 `--`。

应用 E2E 默认先执行生产构建，再启动 Vite preview，并代理后端 API 与实时连接。这样测试过程不会被开发依赖预构建或 HMR 自动刷新打断。调试开发模式时，可以通过通用测试包的 `serverCommand` 显式覆盖启动命令。


## 任务失败与重试

`system/job-retry.spec.ts` 使用真实 PostgreSQL 成功日志触发器制造一次受控存储故障。任务仍由 Quartz 实际执行，失败账本与失败日志保留；移除故障后通过页面重试，核对同一执行键的第二次尝试、成功状态与两条尝试日志。更新任务配置后，旧失败记录必须隐藏重试，直接调用旧回执也会被拒绝。

启用该项需要 `E2E_JOB_FAULTS_ENABLED=true`、`E2E_PG_URL`（也可使用 `NZ_DATASOURCE_URL`）和 `PGUSER/PGPASSWORD`，并提供可创建触发器的独立测试数据库。客户端默认使用 `psql`，可用 `E2E_PSQL_BIN` 指定路径。CI 的 PostgreSQL 服务显式启用此项；缺少凭据时启用测试会失败，关闭开关时明确跳过，不计为已验收。

故障仅匹配本次随机任务的 ID 与名称，安装前会与 API 返回核对；清理先移除本次触发器/函数，再删除该任务和日志。SQL 通过 stdin 交给 psql，凭据通过环境变量传递，连接 URL 不保留密码。
