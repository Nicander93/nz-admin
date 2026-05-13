# P2 脚手架打磨阶段说明

本阶段目标见 [ruoyi-alignment-plan.md](exec-plans/ruoyi-alignment-plan.md) 中 **P2：脚手架差异化打磨**。当前轮次**不包含代码生成器**，该能力单独延后。

## 子阶段与文档

| 子阶段 | 说明 | 主要文档 |
|--------|------|----------|
| P2.0 | 规范与仓库事实对齐（Java 版本、实体分包、执行计划勾选） | 本仓库 `.docs/architecture.md`、`.docs/naming-convention-service-dao-and-model.md` |
| P2.1 | 前后端 CRUD 范式与代表页面 | [crud-paradigm.md](crud-paradigm.md)、[frontend-coding-conventions.md](frontend-coding-conventions.md)、[.docs/api-convention.md](../.docs/api-convention.md) |
| P2.2 | 新增业务模块的操作步骤与验收 | [module-development-guide.md](module-development-guide.md) |
| P2.3 | 轻量工作台 | 页面 `nz-web/src/views/workbench/` |
| P2.4 | 基础运行监控 | 页面 `nz-web/src/views/system/monitor/`、接口 `/api/system/monitor/summary` |
| P2.5 | 测试与 CI | [common-framework-test-plan.md](exec-plans/common-framework-test-plan.md)、`.github/workflows/ci.yml` |

## 推荐阅读顺序

1. [module-development-guide.md](module-development-guide.md) 总流程  
2. [crud-paradigm.md](crud-paradigm.md) 页面与接口细节  
3. [.docs/nz-web-ui-design-guide.md](../.docs/nz-web-ui-design-guide.md) 工作台与监控页的 UI 基线  
