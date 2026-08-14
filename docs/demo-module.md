# 示例模块

`nz-demo` 是模块化单体的可移除样例，提供一组完整的示例条目 CRUD。它包含独立 Maven 模块、Spring Boot 自动装配、前端清单、菜单权限、Flyway V7 迁移和单元测试。

## 边界

后端代码位于 `nz-server/nz-module/nz-demo`，只依赖公共对象和 framework starter，不依赖 `nz-system`。接口前缀是 `/api/demo/item`，权限前缀是 `demo:item`。

前端清单位于 `nz-web/src/modules/demo/manifest.ts`。中央注册表通过 `import.meta.glob` 自动发现清单，删除该文件后不需要修改注册数组。页面组件路径是 `demo/item/index`。

## 启停

配置在 `nz-server/nz-app/src/main/resources/application.yml`：

```yaml
nz:
  modules:
    demo:
      enabled: true
```

改为 `false` 并重启后，demo 自动装配不再加载，后端模块状态接口会返回禁用状态，前端动态路由会过滤 demo 页面。此开关不是运行时热切换。

## 删除源码

未投产或数据库尚未执行 V7 时，可以同时删除下列内容：

- `nz-server/nz-module/nz-demo`
- `nz-web/src/modules/demo`
- `nz-web/src/api/demo`
- `nz-web/src/views/demo`
- `nz-web/tests/unit/views/demo`
- 父 POM、`nz-module/pom.xml`、`nz-app/pom.xml` 中的 `nz-demo` 接线
- `application.yml` 中的 `nz.modules.demo`
- V7 demo 迁移及迁移资源测试中的 V7 断言

数据库已经执行 V7 后，不要直接删除迁移文件。Flyway 会把已执行但本地缺失的版本视为校验异常。此时保留 V7，并新增后续迁移删除 `demo_item`、demo 菜单和 `sys_role_menu` 关联；应用升级完成后再移除源码接线。

## 验证

正常接入时运行：

```bash
cd nz-server
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw -pl nz-module/nz-demo,nz-app -am test

cd ../nz-web
pnpm test -- tests/unit/core/modules/registry.test.ts tests/unit/views/demo/item/hooks.test.ts
pnpm exec vue-tsc --noEmit
```

验证可删除性时，在临时副本中按“删除源码”清理后，再运行后端 `nz-app` 反应堆测试和前端类型检查。不要在已执行 V7 的生产库上用删除迁移文件的方式回滚。
