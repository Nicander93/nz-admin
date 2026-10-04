# 系统管理操作指南

系统管理配置三条关系：用户属于哪个组织、角色能调用什么功能、这些功能能操作哪些数据。先完成组织和角色，再给用户授权，排错更容易。

## 操作路线

| 你要做什么 | 页面 |
| --- | --- |
| 建部门、岗位、用户和角色 | [用户与角色](/platform/system/users-roles) |
| 增加业务菜单、配置按钮 | [菜单与按钮](/platform/system/menus) |
| 给页面配置选项或业务参数 | [字典与参数](/platform/system/dictionaries) |
| 执行定时任务、检查操作轨迹 | [任务与日志](/platform/system/jobs-logs) |
| 新建租户和套餐 | [租户隔离](/platform/security/tenant-isolation) |

## 用普通用户验收

管理员账号能看到页面，不代表普通角色的配置正确。测试用户应有明确部门、角色菜单和数据范围；用独立会话查看菜单、按钮、列表、详情以及更新拒绝。

个人中心不接受任意用户 ID 替代当前身份，头像文件也有归属保护。个人信息修改见[个人中心](/user-profile)；会话管理和强制退出见[在线用户](/online-user-management)。

通知公告与站内消息职责不同：公告表达公共通知，消息中心保存逐用户接收与已读状态。业务提醒需要保留历史时，使用[站内消息](/message-center)，在线实时刷新另接[SSE / WebSocket](/realtime-communication)。
