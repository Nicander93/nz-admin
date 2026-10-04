# 维护与部署文档站

本站使用 VitePress 1.6.4，内容源就是仓库 `docs/`，不另外复制一套文章。项目介绍、开发者指南和能力说明与代码一同评审。

## 本地运行与构建

```bash
cd docs
pnpm install --frozen-lockfile
pnpm dev
```

默认地址为 `http://localhost:4175`。生产静态产物：

```bash
pnpm build
pnpm preview
```

输出在 `docs/.vitepress/dist/`，可交给静态服务器、Nginx 或支持静态构建的平台。构建默认检查链接；不使用全局忽略死链来掩盖缺页。

## 内容维护

- 首页：`docs/index.md`，介绍项目并提供阅读入口。
- 项目介绍与边界：`overview.md`、`capabilities.md`。
- 入门与开发：`getting-started.md`、`developer-guide.md`，继续引用各功能指南。
- 导航：`docs/.vitepress/sidebar.ts`；搜索与站点配置：`docs/.vitepress/config.ts`。
- 入门细节：`start/`；开发专题：`guide/`；平台操作与接入：`platform/`；运维：`operations/`。
- 主题与图标：`docs/.vitepress/theme/` 和 `docs/public/logo.svg`。

根目录旧文章保留相对 Markdown 链接；多层专题使用站点绝对路径，例如 `/guide/backend/validation`，避免层级移动破坏引用。页面标题使用一级标题，正文从二级标题开始。侧栏使用同一份层级定义，正文面包屑从中推导，不另维护一套目录。仓库内部 `.docs/` 规范和历史执行计划链接由构建转换到对应 GitHub 文件，保留原文供仓库内阅读；它们不进入本站的公开导航与搜索。

新增能力时同时更新能力总览和对应指南，说明启用条件、可重复命令及当前限制。测试数量和临时验收日志放在执行记录中，避免首页出现随时过期的数字。

## 部署到子路径

默认部署根路径。若站点地址是 `https://example.com/nz-admin/`，构建时设置：

```bash
DOCS_BASE=/nz-admin/ pnpm build
```

PowerShell 使用 `$env:DOCS_BASE = '/nz-admin/'` 后执行构建。修改 base 后必须重新构建，服务器路径需要与 base 一致。

根路径部署的 Nginx 示例：

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    location / {
        try_files $uri $uri.html $uri/ /404.html;
    }
}
```

将构建目录内容复制到静态根目录即可。不要把 `docs/.vitepress/cache/`、依赖或环境文件放入公开目录。远端域名和平台部署由使用方配置；仓库 CI 构建并验收静态产物。

## 验证

```bash
cd docs
pnpm build
cd ../nz-web
pnpm exec playwright install chromium
pnpm docs:e2e
```

浏览器验收检查首页、二级/三级导航、面包屑回到上级、指南深链刷新、本地搜索、主题切换和手机导航。CI 同时构建文档、检查页面和上传静态产物。VitePress 的[部署说明](https://vitepress.dev/guide/deploy)提供其他静态托管方式。
