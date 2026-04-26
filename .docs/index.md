# docs 索引

`docs/` 是本仓库面向人类与智能体的记录系统，目标是让任务上下文尽量留在仓库里，而不是散落在聊天、口头说明或临时提示中。

这套组织方式参考了 OpenAI 在 2026 年 2 月 11 日发布的《[工程技术：在智能体优先的世界中利用 Codex](https://openai.com/zh-Hans-CN/index/harness-engineering/)》：`AGENTS.md` 保持简短，充当目录；更完整的知识沉淀进结构化的 `docs/` 中。

## 当前文档

- `docs/index.md`：文档入口与使用约定
- `docs/architecture.md`：monorepo 结构、包职责、依赖关系、变更落点
- `docs/api-convention.md`：API 接口命名规范（URL、Method、前后端类型对应）
- `docs/nz-web-ui-design-guide.md`：`nz-web` 的 UI 风格、主题变量与组件设计约定
- `docs/harness-engineering.md`：结合本仓库的 harness 实践建议
- `docs/exec-plans/README.md`：执行计划的存放规则

## 使用方式

- 进入仓库先读 `AGENTS.md`，再按需跳转到具体文档。
- 处理具体包时，同时阅读对应包内 README 与源码目录。
- 文档只写稳定信息；临时性问题优先进入执行计划，而不是堆在入口文件。

## 何时更新文档

- 包职责或依赖关系变化时，更新 `docs/architecture.md`
- agent 工作方式、校验流程、文档策略变化时，更新 `docs/harness-engineering.md`
- 中大型任务开始实施时，在 `docs/exec-plans/` 新增计划
- 对外使用方式变化时，更新根 `README.md` 或包内 `README.md`

## 文档维护原则

- 优先写“去哪看”，再写“怎么做”
- 优先写稳定规则，不堆叠一次性提示
- 文档内容应能被代码、脚本或目录结构验证
- 文档过期时直接修正，不保留模糊描述
