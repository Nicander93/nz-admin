# 任务：拆分 nz-framework 为多个 starter

## 背景与目标

原 `nz-framework` 同时承载 Web、MyBatis-Plus、Sa-Token 配置，职责边界不清晰。

本次目标：

- 将 `nz-framework` 改为父模块
- 按职责拆分多个 starter
- 提供 `nz-starter-web`，内置跨域、全局异常、Swagger

## 范围与非目标

范围：

- 调整 `nz-server` Maven 模块结构
- 迁移原有配置类到对应 starter
- 修改 `nz-app` 依赖
- 更新架构文档

非目标：

- 不调整业务模块代码
- 不改动现有接口路径与鉴权规则

## 受影响包

- `nz-server/pom.xml`
- `nz-server/nz-framework`
- `nz-server/nz-app`
- `.docs/architecture.md`

## 实施步骤

1. 将 `nz-framework` 改为 `pom` 父模块，并声明子模块
2. 新增 `nz-starter-web`、`nz-starter-mybatis`、`nz-starter-sa-token`
3. 将原配置迁移为 Spring Boot 自动配置
4. 让 `nz-app` 直接依赖新的 starter
5. 更新文档并验证构建

## 决策记录

- `starter-web` 仅承载通用 Web 能力，避免直接依赖 Sa-Token
- Sa-Token 的未登录/无权限异常处理跟随 `nz-starter-sa-token`
- 新 starter 使用 `AutoConfiguration.imports` 暴露自动配置，避免依赖应用包扫描

## 验证方式

- 在 `nz-server` 根目录执行 `mvn test -pl nz-app -am`
- 检查 `swagger-ui/index.html` 是否可访问
- 检查 `/api/**` 鉴权与跨域行为是否保持不变

## 收尾结论

已完成模块拆分，`nz-framework` 不再直接承载运行时代码，改为聚合父模块；
`nz-app` 通过组合 starter 获得 Web、MyBatis-Plus、Sa-Token 能力，后续扩展边界更清晰。
