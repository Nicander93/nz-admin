# 登录、可信身份与会话

登录使用租户编码选择账号所在租户；后端验证后把用户与租户写入服务端会话。后续请求通过 Authorization 令牌恢复身份，而不是接受客户端随意指定 tenantId。

## 请求中的令牌

前端 request 封装从本地存储读取 token，并直接放入 `Authorization`。当前配置的 token-name 就是 Authorization；不要未经协议核对额外拼接 Bearer 前缀。

账号密码、短信与第三方登录都应遵守客户端授权类型和启用状态。第三方 provider 需要独立凭据与回调地址，不能因为配置示例存在就认为已经在真实账号下验证。

## 在业务服务读取身份

注入 `LoginUserContext`，使用 `getLoginUserOrNull()` 获取轻量身份。关键操作在为空时拒绝，并核对可信租户。不要接受请求中的 userId 作为当前操作人，也不自己反射查找 Spring Bean。

个人中心固定当前用户；管理其他用户使用独立授权接口。异步业务处理应传入明确身份或可信事件，不能假定仍有 HTTP 请求上下文。

## 接口放行

`nz.auth.include-paths` 默认保护 `/api/**`。默认例外是登录、短信/社交认证和官方设计器静态资源路径。静态设计器资源放行不包含设计器配置、保存和办理人查询，这些仍要求 design 权限。

新增公开入口只放行最小路径，并补匿名与已登录测试。不把整个业务目录加入排除列表，也不把“登录后页面能打开”当成所有 API 都已授权。

## 退出与失效

退出清理前端令牌与 Warm-Authorization，并撤销服务端会话和实时连接。集群节点通过 Redis 共享会话与撤销状态，必须配置相同数据库和前缀。

无效令牌应得到未登录拒绝；Redis 共享存储真正不可用时采取拒绝继续处理的行为，不能回落默认租户。两类问题应分别排查。

参考：[LoginUserContext](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-auth/src/main/java/com/nz/admin/framework/auth/core/LoginUserContext.java)、[认证路径配置](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-auth/src/main/java/com/nz/admin/framework/auth/properties/AuthFrameworkProperties.java)。
