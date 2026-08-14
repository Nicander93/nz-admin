# 代码生成器

`nz-generator` 从当前 PostgreSQL 数据源读取表和字段元数据，生成一套符合 nz-admin 分层规则的 CRUD 代码。生成器只提供预览和 ZIP 下载，不直接写入仓库，也不执行生成的菜单 SQL。

## 使用条件

- 数据库为 PostgreSQL，目标表位于可访问的 Schema。
- 目标表只有一个主键。无主键和复合主键会返回业务错误。
- 目标 Maven 业务模块已经存在。生成器会把文件路径指向 `nz-module/nz-<module>`，但不会替你修改聚合 POM 或创建模块清单。
- 当前用户拥有代码生成菜单及按钮权限。

模块开关位于 `application.yml`：

```yaml
nz:
  modules:
    generator:
      enabled: true
```

关闭后重启应用，后端模块状态会变为禁用，前端动态路由也会过滤 `generator` 页面。

## 操作步骤

1. 打开「开发工具 / 代码生成」。
2. 输入 Schema 和表名关键字，查询可生成的表。
3. 点击「配置生成」，确认模块名、业务名、类名、Java 包、功能名称和父菜单 ID。
4. 先预览文件。左侧文件列表与最终 ZIP 使用同一份渲染结果。
5. 下载 ZIP，解压到项目根目录后检查差异。
6. 执行 ZIP 内 `sql/<module>_<business>_menu.sql`，再运行后端和前端测试。

父菜单 ID 只写入生成的 SQL。填 0 会生成一级菜单；指向业务目录时，生成页面会挂到该目录下。

## 生成内容

后端生成以下文件：

- DO、Query、CreateRequest、UpdateRequest、VO 和 Convert。
- Mapper、Service、ServiceImpl 和 Controller。
- 分页查询、详情、新增、修改、删除接口及对应权限注解。

前端生成以下文件：

- `src/api/<module>/<business>.ts`。
- `src/views/<module>/<business>/hooks.ts`。
- `src/views/<module>/<business>/index.vue`。

菜单 SQL包含页面权限和查询、新增、修改、删除按钮权限，并给 `admin` 角色授权。脚本用权限标识判断是否已存在，允许重复执行。

## 接口

| 方法 | 路径 | 权限 | 作用 |
| --- | --- | --- | --- |
| GET | `/api/generator/tables` | `generator:table:list` | 按 Schema 和关键字查询表 |
| GET | `/api/generator/columns` | `generator:table:query` | 查询表字段、类型和主键 |
| POST | `/api/generator/preview` | `generator:table:preview` | 返回字段和文件内容 |
| POST | `/api/generator/download` | `generator:table:download` | 下载 UTF-8 ZIP |

Schema、表名、模块名、业务名、类名和包名都经过格式校验。元数据查询使用参数绑定；ZIP 路径在输出前会拒绝绝对路径、反斜杠和 `..`。

## 模板维护

模板位于：

```text
nz-server/nz-module/nz-generator/src/main/resources/generator/templates
├─ backend
├─ frontend
└─ sql
```

`GeneratorTemplateRenderer` 负责数据库类型映射、字段命名和模板变量替换。新增模板时要同时更新模板定义和 `GeneratorTemplateRendererTest` 的文件数量及关键文件断言。预览和 ZIP 不应各维护一套渲染逻辑。

当前类型映射覆盖 PostgreSQL 常用数值、布尔、日期时间、UUID、二进制和字符串类型。JSON、数组及未识别类型按字符串生成，落地前需要根据业务模型调整。

## 数据库迁移

新库由 `V8__generator_module.sql` 创建「开发工具 / 代码生成」菜单和按钮权限。已有环境若没有使用 Flyway，可执行 `db/upgrade-p8-generator.sql`。已经由 Flyway 执行 V8 的环境不要再手工执行升级脚本。

## 验证

```bash
cd nz-server
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw -pl nz-module/nz-generator,nz-app -am test

cd ../nz-web
pnpm test
pnpm build
```

模板测试会验证 14 个输出文件、主键 getter、权限 SQL、未替换变量和 ZIP 条目。前端测试覆盖模块自动发现、参数推导、表字段读取和代码预览。
