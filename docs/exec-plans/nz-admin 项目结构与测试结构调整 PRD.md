# nz-admin 项目结构与测试结构调整 PRD

## 1. 背景

`nz-admin` 是一个自用的现代化前后端管理系统脚手架，用于替代或改善传统 `RuoYi` 类后台项目在技术栈、代码质量、工程规范、测试能力、可维护性上的不足。

当前项目已经具备基本的前后端能力，但还需要进一步明确：

* 仓库顶层结构；
* 后端模块结构；
* 前端目录结构；
* 后端测试结构；
* 前端测试结构；
* 后续 AI coding agent 友好的项目说明文件结构。

本 PRD 的目标是先完成项目结构与测试结构的整理，不引入复杂功能，不做大规模业务重构。

---

## 2. 目标

### 2.1 核心目标

建立一套清晰、稳定、适合长期维护的项目结构，使 `nz-admin` 具备以下特征：

1. 前后端边界清晰；
2. 顶层目录不过度混乱；
3. 后端模块边界明确；
4. 前端页面、接口、组件、状态管理职责清楚；
5. 测试代码与生产代码结构保持对应关系；
6. 方便人类开发者和 coding agent 快速理解项目；
7. 后续新增业务模块时，有稳定的结构范式可遵循。

### 2.2 非目标

本阶段不做以下事项：

1. 不引入微服务；
2. 不引入复杂 DDD 分层；
3. 不重写现有业务逻辑；
4. 不实现完整 AI 助手能力；
5. 不做代码生成器；
6. 不做权限系统大改；
7. 不做数据库迁移工具引入，除非调整结构时顺手整理文档入口。

---

## 3. 总体设计原则

1. **源码和测试分离，但结构镜像。**

   * 后端遵循 Maven 标准：`src/main/java` 与 `src/test/java` 对应。
   * 前端遵循：`src` 与 `tests/unit` 对应。

2. **业务域优先，技术层次次之。**

   * 后端业务模块内部按业务域组织，例如 `user`、`role`、`menu`。
   * 前端页面按路由和业务域组织，例如 `views/system/user`。

3. **不过度设计。**

   * 暂不强制 `domain/application/infrastructure/interfaces` 等复杂 DDD 结构。
   * 保持管理系统脚手架应有的直接性和可理解性。

4. **保留测试分类能力，但不依赖目录分类。**

   * 后端通过命名区分单元测试和集成测试：

     * `*Test.java`
     * `*IT.java`
   * 前端通过目录和命名区分：

     * `tests/unit/**/*.test.ts`
     * `tests/e2e/**/*.spec.ts`

5. **为 coding agent 提供明确入口。**

   * 本阶段预留 `AGENTS.md` 和 `.ai/` 目录。
   * 重点是让后续 agent 明确项目结构、开发规范、测试规范。

---

## 4. 仓库顶层结构

### 4.1 目标结构

将仓库顶层整理为：

```text
nz-admin/
  AGENTS.md
  README.md
  docs/
  .ai/
  deploy/
  scripts/

  nz-server/
    pom.xml
    nz-app/
    nz-common/
    nz-framework/
    nz-module-system/
    nz-module-example/

  nz-web/
    package.json
    src/
    tests/
```

### 4.2 目录说明

#### `AGENTS.md`

coding agent 的仓库入口说明文件。用于说明：

* 项目定位；
* 技术栈；
* 目录结构；
* 常用命令；
* 开发约束；
* 测试要求；
* 任务完成前检查项。

#### `.ai/`

AI 友好型项目知识目录。当前阶段只建立基础结构，可以先不填充完整内容。

建议结构：

```text
.ai/
  project-brief.md
  architecture.md
  backend-guidelines.md
  frontend-guidelines.md
  testing-guidelines.md
  skills/
    add-backend-crud-module.md
    add-frontend-crud-page.md
    add-test-case.md
  decisions/
    0001-project-structure.md
    0002-testing-structure.md
```

#### `docs/`

普通项目文档目录，面向开发者阅读。

#### `deploy/`

部署相关文件，例如：

```text
deploy/
  docker/
  nginx/
  sql/
```

#### `scripts/`

开发、构建、部署辅助脚本。

#### `nz-server/`

后端工程根目录。后端 Maven 多模块统一放在此目录下。

