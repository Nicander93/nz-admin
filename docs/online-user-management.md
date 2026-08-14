# 在线用户管理

在线用户页面用于查看当前有效登录会话，并在权限和租户边界内强制退出指定会话。

## 接口与权限

查询接口：

    GET /api/system/online?username=&loginIp=

需要 `system:online:list` 权限。`username` 和 `loginIp` 都是可选的模糊匹配条件。

强制退出接口：

    DELETE /api/system/online/{tokenValue}

需要 `system:online:force` 权限。后端会再次检查会话所属租户，不能依靠前端隐藏按钮实现隔离。

## 会话数据

登录成功后，服务端 Token Session 会保存：

- 租户 ID 和租户编码；
- 用户名和部门名称；
- 登录 IP、登录时间和 User-Agent。

页面还会显示 Sa-Token 返回的剩余有效时间。升级前已经存在的会话没有这些元数据，需要重新登录后才会完整显示。

## 租户规则

- 默认租户可以查看和退出全部租户的在线会话。
- 普通租户只能查看和退出本租户的在线会话。
- 缺少租户元数据的旧会话不会暴露给普通租户。
- 强制退出不存在或无权访问的会话时，接口返回业务错误。

## 数据库升级

新库由 Flyway 自动执行 `V12__online_user_management.sql`。已有部署可以执行：

    nz-server/nz-app/src/main/resources/db/upgrade-p12-online-user-management.sql

迁移会补齐在线用户页面组件路径，将旧的删除权限收敛为强制退出权限，并移除无实际用途的查询按钮。

## 验证

后端：

    cd nz-server
    JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./mvnw -pl nz-module/nz-system,nz-app -am test -DskipITs
