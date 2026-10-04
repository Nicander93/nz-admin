# 测试分层与验收选择

按需要证明的行为选择测试环境。mock 能证明业务分支，但不能证明 SQL 被正确隔离；构建能证明代码可打包，但不能证明拖拽、登录与审批实际可用。

| 要验证 | 使用什么 | 入口 |
| --- | --- | --- |
| 服务分支、转换和状态 | Mockito / Vitest | [后端测试](/guide/testing/backend)、前端单测 |
| MyBatis 查询和更新隔离 | H2 插件集成、真实 PostgreSQL | [后端测试](/guide/testing/backend) |
| HTTP 登录、按钮授权和迁移 | 独立 PostgreSQL 应用测试 | [通用测试手册](/testing) |
| 点击、表单回填、iframe 拖拽 | Chromium / Playwright | [浏览器验收](/guide/testing/browser) |
| 两节点会话、并发审批和实时状态 | 两个 Java 进程 + PG/Redis | [集群验收](/operations/cluster) |
| 文档导航、搜索和子路径 | 构建静态站 + Chromium | [文档站维护](/documentation-site) |

## 日常验证

```bash
./nz migration check
./nz verify
```

verify 不会自动为你提供真实 PostgreSQL、Redis 或测试账号。集成测试未配置环境时可能跳过，因此报告结果要区分单测通过、集成执行和浏览器验收。

前端 `@nz/test` 提供分页响应、hook 宿主和测试配置，业务 mock 与断言仍留在所属测试文件。工具包不应该硬编码某个业务模块的数据。

## CI 的职责

当前 CI 分为 backend、frontend、cli、delivery、docs、e2e。e2e 在独立库运行真实权限/迁移测试，启动两节点，再运行浏览器和跨节点脚本；docs 不依赖后端账号。失败时从对应阶段日志与 trace 定位，而不是把所有失败都归为“CI 环境问题”。

新增测试要检查对用户重要的行为，例如未授权修改不落库、失败重试不重复启动、退回修改保留旧快照。无需给纯文案改动重复跑完整业务验收。
