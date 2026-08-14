# nz-admin CLI 工具结论

CLI 已在 `tools/nz-cli` 落地，根目录提供 `nz` 和 `nz.cmd`。

已完成：

- [x] `doctor`：Node.js、Java、pnpm、Maven Wrapper、迁移和模块 POM 接入。
- [x] `dev`：准备 Maven Reactor 后同时启动后端和前端，统一处理退出信号。
- [x] `build`：后端打包与前端生产构建。
- [x] `verify`：后端测试、前端测试和前端构建。
- [x] `migration check`：连续版本、重复版本和手工升级脚本配对。
- [x] `module add`：后端模块、前端清单、菜单权限迁移、测试和 Maven 接入。
- [x] `module enable/disable`：部署期开关。
- [x] `codegen`：使用登录令牌调用生成器下载 ZIP。
- [x] `rename`：受控文本替换。
- [x] `rollback`：恢复原文件并删除命令创建的文件。

安全约束：

- 修改型命令必须使用 `--dry-run` 或 `--yes`。
- 实际写入在 `.nz-cli/backups` 留存原文件和 `backup.json`。
- 备份目录、依赖、构建产物和对标参考仓库不参与重命名。
- CLI 不执行菜单 SQL，不直接访问数据库，不依赖本地 link 包。

验证结果：

- Node 内置测试 19 项通过。
- 在完整临时副本中创建 `audit-center` 模块后，25 个 Maven 模块构建成功。
- 新模块清单、Flyway V9、生成的 Vue 页面类型检查和前端模块注册测试通过。

命令参数和失败条件见 [../cli.md](../cli.md)。

