# AGENTS.md

本文件是智能体在 `nz-admin` 仓库中的入口地图，不承载全部知识。

## 工作原则

- 先读相关文档，再改代码；不要把 `AGENTS.md` 当百科全书。
- 优先做最小改动，保持与现有风格一致。
- 发现规则缺失时，优先补充 `docs/`，而不是把大量临时约束写进提示词。

## Codex App 使用约定

- 仓库位于 WSL 时，回复中的可点击文件链接优先使用 Windows UNC 绝对路径，例如 `\\wsl.localhost\Ubuntu\home\pc\my-code-repo\nz-admin\...`，避免 Codex App 使用 `/home/...` 路径时无法加载文件。
- 在 Windows PowerShell + WSL UNC 工作目录下，如果普通沙箱命令报 `windows sandbox: setup refresh failed`，不要对每个读取命令重复试错；确认一次后，后续同类读文件、`rg`、`Get-Content`、`Get-ChildItem` 等仓库内只读操作直接沿用已验证的可执行方式，并合并并行读取，减少重复提示和噪音。

## 仓库结构

- `nz-server` 后端（Maven 多模块项目，使用 Maven Wrapper）
- `nz-web` 前端（Vue3 + TypeScript + Vite）

## 工具库使用规范

### 后端工具库

#### Hutool（优先使用）
- **字符串处理**：`StrUtil.isBlank(str)`、`StrUtil.isNotBlank(str)`、`StrUtil.subPre(str, len)`
- **JSON 处理**：`JSONUtil.toJsonStr(obj)`、`JSONUtil.parseObj(json)`
- **反射工具**：`ReflectUtil.invoke(obj, "methodName", args...)`（避免自己写反射）
- **Bean 工具**：`BeanUtil.copyProperties(source, target)`
- **其他**：`DateUtil`、`RandomUtil`、`CollUtil`、`MapUtil`

#### Spring 框架
- **依赖注入**：使用 `@Autowired` 或构造器注入，**禁止自己反射调用 `getBean()`**
- **请求上下文**：使用 `RequestContextHolder.getRequestAttributes()` 获取 `HttpServletRequest`
- **异常处理**：捕获具体异常（如 `BusinessException`），**禁止直接 catch(Throwable)**
- **AOP 切面**：使用 `@Aspect` + `@Component`，通过 `@Autowired` 注入服务

#### 其他后端工具
- **Sa-Token**：权限校验用 `StpUtil.checkPermission()`、角色校验用 `StpUtil.checkRole()`
- **MyBatis-Plus**：使用 `ServiceImpl<Mapper, Entity>` 实现 Service，避免继承 `IService`
- **Lombok**：使用 `@Data`、`@Slf4j` 等注解减少样板代码

### 前端工具库

#### Element Plus
- **按需引入**：`import { ElMessage } from 'element-plus'`
- **递归组件**：侧边栏菜单使用递归渲染（参考 `SidebarMenuItem.vue`）

#### Vue 3 + TypeScript
- **Hooks 模式**：业务逻辑封装在 `hooks.ts` 中（参考 `views/system/*/hooks.ts`）
- **Store 模式**：使用 Pinia（`@/stores/`）
- **路由守卫**：在 `router/index.ts` 中实现动态路由加载

## 构建工具

### 后端（Maven Wrapper）
```bash
cd nz-server
# 使用 Maven Wrapper（推荐）
./mvnw clean compile -DskipTests
./mvnw install -DskipTests

# 指定 JDK 版本（如需）
export JAVA_HOME=~/.jdks/jdk-17.0.2
export PATH=$JAVA_HOME/bin:$PATH
./mvnw compile
```

### 前端（pnpm）
```bash
cd nz-web
pnpm install
pnpm test        # 运行测试
pnpm dev          # 开发模式
pnpm build       # 生产构建
```

## 文档更新约定

- 架构调整：更新 `docs/architecture.md`
- 工作方式或 agent 约束调整：更新 `docs/harness-engineering.md`
- 较大任务：在 `docs/exec-plans/` 记录计划与结论

## 代码基本原则

- 注释保持克制，类级别、方法、方法参数、关键逻辑应添加注释，注释应保持口语化；
- 代码应优先保证简洁性、可读性、可维护性；
- **公共类代码应尽量复用引入的工具库**（Hutool、Spring 等），避免重复造轮子；
- 后端框架级组件放在 `nz-framework/nz-starter-*` 模块，业务代码放在 `nz-module/nz-system`；
- 前端测试目录与源码目录对应（`test/unit/` 对应 `src/`）。

## 常见问题

### 编译问题
- **JDK 缺失**：下载 JDK 到 `~/.jdks/` 并配置 `JAVA_HOME`
- **Maven 依赖下载慢**：已配置阿里云镜像（`pom.xml` 中）
- **模块依赖找不到**：先运行 `./mvnw install -DskipTests` 安装父 POM 和公共模块

### 架构原则
- **框架模块不依赖业务模块**：`nz-starter-*` 不应依赖 `nz-system`
- **业务模块可依赖框架模块**：`nz-system` 可依赖 `nz-starter-auth`、`nz-starter-log` 等
