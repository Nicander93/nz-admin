# Convert（对象转换）规范

## 背景
后端建议把持久化对象（DO）与接口出入参（VO/DTO）分开，避免直接返回 DO 导致字段泄漏（例如 `password`）或接口层到处写拼装代码。

## 目录与命名
- 每个业务模块维护自己的转换器包：`com.nz.admin.modules.<module>.convert`
  - 例：`com.nz.admin.modules.system.convert`
- 转换器命名：`XxxConvert`
- VO 命名建议：`XxxRespVO`（出参）、`XxxCreateReqVO` / `XxxUpdateReqVO`（入参）

## MapStruct 使用方式（芋道风格）
- 使用 `XxxConvert INSTANCE = Mappers.getMapper(XxxConvert.class);`
- 方法命名建议：
  - `convert(XxxDO)`
  - `convertList(List<XxxDO>)`
  - `convertPage(Page<XxxDO>)`（仅转换 records，并保留分页信息）

## 约束
- 当出现 DO/VO/DTO 转换需求时，优先新增 Convert，避免散落在 controller/service 里手写 `new` + `set` 或使用 `BeanUtil.copyProperties`。