#### `nz-web/`

前端工程根目录。

---

## 5. 后端结构设计

### 5.1 后端模块目标结构

```text
nz-server/
  pom.xml
  nz-app/
  nz-common/
  nz-framework/
  nz-module-system/
  nz-module-example/
```

### 5.2 模块职责

#### `nz-app`

应用启动模块。

职责：

* Spring Boot 启动类；
* 应用装配；
* 全局配置入口；
* 不承载具体业务逻辑。

#### `nz-common`

通用基础模块。

职责：

* 通用返回结构；
* 通用异常；
* 通用工具类；
* 通用常量；
* 基础分页模型；
* 基础校验能力；
* 与具体业务无关的公共代码。

约束：

* 不允许依赖业务模块；
* 不允许包含系统管理领域代码；
* 避免变成无边界的工具垃圾桶。

#### `nz-framework`

框架集成模块。

职责：

* Spring 配置；
* Sa-Token 配置；
* MyBatis-Plus 配置；
* Redis 配置；
* Web MVC 配置；
* 安全上下文；
* 审计、日志等基础设施集成。

约束：

* 负责“框架接入”，不负责具体业务流程。

#### `nz-module-system`

系统管理模块。

包含：

* 用户；
* 角色；
* 菜单；
* 权限；
* 部门；
* 字典；
* 参数配置；
* 系统日志等。

#### `nz-module-example`

示例业务模块。

职责：

* 作为新增业务模块的标准样例；
* 展示后端 CRUD、权限、校验、测试、前端页面联动的推荐写法；
* 不应只是演示代码，而应作为脚手架开发范式的参考实现。

---

## 6. 后端业务模块内部结构

以后端 `user` 模块为例，推荐结构：

```text
nz-module-system/
  src/main/java/.../system/user/
    UserController.java
    UserService.java
    UserRepository.java
    UserMapper.java
    UserEntity.java
    UserConvert.java

    dto/
      UserCreateRequest.java
      UserUpdateRequest.java
      UserQueryRequest.java

    vo/
      UserPageVO.java
      UserDetailVO.java
```

也可以根据当前项目习惯保留现有包名，但原则是：

1. 同一业务域相关代码尽量放在同一个业务目录下；
2. 不采用全局横向大目录，例如：

   * `controller/user`
   * `service/user`
   * `mapper/user`
3. Controller 不写业务逻辑；
4. Entity 不直接返回给前端；
5. DTO/VO 与 Entity 分离；
6. 查询对象、创建对象、更新对象分离；
7. 转换逻辑集中管理，优先使用已有转换方案。

---

## 7. 后端测试结构

### 7.1 核心原则

后端测试结构应与生产代码保持一致。

不单独创建：

```text
unit/
integration/
architecture/
```

这样的顶层分类目录。

通过测试类命名区分测试类型。

### 7.2 目标结构示例

生产代码：

```text
src/main/java/.../system/user/
  UserService.java
  UserController.java
  UserRepository.java
  UserMapper.java
```

测试代码：

```text
src/test/java/.../system/user/
  UserServiceTest.java
  UserControllerIT.java
  UserRepositoryIT.java
  UserMapperIT.java
```

### 7.3 命名规则

#### 单元测试

使用：

```text
*Test.java
```

例如：

```text
UserServiceTest.java
PasswordPolicyTest.java
PermissionMatcherTest.java
```

#### 集成测试

使用：

```text
*IT.java
```

例如：

```text
UserControllerIT.java
UserRepositoryIT.java
UserMapperIT.java
LoginIT.java
```

### 7.4 Maven 执行规则建议

后续可配置：

```text
mvn test
```

执行：

```text
*Test.java
```

```text
mvn verify
```

执行：

```text
*IT.java
```

可以使用：

* Maven Surefire Plugin：运行 `*Test.java`
* Maven Failsafe Plugin：运行 `*IT.java`

### 7.5 测试拆分类原则

当单个测试类过大时，不要把所有用例堆进一个 `UserServiceTest`。

可以拆成：

```text
UserCreateServiceTest.java
UserUpdateServiceTest.java
UserDeleteServiceTest.java
UserPermissionServiceTest.java
```

或：

```text
UserServiceCreateTest.java
UserServiceUpdateTest.java
UserServicePermissionTest.java
```

