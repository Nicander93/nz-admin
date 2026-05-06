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
export E2E_PASSWORD=123456
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
