# Agent 任务配方

## 新增 CRUD 模块

1. 确认是业务模块还是 framework starter。
2. 新增 DTO、VO、DO、Mapper、Service、Controller；Controller 不写业务逻辑，DO 不直接返回。
3. 同步前端 API、页面 index.vue/hook.ts、菜单和按钮权限 SQL。
4. 增加后端 Test 和前端 tests/unit 测试，并运行相关构建。

## 新增 starter

只承载跨业务能力；添加自动配置、条件化 bean、最小配置和独立测试，禁止依赖 nz-system。

## SQL、权限和测试

权限串在 Controller、前端 v-permission 和 SQL 中保持一致。初始数据写 init.sql，升级写版本化脚本。后端测试镜像生产包，前端测试镜像 src；外部 Redis、OSS、网络必须 mock。

## 同步检查

检查 API、类型、SQL、权限、测试、文档、环境变量与 CI 命令。