具体采用哪种命名，以项目统一风格为准。

---

## 8. 前端结构设计

### 8.1 目标结构

```text
nz-web/
  src/
    api/
    assets/
    components/
      common/
      business/
    directives/
    hooks/
    layout/
    router/
    stores/
    styles/
    utils/
    views/

  tests/
    unit/
    e2e/
```

### 8.2 目录职责

#### `src/api`

后端接口边界。

建议按业务域组织：

```text
api/
  request.ts
  types.ts
  system/
    user.ts
    role.ts
    menu.ts
    dict.ts
```

规则：

* 只放接口函数和接口相关类型；
* 不写页面状态逻辑；
* 不写复杂 UI 转换逻辑；
* API 模块可被页面、业务组件、store 复用。

#### `src/views`

路由页面目录。

按业务域和菜单结构组织：

```text
views/
  login/
    index.vue

  dashboard/
    index.vue

  system/
    user/
      index.vue
      hooks.ts
      types.ts
      components/
        UserSearchForm.vue
        UserFormDialog.vue

    role/
      index.vue
      hooks.ts
      types.ts
      components/

    menu/
      index.vue
      hooks.ts
      types.ts
      components/
```

页面模块标准结构：

```text
index.vue       页面入口，只负责布局组织和事件绑定
hooks.ts        页面状态、查询、提交、弹窗控制
types.ts        当前页面私有类型
components/     当前页面私有组件
```

规则：

* `index.vue` 不应堆积过多业务逻辑；
* 页面私有组件放页面目录下；
* 多页面复用组件再提升到 `src/components/business`；
* 跨页面复用 hooks 再提升到 `src/hooks`。

#### `src/components/common`

纯通用组件。

示例：

```text
components/common/
  NzTable/
  NzDialog/
  NzForm/
```

要求：

* 不依赖具体业务；
* 不直接调用业务 API；
* 不依赖系统管理模块语义。

#### `src/components/business`

业务通用组件。

示例：

```text
components/business/
  UserSelect/
  RoleSelect/
  DeptTreeSelect/
```

允许：

* 具备业务语义；
* 调用业务 API；
* 被多个页面复用。

#### `src/hooks`

跨页面复用 hooks。

示例：

```text
hooks/
  useTable.ts
  useDialog.ts
  usePermission.ts
```

规则：

* 单页面私有逻辑不放这里；
* 只有两个及以上页面复用时再提升。

#### `src/stores`

全局状态。

示例：

```text
stores/
  app.ts
  user.ts
  permission.ts
  tabs.ts
```

规则：

* 不把页面临时状态放入全局 store；
* store 只承载真正跨页面共享的状态。

#### `src/router`

路由和守卫。

示例：

```text
router/
  index.ts
  guards.ts
  routes.ts
```

#### `src/utils`

纯工具函数。

示例：

```text
utils/
  auth.ts
  storage.ts
  tree.ts
```

规则：

* 不依赖 Vue 组件；
* 不直接调用业务 API；
* 应适合单元测试。

---

## 9. 前端测试结构

### 9.1 核心原则

前端测试文件单独放在 `tests/` 下，不和源码文件混在一起。

同时，`tests/unit` 内部目录结构应镜像 `src`。

### 9.2 目标结构

```text
nz-web/
  src/
    api/
    components/
    hooks/
    router/
    stores/
    utils/
    views/

  tests/
    unit/
      api/
      components/
        common/
        business/
      hooks/
      router/
      stores/
      utils/
      views/
        system/
          user/
            hooks.test.ts
            UserFormDialog.test.ts

    e2e/
      login.spec.ts
      permission.spec.ts
      system/
        user.spec.ts
        role.spec.ts
        menu.spec.ts
```

### 9.3 示例映射

生产代码：

```text
src/utils/tree.ts
```

测试代码：

```text
tests/unit/utils/tree.test.ts
```

生产代码：

```text
src/views/system/user/hooks.ts
```

测试代码：

```text
tests/unit/views/system/user/hooks.test.ts
```

生产代码：

```text
src/views/system/user/components/UserFormDialog.vue
```

测试代码：

```text
tests/unit/views/system/user/UserFormDialog.test.ts
```

E2E 测试：

```text
tests/e2e/system/user.spec.ts
```

### 9.4 命名规则

#### Vitest 单元测试 / 组件测试

