# 常见问题

## 数据库连接失败

核对配置中实际主机、端口、数据库名称和账号。Docker 容器的 `5432` 是容器端口，宿主机连接应使用发布端口；WSL 与 Windows 的 localhost 访问路径也需按当前环境确认。不要为了修复连接问题删除已有数据卷。

## Maven 找不到模块依赖

先在 `nz-server` 执行 `./mvnw -pl nz-app -am install -DskipTests`，安装应用依赖的公共和业务模块。确认 `JAVA_HOME` 使用 JDK 17。

## 页面或按钮没有显示

依次确认模块启用、租户套餐包含菜单、角色已分配菜单及按钮、菜单组件路径与前端模块清单匹配。菜单可见不等于数据对象可访问；后端仍检查租户、数据范围和业务归属。参考[模块指南](module-development-guide.md)与[多租户](multi-tenancy.md)。

## 流程中心提示新引擎未启用

先升级至 V30，再设置 `NZ_WARM_FLOW_ENABLED=true` 并重启后端。有 `workflow:engine:design` 权限的用户才看到定义管理。请假选择列表只显示用途为“请假申请”的已发布流程。

## 退回后为什么产生新的审批实例

没有退回连线时，当前引擎不能跳回开始节点，因此结束本次审批并把单据交还业务修改。重新提交产生新实例，原审批轨迹继续保存。[工作流指南](workflow.md)说明指定节点退回与业务退回的区别。

## 集群节点的状态不一致

核对所有节点使用相同 PostgreSQL、Redis 数据库和 `NZ_CACHE_KEY_PREFIX`，并启用 `NZ_CLUSTER_ENABLED=true` 与 Redis。实时连接统计仍是节点级值。诊断和双节点验收见[部署指南](deployment.md)与[测试指南](testing.md)。

## 文档站子路径部署出现资源 404

构建时设置 `DOCS_BASE=/nz-admin/` 等实际前缀，服务器从相同路径提供构建产物；需要重新构建。静态站采用 clean URL，代理应尝试 `.html` 文件。见[文档站维护](documentation-site.md)。
