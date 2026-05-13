# Agent 工作方式

## 后端模块说明

修改或新增 `nz-server` 后端 Maven 模块时，同步维护模块说明：

- 在对应 `pom.xml` 的 `<name>` 附近补 `<description>`，用一句话说明 Maven 模块定位、核心职责和聚合范围。
- 在有 Java 源码的模块根包补 `package-info.java`，说明模块职责、模块内领域或职责划分、依赖边界，以及不应放入该模块的内容。
- 聚合 POM 不强行创建 Java 包；没有源码根包时只写 POM 描述。
- `nz-framework/nz-starter-*` 的说明聚焦框架能力、自动配置、注解、切面和扩展点，不绑定业务表或业务流程。
- `nz-module/*` 的说明聚焦业务域划分；同一业务域应在 `controller/service/mapper/entity/convert` 等技术层使用一致名称。
- 描述文字优先简短、稳定、面向维护者，避免把临时实现细节写进模块说明。

编辑时继续遵守仓库约定：保留原编码，新文件优先 UTF-8，尽量局部修改，不整文件重写。