```text
*.test.ts
```

#### Playwright E2E 测试

```text
*.spec.ts
```

### 9.5 测试边界

#### 单元测试重点覆盖

* `utils` 工具函数；
* `hooks` 业务逻辑；
* `stores` 状态逻辑；
* 页面私有 hooks；
* 关键业务组件。

#### E2E 测试重点覆盖

* 登录；
* 路由权限；
* 菜单加载；
* 用户管理 CRUD；
* 角色权限配置；
* 典型系统管理流程。

---

## 10. AGENTS.md 基础内容要求

本阶段需要新增或整理 `AGENTS.md`。

建议内容：

````md
# AGENTS.md

## Project Overview

nz-admin is a modern full-stack admin system scaffold designed for maintainable business applications.

## Tech Stack

- Backend: Java, Spring Boot, MyBatis-Plus, Sa-Token, PostgreSQL
- Frontend: Vue 3, TypeScript, Vite, Element Plus, Pinia
- Testing: JUnit 5, Vitest, Playwright

## Repository Structure

- `nz-server`: backend Maven multi-module project
- `nz-web`: frontend Vue application
- `docs`: project documentation
- `.ai`: AI-friendly project knowledge and task skills
- `deploy`: deployment resources
- `scripts`: helper scripts

## Development Rules

- Do not put business logic in controllers.
- Do not expose entity objects directly to frontend.
- Use DTO/VO objects for API boundaries.
- Keep tests structurally aligned with source code.
- Add or update tests when changing business logic.
- Keep module boundaries clear.
- Prefer explicit code over excessive abstraction.

## Common Commands

### Backend

```bash
cd nz-server
mvn test
mvn clean package
````

### Frontend

```bash
cd nz-web
pnpm install
pnpm test
pnpm build
```

## Before Completing a Task

* Run relevant tests.
* Check formatting.
* Update docs if behavior or public APIs changed.
* Do not introduce unnecessary abstractions.

````

---

## 11. `.ai/` 目录要求

本阶段至少建立以下文件：

```text
.ai/
  project-brief.md
  architecture.md
  backend-guidelines.md
  frontend-guidelines.md
  testing-guidelines.md
  skills/
    add-backend-crud-module.md
    add-frontend-crud-page.md
    add-test-case.md
  decisions/
    0001-project-structure.md
    0002-testing-structure.md
````

### 11.1 `project-brief.md`

说明项目定位：

* 不是简单复制 RuoYi；
* 面向现代化管理系统开发；
* 强调代码质量、测试、可维护性、工程规范；
* v0 不追求功能最多，而追求新增业务模块体验更好。

### 11.2 `architecture.md`

说明：

* 仓库结构；
* 后端模块结构；
* 前端模块结构；
* 模块边界；
* 不采用复杂 DDD 的原因。

### 11.3 `backend-guidelines.md`

说明：

* Controller 规则；
* Service 规则；
* Repository/Mapper 规则；
* DTO/VO/Entity 规则；
* 异常、返回、分页、校验规范。

### 11.4 `frontend-guidelines.md`

说明：

* 页面模块结构；
* API 结构；
* 组件分类；
* hooks 使用规则；
* store 使用规则；
* 测试放置规则。

### 11.5 `testing-guidelines.md`

说明：

* 后端 `*Test.java` / `*IT.java` 规则；
* 前端 `tests/unit` / `tests/e2e` 规则；
* 测试目录镜像生产代码；
* 新增业务模块应补哪些测试。

### 11.6 `skills/`

用于指导 coding agent 执行具体任务。

示例：

```text
add-backend-crud-module.md
add-frontend-crud-page.md
add-test-case.md
```

---

## 12. 执行步骤建议

### 阶段 1：整理顶层目录

1. 新增 `nz-server/`。
2. 将后端 Maven 多模块移动到 `nz-server/` 下。
3. 确认 `nz-server/pom.xml` 能正常管理所有后端模块。
4. 调整 README、CI、脚本中涉及后端路径的命令。

### 阶段 2：整理后端模块结构

1. 保留现有核心模块。
2. 确认模块职责：

   * `nz-app`
   * `nz-common`
   * `nz-framework`
   * `nz-module-system`
   * `nz-module-example`
