# 项目 CLI

项目根目录的 `nz` 和 `nz.cmd` 是统一工程入口。CLI 使用 Node.js 内置模块实现，不需要单独安装依赖。Node.js 版本必须不低于 22.13。

Linux、macOS 和 WSL 使用：

```bash
./nz help
```

Windows 使用：

```powershell
.\nz.cmd help
```

## 环境检查

```bash
./nz doctor
./nz migration check
```

`doctor` 检查 Node.js、Java 17、pnpm、Maven Wrapper、Flyway 版本连续性和业务模块的 Maven 接入。任一必需项失败时命令返回非零退出码。

`migration check` 还会检查：

- Flyway 文件从 V1 开始且版本连续。
- 同一版本没有重复文件。
- V2 之后每个 Flyway 版本都有一个 `upgrade-p<版本>-*.sql`。
- 手工升级脚本没有指向不存在的 Flyway 版本。

## 开发、构建和验证

```bash
./nz dev
./nz build
./nz verify
```

`dev` 先安装后端 Reactor 依赖，再同时启动后端和前端。按 `Ctrl+C` 会停止两个子进程。依赖已经准备好时可以使用 `./nz dev --no-prepare`。

`build` 执行后端打包和前端生产构建；`verify` 执行后端测试、前端测试、前端生产构建和 CLI 测试。两条命令都支持 `--backend-only` 或 `--frontend-only`。

## 交付环境

```bash
cp deploy/.env.example deploy/.env
./nz delivery check --env deploy/.env --compose
./nz delivery up --yes
./nz delivery smoke --url http://127.0.0.1
./nz delivery ps
./nz delivery down --yes
```

`delivery check` 检查 Dockerfile、Nginx、Compose、健康检查、数据卷和环境变量，并拒绝示例密码或过短密钥。添加 `--compose` 时必须同时传入 `--env`，命令会调用本机 Docker Compose 做真实配置解析。

`delivery up` 根据环境文件自动选择 profile：

- `NZ_REDIS_ENABLED=true` 启用 `redis`；
- `NZ_FILE_STORAGE_TYPE=s3` 启用 `storage`；
- `--profile redis,storage` 可以显式追加 profile。

`up` 和 `down` 会改变容器运行状态，必须显式添加 `--yes`。`down` 不删除数据卷；CLI 不提供删除卷命令，避免误删数据库和上传文件。

`delivery smoke` 依次验证前端 HTML、Nginx 存活端点和后端 readiness。它只接受成功响应，不负责启动服务。

## 创建模块

先检查计划：

```bash
./nz module add audit-center \
  --title 审计中心 \
  --description 审计事件管理 \
  --parent-menu-id 1000 \
  --menu-id 9300 \
  --dry-run
```

确认文件列表和迁移版本后执行：

```bash
./nz module add audit-center \
  --title 审计中心 \
  --description 审计事件管理 \
  --parent-menu-id 1000 \
  --menu-id 9300 \
  --yes
```

模块编码使用小写字母、数字和连字符。命令会一次生成并接入：

- `nz-module/nz-<code>` 后端模块、自动装配、模块清单、入口接口和清单测试。
- 前端模块清单、API、入口页面和注册表测试。
- 根 POM、`nz-module` 聚合 POM 和 `nz-app` 依赖。
- `application.yml` 模块开关。
- 下一版 Flyway 迁移、对应手工升级脚本、菜单权限和管理员授权。
- `FlywayMigrationResourcesTest` 的新资源声明。

未提供 `--menu-id` 时，CLI 会根据已有迁移中的菜单 ID 选择下一个十位边界。生产项目建议显式指定 ID，避免并行分支得到相同值。

该命令创建的是可编译、可访问的模块骨架，不生成业务表 CRUD。创建模块后，再通过代码生成器下载 CRUD 文件。

## 模块启停

```bash
./nz module disable demo --dry-run
./nz module disable demo --yes
./nz module enable demo --yes
```

命令只修改 `nz.modules.<code>.enabled`。后端重启后生效；前端会根据后端模块状态过滤动态路由。启停不会删除模块代码、菜单或数据。

## 调用代码生成器

先准备请求文件，例如 `generator-request.json`：

```json
{
  "schemaName": "public",
  "tableName": "sys_notice",
  "moduleName": "system",
  "businessName": "notice",
  "className": "Notice",
  "packageName": "com.nz.admin.modules.system",
  "featureName": "通知管理",
  "author": "nz-admin",
  "parentMenuId": 1000
}
```

检查请求：

```bash
./nz codegen --request generator-request.json --dry-run
```

下载 ZIP：

```bash
NZ_TOKEN=<登录令牌> ./nz codegen \
  --request generator-request.json \
  --server http://localhost:8080 \
  --output generated/notice.zip \
  --yes
```

输出文件必须位于项目目录内。令牌可以使用 `--token` 传入，也可以放在 `NZ_TOKEN` 环境变量中。CLI 不会打印令牌。服务端返回 JSON 业务错误时，命令不会把错误响应保存成 ZIP。

## 项目标识替换

```bash
./nz rename --from nz-admin --to acme-admin --dry-run
./nz rename --from nz-admin --to acme-admin --yes
```

`rename` 只替换文本文件内容，不改文件名、目录名和 Java 包路径。命令跳过 `.git`、`.nz-cli`、`.serena`、`node_modules`、`target`、`dist` 和仓库内的对标参考目录 `ruoyi-vue-pro`。重命名后必须运行 `./nz verify`。

## 备份与回滚

所有实际修改型命令都要求 `--yes`，并在 `.nz-cli/backups/<backup-id>` 保存原文件和操作清单。先用 `--dry-run` 查看回滚范围：

```bash
./nz rollback <backup-id> --dry-run
./nz rollback <backup-id> --yes
```

回滚会恢复被修改或覆盖的文件，并删除该操作创建的文件。备份记录会保留，`backup.json` 中写入回滚时间。

## 测试

```bash
cd tools/nz-cli
node --test
```

测试覆盖命令解析、确认门槛、Flyway 断档和重复版本、模块创建与回滚、模块启停、生成器下载、项目重命名以及构建命令调度。模块模板还应在完整项目临时副本中执行 Maven 编译和 Vue 类型检查。
