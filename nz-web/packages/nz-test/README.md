# @nz/test

NZ Admin 内部测试支持 workspace 包。导出 `./unit`、`./e2e`、`./config`，使用方式见 [测试文档](../../../docs/testing.md)。业务 mock 和断言由调用方维护。

浏览器配置默认运行生产构建与 preview，避免测试中途触发开发服务器重载；需要不同服务时传入 `serverCommand`。应用 E2E 类型检查使用 `pnpm e2e:typecheck`。