3. 如果当前已有 demo/example 代码，归入 `nz-module-example`。
4. 不做大规模业务逻辑重写，只调整明显不合理的目录和命名。

### 阶段 3：整理后端测试结构

1. 测试目录镜像生产代码目录。
2. 单元测试使用 `*Test.java`。
3. 集成测试使用 `*IT.java`。
4. 检查 Maven 测试插件配置。
5. 至少保留或新增一个典型样例：

   * Service 单元测试；
   * Controller 集成测试；
   * Mapper/Repository 集成测试。

### 阶段 4：整理前端结构

1. 保持 `src/views` 作为页面入口。
2. 页面模块尽量统一为：

   * `index.vue`
   * `hooks.ts`
   * `types.ts`
   * `components/`
3. 将公共组件区分为：

   * `components/common`
   * `components/business`
4. 确认 API 仍统一放在 `src/api`。
5. 不引入过度复杂的 `features/entities/widgets/pages` 结构。

### 阶段 5：整理前端测试结构

1. 测试统一放在 `nz-web/tests`。
2. 建立：

   * `tests/unit`
   * `tests/e2e`
3. `tests/unit` 镜像 `src`。
4. E2E 测试按业务流程组织。
5. 调整 Vitest / Playwright 配置，确保能识别新目录。

### 阶段 6：补充 AI 友好文件

1. 新增 `AGENTS.md`。
2. 新增 `.ai/` 基础结构。
3. 补充项目定位、结构说明、测试规范、开发规则。
4. 确保这些文件简洁、明确，适合 coding agent 快速读取。

---

## 13. 验收标准

### 13.1 仓库结构验收

满足：

```text
nz-admin/
  AGENTS.md
  docs/
  .ai/
  deploy/
  scripts/
  nz-server/
  nz-web/
```

### 13.2 后端验收

1. 后端模块统一位于 `nz-server/` 下；
2. `cd nz-server && mvn test` 可执行；
3. `cd nz-server && mvn clean package` 可执行；
4. 后端测试结构与生产代码结构对应；
5. 单元测试和集成测试命名规则清晰；
6. README 或文档已更新后端运行命令。

### 13.3 前端验收

1. `src/views` 按业务域组织；
2. 页面模块结构基本统一；
3. `components/common` 与 `components/business` 边界清楚；
4. `tests/unit` 镜像 `src`；
5. `tests/e2e` 独立存放流程测试；
6. `cd nz-web && pnpm test` 可执行；
7. `cd nz-web && pnpm build` 可执行。

### 13.4 AI 文件验收

1. 存在 `AGENTS.md`；
2. 存在 `.ai/project-brief.md`；
3. 存在 `.ai/architecture.md`；
4. 存在 `.ai/backend-guidelines.md`；
5. 存在 `.ai/frontend-guidelines.md`；
6. 存在 `.ai/testing-guidelines.md`；
7. 存在至少 3 个 `.ai/skills/*.md`；
8. 文档内容与实际目录结构一致。

---

## 14. 注意事项

1. 本次任务以结构整理为主，不要顺手做大量功能重构。
2. 移动目录后必须同步修改：

   * Maven 配置；
   * CI 配置；
   * Docker 配置；
   * README 命令；
   * 脚本路径；
   * IDE 配置说明。
3. 如果遇到现有结构与本 PRD 冲突，优先保持项目可运行，再逐步调整。
4. 不要为了形式统一破坏当前可工作的业务逻辑。
5. 每次较大移动后都应运行测试或至少编译验证。
6. 对外暴露的包名、接口路径、前端路由不应因目录调整随意变更。
7. 调整完成后，应提交一份结构变更说明，说明移动了哪些目录、修改了哪些配置、测试是否通过。

---

## 15. 推荐最终状态总结

目标状态不是复杂，而是稳定、清晰、可复制：

```text
后端：
- nz-server 管理全部后端模块
- Maven 多模块边界清楚
- 业务模块按业务域组织
- 测试目录镜像源码
- 通过 Test / IT 命名区分测试类型

前端：
- nz-web 独立
- src/views 按页面和业务域组织
- src/api 作为接口边界
- components 区分 common 与 business
- tests/unit 镜像 src
- tests/e2e 独立放流程测试

AI 友好：
- AGENTS.md 作为 agent 入口
- .ai/ 保存项目知识、规范和任务 skills
```
