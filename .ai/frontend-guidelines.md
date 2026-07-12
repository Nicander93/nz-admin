# 前端规范

src/api 只放请求和类型；页面在 src/views/<domain>/<feature>，入口 index.vue，状态在 hooks.ts。私有组件不提前全局化；Pinia 只放跨页状态。CRUD 工具位于 src/utils/crud.ts，不依赖仓库外 link 包。

