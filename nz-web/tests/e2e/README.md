# Playwright E2E（本地）

## 1. 安装依赖与浏览器

```bash
cd nz-web
pnpm install
pnpm run e2e:install
```

## 2. 配置环境变量

可基于 `e2e.env.example` 设置运行变量：

```bash
export E2E_BASE_URL=http://127.0.0.1:5173
export E2E_USERNAME=admin
export E2E_PASSWORD=admin123
```

说明：
- `E2E_BASE_URL`：前端访问地址（建议指向本地已启动的 `pnpm dev`）。
- `E2E_USERNAME`、`E2E_PASSWORD`：用于登录的测试账号。

## 3. 运行用例

```bash
pnpm run e2e
```

可选命令：

```bash
pnpm run e2e:headed
pnpm run e2e:ui
```

## 4. 当前冒烟覆盖

- `tests/e2e/auth/login.spec.ts`：登录成功后离开登录页
- `tests/e2e/system/config.spec.ts`：系统参数页面可访问
- `tests/e2e/system/post.spec.ts`：岗位页面可打开新增弹窗

## 5. 最小前置条件

- 后端与接口可用（登录接口可正常返回 token）
- 测试账号存在且具备访问 `/system/config`、`/system/post` 的权限

## 新流程与双节点验收

设置 `NZ_WARM_FLOW_ENABLED=true`、`E2E_WARM_FLOW_ENABLED=true`，数据库升级至 V30。流程用例通过管理接口创建临时普通申请人和角色，验证官方经典画布与两个用户的请假审批；请在独立测试库运行。审批实例和业务快照作为审计记录保留，临时用户/角色会清理。

双节点脚本需要两个后端进程共享 PostgreSQL 和 Redis，均设置 `NZ_CLUSTER_ENABLED=true`、相同 `NZ_CACHE_KEY_PREFIX`，端口分别 8080、8081。在仓库根目录执行：

```bash
E2E_USERNAME=admin E2E_PASSWORD=admin123 node tools/tests/workflow-cluster.mjs
```

可通过 `NZ_CLUSTER_NODE_A` / `NZ_CLUSTER_NODE_B` 修改地址。脚本覆盖共享会话、并发定义版本/发布、业务重复提交、跨节点幂等审批及回调、票据跨节点消费、消息广播和退出撤销。脚本最后退出该登录会话。
