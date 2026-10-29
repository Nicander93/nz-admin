<!-- 由 pnpm catalog:update 生成；编辑 scripts/catalog.config.mjs 或对应源码。 -->
# 源码与模块索引

本页从仓库 POM、清单、自动配置和公开接口生成，可被站内搜索检索。它记录源码声明，不代表模块已在某个进程启用，也不替代行为测试。

使用[开发者能力目录](/developer/catalog)按关键词、架构层和扩展点筛选。维护方法见[源码与文档同步](/developer/documentation-contract)。

## 聚合工程

### nz-common

公共协议聚合。公共能力聚合模块，收拢基础对象、轻量协议和跨模块任务契约。

- 指南：[公共协议聚合](/developer/architecture)
- POM：[nz-server/nz-common/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/pom.xml)
- 显式生产依赖：没有仓库内部生产依赖

### nz-framework

框架聚合。框架能力聚合模块，组织可复用 starter、自动配置、注解和扩展点。

- 指南：[框架聚合](/developer/architecture)
- POM：[nz-server/nz-framework/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/pom.xml)
- 显式生产依赖：没有仓库内部生产依赖

### nz-module

业务聚合。业务模块聚合模块，收拢按业务域沉淀的后端业务实现。

- 指南：[业务聚合](/developer/module-lifecycle)
- POM：[nz-server/nz-module/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/pom.xml)
- 显式生产依赖：没有仓库内部生产依赖

### nz-server

后端父工程。后端父工程，统一版本、依赖管理、插件配置和模块聚合。

- 指南：[后端父工程](/developer/architecture)
- POM：[nz-server/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/pom.xml)
- 显式生产依赖：没有仓库内部生产依赖

## 应用装配

### nz-app

应用装配。后端启动模块，负责应用入口、运行配置、启动期装配和基础数据初始化。

- 指南：[应用装配](/developer/architecture)
- POM：[nz-server/nz-app/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-app/pom.xml)
- 显式生产依赖：`nz-common-module`、`nz-demo`、`nz-generator`、`nz-job`、`nz-starter-auth`、`nz-starter-cache`、`nz-starter-log`、`nz-starter-mybatis`、`nz-starter-tenant`、`nz-starter-web`、`nz-system`、`nz-workflow`
- 测试入口：[nz-server/nz-app/src/test/java/com/nz/admin/ClusterStateRedisTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-app/src/test/java/com/nz/admin/ClusterStateRedisTest.java)（共 19 个测试源文件，不代表本次执行结果）。

## 公共对象与协议

### nz-common-core

统一响应与基础对象。公共核心模块，提供基础实体、分页查询、统一响应等轻量对象。

- 指南：[统一响应与基础对象](/guide/backend/api-contract)
- POM：[nz-server/nz-common/nz-common-core/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-core/pom.xml)
- 显式生产依赖：没有仓库内部生产依赖
- 公开接口：[ErrorCode](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-core/src/main/java/com/nz/admin/common/core/ErrorCode.java)、[IdentityValidator](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-core/src/main/java/com/nz/admin/common/core/identity/IdentityValidator.java)
- 测试入口：[nz-server/nz-common/nz-common-core/src/test/java/com/nz/admin/common/core/PageQueryTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-core/src/test/java/com/nz/admin/common/core/PageQueryTest.java)（共 3 个测试源文件，不代表本次执行结果）。

### nz-common-job

任务执行协议。公共任务契约模块，定义定时任务执行和任务日志下沉的跨模块协议。

- 指南：[任务执行协议](/platform/system/jobs-logs)
- POM：[nz-server/nz-common/nz-common-job/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-job/pom.xml)
- 显式生产依赖：没有仓库内部生产依赖
- 公开接口：[JobExecutionCoordinator](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-job/src/main/java/com/nz/admin/common/job/JobExecutionCoordinator.java)、[JobInvokeLogSink](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-job/src/main/java/com/nz/admin/common/job/JobInvokeLogSink.java)、[JobTenantProvider](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-job/src/main/java/com/nz/admin/common/job/JobTenantProvider.java)
- 测试入口：[nz-server/nz-common/nz-common-job/src/test/java/com/nz/admin/common/job/JobExecuteServiceTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-job/src/test/java/com/nz/admin/common/job/JobExecuteServiceTest.java)（共 1 个测试源文件，不代表本次执行结果）。

### nz-common-module

模块与业务协作协议。模块描述、注册和依赖校验协议，不依赖 Spring 或持久化框架。

- 指南：[模块与业务协作协议](/developer/extensions)
- POM：[nz-server/nz-common/nz-common-module/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-module/pom.xml)
- 显式生产依赖：没有仓库内部生产依赖
- 公开接口：[NzUserNotificationPublisher](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzUserNotificationPublisher.java)、[NzWorkflowAssigneeResolver](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzWorkflowAssigneeResolver.java)、[NzWorkflowBusinessHandler](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzWorkflowBusinessHandler.java)、[NzWorkflowBusinessLauncher](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzWorkflowBusinessLauncher.java)、[NzWorkflowParticipantProvider](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzWorkflowParticipantProvider.java)
- 测试入口：[nz-server/nz-common/nz-common-module/src/test/java/com/nz/admin/common/module/NzModuleDependencyValidatorTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-module/src/test/java/com/nz/admin/common/module/NzModuleDependencyValidatorTest.java)（共 1 个测试源文件，不代表本次执行结果）。

## 框架 Starter

### nz-starter-auth

认证授权。认证鉴权 starter，提供 Sa-Token 接入、权限注解、鉴权切面和异常处理。

- 指南：[认证授权](/platform/security/authentication)
- POM：[nz-server/nz-framework/nz-starter-auth/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-auth/pom.xml)
- 显式生产依赖：`nz-common-core`、`nz-starter-cache`
- 自动配置：[AuthContextAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-auth/src/main/java/com/nz/admin/framework/auth/config/AuthContextAutoConfiguration.java)、[SaTokenAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-auth/src/main/java/com/nz/admin/framework/auth/config/SaTokenAutoConfiguration.java)、[ClusterAuthAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-auth/src/main/java/com/nz/admin/framework/auth/config/ClusterAuthAutoConfiguration.java)
- 公开接口：[AuthUserResolver](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-auth/src/main/java/com/nz/admin/framework/auth/core/AuthUserResolver.java)、[PermissionResolver](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-auth/src/main/java/com/nz/admin/framework/auth/core/PermissionResolver.java)
- 测试入口：[nz-server/nz-framework/nz-starter-auth/src/test/java/com/nz/admin/framework/auth/config/SaTokenAutoConfigurationTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-auth/src/test/java/com/nz/admin/framework/auth/config/SaTokenAutoConfigurationTest.java)（共 1 个测试源文件，不代表本次执行结果）。

### nz-starter-cache

缓存与原子共享状态。缓存 starter，提供 RedisTemplate、Spring Cache 默认配置和缓存 key 工具。

- 指南：[缓存与原子共享状态](/operations/cluster)
- POM：[nz-server/nz-framework/nz-starter-cache/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-cache/pom.xml)
- 显式生产依赖：`nz-common-core`
- 自动配置：[NzCacheAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-cache/src/main/java/com/nz/admin/framework/cache/config/NzCacheAutoConfiguration.java)、[AtomicStateAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-cache/src/main/java/com/nz/admin/framework/cache/config/AtomicStateAutoConfiguration.java)
- 公开接口：[AtomicStateStore](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-cache/src/main/java/com/nz/admin/framework/cache/core/AtomicStateStore.java)
- 测试入口：[nz-server/nz-framework/nz-starter-cache/src/test/java/com/nz/admin/framework/cache/config/NzCacheAutoConfigurationTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-cache/src/test/java/com/nz/admin/framework/cache/config/NzCacheAutoConfigurationTest.java)（共 2 个测试源文件，不代表本次执行结果）。

### nz-starter-datascope

功能数据范围。数据权限 starter，提供受保护表规则、SQL 行过滤和当前数据范围解析扩展点。

- 指南：[功能数据范围](/platform/security/data-scope)
- POM：[nz-server/nz-framework/nz-starter-datascope/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-datascope/pom.xml)
- 显式生产依赖：`nz-starter-mybatis`
- 自动配置：[NzDatascopeAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-datascope/src/main/java/com/nz/admin/framework/datascope/config/NzDatascopeAutoConfiguration.java)
- 公开接口：[DataScopeResolver](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-datascope/src/main/java/com/nz/admin/framework/datascope/core/DataScopeResolver.java)、[DataScopeRuleCustomizer](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-datascope/src/main/java/com/nz/admin/framework/datascope/core/DataScopeRuleCustomizer.java)
- 测试入口：[nz-server/nz-framework/nz-starter-datascope/src/test/java/com/nz/admin/framework/datascope/DataScopeIntegrationTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-datascope/src/test/java/com/nz/admin/framework/datascope/DataScopeIntegrationTest.java)（共 1 个测试源文件，不代表本次执行结果）。

### nz-starter-encryption

字段加密与脱敏。字段加密与敏感信息脱敏组件

- 指南：[字段加密与脱敏](/field-encryption)
- POM：[nz-server/nz-framework/nz-starter-encryption/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-encryption/pom.xml)
- 显式生产依赖：`nz-starter-mybatis`
- 自动配置：[FieldEncryptionAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-encryption/src/main/java/com/nz/admin/framework/encryption/config/FieldEncryptionAutoConfiguration.java)
- 公开接口：[FieldCipher](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-encryption/src/main/java/com/nz/admin/framework/encryption/core/FieldCipher.java)
- 测试入口：[nz-server/nz-framework/nz-starter-encryption/src/test/java/com/nz/admin/framework/encryption/config/FieldEncryptionAutoConfigurationTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-encryption/src/test/java/com/nz/admin/framework/encryption/config/FieldEncryptionAutoConfigurationTest.java)（共 4 个测试源文件，不代表本次执行结果）。

### nz-starter-excel

Excel 数据交换。Excel starter，提供导出响应、模板下载、导入错误结构和字典转换扩展点。

- 指南：[Excel 数据交换](/guide/backend/excel)
- POM：[nz-server/nz-framework/nz-starter-excel/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-excel/pom.xml)
- 显式生产依赖：没有仓库内部生产依赖
- 自动配置：[NzExcelAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-excel/src/main/java/com/nz/admin/framework/excel/config/NzExcelAutoConfiguration.java)
- 公开接口：[DictValueResolver](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-excel/src/main/java/com/nz/admin/framework/excel/core/DictValueResolver.java)
- 测试入口：[nz-server/nz-framework/nz-starter-excel/src/test/java/com/nz/admin/framework/excel/support/ExcelResponseUtilsTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-excel/src/test/java/com/nz/admin/framework/excel/support/ExcelResponseUtilsTest.java)（共 1 个测试源文件，不代表本次执行结果）。

### nz-starter-file

文件存储。文件存储 starter，提供文件存储抽象、本地存储、OSS 存储和配置绑定。

- 指南：[文件存储](/file-configuration)
- POM：[nz-server/nz-framework/nz-starter-file/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-file/pom.xml)
- 显式生产依赖：`nz-starter-web`
- 自动配置：[NzFileAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-file/src/main/java/com/nz/admin/framework/file/config/NzFileAutoConfiguration.java)
- 公开接口：[FileStorageAdapterFactory](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-file/src/main/java/com/nz/admin/framework/file/FileStorageAdapterFactory.java)、[FileStorageService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-file/src/main/java/com/nz/admin/framework/file/FileStorageService.java)
- 测试入口：[nz-server/nz-framework/nz-starter-file/src/test/java/com/nz/admin/framework/file/FileConfigSecretCodecTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-file/src/test/java/com/nz/admin/framework/file/FileConfigSecretCodecTest.java)（共 6 个测试源文件，不代表本次执行结果）。

### nz-starter-log

操作日志。操作日志 starter，提供日志注解、采集入口、自动配置和日志记录扩展点。

- 指南：[操作日志](/platform/system/jobs-logs)
- POM：[nz-server/nz-framework/nz-starter-log/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-log/pom.xml)
- 显式生产依赖：`nz-common-core`、`nz-starter-auth`
- 自动配置：[NzLogAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-log/src/main/java/com/nz/admin/framework/log/config/NzLogAutoConfiguration.java)
- 公开接口：[OperationLogRecorder](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-log/src/main/java/com/nz/admin/framework/log/core/OperationLogRecorder.java)
- 测试入口：[nz-server/nz-framework/nz-starter-log/src/test/java/com/nz/admin/framework/log/aspect/OperationLogAspectTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-log/src/test/java/com/nz/admin/framework/log/aspect/OperationLogAspectTest.java)（共 1 个测试源文件，不代表本次执行结果）。

### nz-starter-mail

邮件发送。邮件 starter，提供发送协议、SMTP 实现、配置属性与自动装配。

- 指南：[邮件发送](/mail)
- POM：[nz-server/nz-framework/nz-starter-mail/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-mail/pom.xml)
- 显式生产依赖：没有仓库内部生产依赖
- 自动配置：[MailAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-mail/src/main/java/com/nz/admin/framework/mail/config/MailAutoConfiguration.java)
- 公开接口：[MailService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-mail/src/main/java/com/nz/admin/framework/mail/core/MailService.java)
- 测试入口：[nz-server/nz-framework/nz-starter-mail/src/test/java/com/nz/admin/framework/mail/config/MailAutoConfigurationTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-mail/src/test/java/com/nz/admin/framework/mail/config/MailAutoConfigurationTest.java)（共 1 个测试源文件，不代表本次执行结果）。

### nz-starter-monitor

运行状态。监控 starter，提供 Actuator 健康检查接入和轻量运行状态读取能力。

- 指南：[运行状态](/operations/troubleshooting)
- POM：[nz-server/nz-framework/nz-starter-monitor/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-monitor/pom.xml)
- 显式生产依赖：没有仓库内部生产依赖
- 自动配置：[NzMonitorAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-monitor/src/main/java/com/nz/admin/framework/monitor/config/NzMonitorAutoConfiguration.java)
- 公开接口：[RedisMonitorStatusProvider](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-monitor/src/main/java/com/nz/admin/framework/monitor/core/RedisMonitorStatusProvider.java)
- 测试入口：[nz-server/nz-framework/nz-starter-monitor/src/test/java/com/nz/admin/framework/monitor/config/NzMonitorAutoConfigurationTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-monitor/src/test/java/com/nz/admin/framework/monitor/config/NzMonitorAutoConfigurationTest.java)（共 2 个测试源文件，不代表本次执行结果）。

### nz-starter-mybatis

MyBatis 与分页。MyBatis-Plus starter，提供持久层基础依赖和自动配置。

- 指南：[MyBatis 与分页](/guide/backend/persistence)
- POM：[nz-server/nz-framework/nz-starter-mybatis/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-mybatis/pom.xml)
- 显式生产依赖：`nz-common-core`
- 自动配置：[MybatisPlusAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-mybatis/src/main/java/com/nz/admin/framework/mybatis/config/MybatisPlusAutoConfiguration.java)
- 公开接口：[BaseMapperX](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-mybatis/src/main/java/com/nz/admin/framework/mybatis/core/mapper/BaseMapperX.java)、[MybatisPlusInterceptorCustomizer](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-mybatis/src/main/java/com/nz/admin/framework/mybatis/plugin/MybatisPlusInterceptorCustomizer.java)
- 测试入口：[nz-server/nz-framework/nz-starter-mybatis/src/test/java/com/nz/admin/framework/mybatis/config/MybatisPlusAutoConfigurationTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-mybatis/src/test/java/com/nz/admin/framework/mybatis/config/MybatisPlusAutoConfigurationTest.java)（共 2 个测试源文件，不代表本次执行结果）。

### nz-starter-protection

限流、防重与幂等。保护 starter，提供防重复提交、接口限流和输入清理基础能力。

- 指南：[限流、防重与幂等](/guide/backend/protection)
- POM：[nz-server/nz-framework/nz-starter-protection/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-protection/pom.xml)
- 显式生产依赖：`nz-common-core`、`nz-starter-auth`、`nz-starter-cache`
- 自动配置：[NzProtectionAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-protection/src/main/java/com/nz/admin/framework/protection/config/NzProtectionAutoConfiguration.java)、[IdempotencyAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-protection/src/main/java/com/nz/admin/framework/protection/config/IdempotencyAutoConfiguration.java)
- 公开接口：[ProtectionStore](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-protection/src/main/java/com/nz/admin/framework/protection/core/ProtectionStore.java)
- 测试入口：[nz-server/nz-framework/nz-starter-protection/src/test/java/com/nz/admin/framework/protection/aspect/ProtectionAspectTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-protection/src/test/java/com/nz/admin/framework/protection/aspect/ProtectionAspectTest.java)（共 5 个测试源文件，不代表本次执行结果）。

### nz-starter-quartz

Quartz 调度。Quartz 调度 starter，提供调度装配、任务执行封装和调度扩展能力。

- 指南：[Quartz 调度](/platform/system/jobs-logs)
- POM：[nz-server/nz-framework/nz-starter-quartz/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-quartz/pom.xml)
- 显式生产依赖：`nz-common-job`、`nz-starter-tenant`
- 自动配置：[QuartzAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-quartz/src/main/java/com/nz/admin/framework/quartz/config/QuartzAutoConfiguration.java)
- 测试入口：[nz-server/nz-framework/nz-starter-quartz/src/test/java/com/nz/admin/framework/quartz/config/QuartzAutoConfigurationTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-quartz/src/test/java/com/nz/admin/framework/quartz/config/QuartzAutoConfigurationTest.java)（共 3 个测试源文件，不代表本次执行结果）。

### nz-starter-realtime

SSE 与 WebSocket。实时通信 starter，提供一次性连接票据、SSE、WebSocket 和用户定向推送。

- 指南：[SSE 与 WebSocket](/realtime-communication)
- POM：[nz-server/nz-framework/nz-starter-realtime/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-realtime/pom.xml)
- 显式生产依赖：`nz-starter-cache`
- 自动配置：[RealtimeAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-realtime/src/main/java/com/nz/admin/framework/realtime/config/RealtimeAutoConfiguration.java)
- 公开接口：[RealtimeAccessPolicy](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-realtime/src/main/java/com/nz/admin/framework/realtime/core/RealtimeAccessPolicy.java)、[RealtimeConnectionManager](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-realtime/src/main/java/com/nz/admin/framework/realtime/core/RealtimeConnectionManager.java)、[RealtimePublisher](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-realtime/src/main/java/com/nz/admin/framework/realtime/core/RealtimePublisher.java)、[RealtimeTicketService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-realtime/src/main/java/com/nz/admin/framework/realtime/core/RealtimeTicketService.java)
- 测试入口：[nz-server/nz-framework/nz-starter-realtime/src/test/java/com/nz/admin/framework/realtime/config/RealtimeAutoConfigurationTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-realtime/src/test/java/com/nz/admin/framework/realtime/config/RealtimeAutoConfigurationTest.java)（共 4 个测试源文件，不代表本次执行结果）。

### nz-starter-sms

短信发送。短信发送框架，提供渠道扩展协议、日志渠道、Webhook 渠道与自动装配。

- 指南：[短信发送](/sms-management)
- POM：[nz-server/nz-framework/nz-starter-sms/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-sms/pom.xml)
- 显式生产依赖：没有仓库内部生产依赖
- 自动配置：[SmsAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-sms/src/main/java/com/nz/admin/framework/sms/config/SmsAutoConfiguration.java)
- 公开接口：[SmsGateway](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-sms/src/main/java/com/nz/admin/framework/sms/core/SmsGateway.java)、[SmsProvider](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-sms/src/main/java/com/nz/admin/framework/sms/core/SmsProvider.java)
- 测试入口：[nz-server/nz-framework/nz-starter-sms/src/test/java/com/nz/admin/framework/sms/config/SmsAutoConfigurationTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-sms/src/test/java/com/nz/admin/framework/sms/config/SmsAutoConfigurationTest.java)（共 2 个测试源文件，不代表本次执行结果）。

### nz-starter-social

OAuth2 / OIDC。OAuth2/OIDC 社交认证框架，提供 PKCE、一次性状态与身份解析。

- 指南：[OAuth2 / OIDC](/social-login)
- POM：[nz-server/nz-framework/nz-starter-social/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-social/pom.xml)
- 显式生产依赖：`nz-starter-cache`
- 自动配置：[SocialAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-social/src/main/java/com/nz/admin/framework/social/config/SocialAutoConfiguration.java)
- 公开接口：[SocialAuthorizationStateStore](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-social/src/main/java/com/nz/admin/framework/social/core/SocialAuthorizationStateStore.java)、[SocialOAuthService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-social/src/main/java/com/nz/admin/framework/social/core/SocialOAuthService.java)
- 测试入口：[nz-server/nz-framework/nz-starter-social/src/test/java/com/nz/admin/framework/social/config/SocialAutoConfigurationTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-social/src/test/java/com/nz/admin/framework/social/config/SocialAutoConfigurationTest.java)（共 3 个测试源文件，不代表本次执行结果）。

### nz-starter-tenant

可信租户隔离。多租户 starter，提供可信租户上下文、异步传递和 MyBatis 行级隔离。

- 指南：[可信租户隔离](/platform/security/tenant-isolation)
- POM：[nz-server/nz-framework/nz-starter-tenant/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-tenant/pom.xml)
- 显式生产依赖：`nz-starter-auth`、`nz-starter-mybatis`
- 自动配置：[TenantPropertiesAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-tenant/src/main/java/com/nz/admin/framework/tenant/config/TenantPropertiesAutoConfiguration.java)、[TenantAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-tenant/src/main/java/com/nz/admin/framework/tenant/config/TenantAutoConfiguration.java)
- 公开接口：[TenantTableRuleCustomizer](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-tenant/src/main/java/com/nz/admin/framework/tenant/core/TenantTableRuleCustomizer.java)
- 测试入口：[nz-server/nz-framework/nz-starter-tenant/src/test/java/com/nz/admin/framework/tenant/config/TenantAutoConfigurationTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-tenant/src/test/java/com/nz/admin/framework/tenant/config/TenantAutoConfigurationTest.java)（共 2 个测试源文件，不代表本次执行结果）。

### nz-starter-test

后端测试支持。测试支撑 starter，提供单元测试、数据库测试、HTTP 测试客户端和随机对象生成工具。

- 指南：[后端测试支持](/guide/testing/backend)
- POM：[nz-server/nz-framework/nz-starter-test/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-test/pom.xml)
- 显式生产依赖：`nz-starter-auth`、`nz-starter-mybatis`
- 测试入口：[nz-server/nz-framework/nz-starter-test/src/test/java/com/nz/admin/framework/test/core/util/TestContextCleanerTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-test/src/test/java/com/nz/admin/framework/test/core/util/TestContextCleanerTest.java)（共 1 个测试源文件，不代表本次执行结果）。

### nz-starter-web

Web 入口。Web 框架 starter，提供 Web 依赖、接口文档、参数校验和全局异常处理。

- 指南：[Web 入口](/guide/backend/validation)
- POM：[nz-server/nz-framework/nz-starter-web/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-web/pom.xml)
- 显式生产依赖：`nz-common-core`
- 自动配置：[NzWebAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-web/src/main/java/com/nz/admin/framework/web/config/NzWebAutoConfiguration.java)、[SwaggerAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-web/src/main/java/com/nz/admin/framework/web/config/SwaggerAutoConfiguration.java)
- 公开接口：[ApiExceptionLogRecorder](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-web/src/main/java/com/nz/admin/framework/web/exception/ApiExceptionLogRecorder.java)
- 测试入口：[nz-server/nz-framework/nz-starter-web/src/test/java/com/nz/admin/framework/web/config/NzWebAutoConfigurationTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-web/src/test/java/com/nz/admin/framework/web/config/NzWebAutoConfigurationTest.java)（共 2 个测试源文件，不代表本次执行结果）。

## 业务模块

### nz-demo

纵向业务示例。可独立装配的示例模块，提供条目管理与通过公共 SPI 接入的请假审批。

- 指南：[纵向业务示例](/demo-module)
- POM：[nz-server/nz-module/nz-demo/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-demo/pom.xml)
- 显式生产依赖：`nz-common-core`、`nz-common-module`、`nz-starter-auth`、`nz-starter-datascope`、`nz-starter-excel`、`nz-starter-log`、`nz-starter-mybatis`、`nz-starter-protection`、`nz-starter-tenant`、`nz-starter-web`
- 业务清单：[nz-server/nz-module/nz-demo/src/main/resources/META-INF/nz/module.yaml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-demo/src/main/resources/META-INF/nz/module.yaml)；默认启用。
- 前端清单：[nz-web/src/modules/demo/manifest.ts](https://github.com/Nicander93/nz-admin/blob/master/nz-web/src/modules/demo/manifest.ts)；组件前缀 `demo`。
- 自动配置：[NzDemoModuleAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-demo/src/main/java/com/nz/admin/modules/demo/config/NzDemoModuleAutoConfiguration.java)
- 公开接口：[DemoItemMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-demo/src/main/java/com/nz/admin/modules/demo/mapper/DemoItemMapper.java)、[DemoItemService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-demo/src/main/java/com/nz/admin/modules/demo/service/DemoItemService.java)
- 测试入口：[nz-server/nz-module/nz-demo/src/test/java/com/nz/admin/modules/demo/config/NzDemoModuleManifestTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-demo/src/test/java/com/nz/admin/modules/demo/config/NzDemoModuleManifestTest.java)（共 3 个测试源文件，不代表本次执行结果）。

### nz-generator

CRUD 代码生成。数据库元数据驱动的 CRUD 代码生成模块。

- 指南：[CRUD 代码生成](/code-generator)
- POM：[nz-server/nz-module/nz-generator/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-generator/pom.xml)
- 显式生产依赖：`nz-common-core`、`nz-starter-auth`、`nz-starter-datascope`、`nz-starter-protection`、`nz-starter-tenant`、`nz-starter-web`
- 业务清单：[nz-server/nz-module/nz-generator/src/main/resources/META-INF/nz/module.yaml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-generator/src/main/resources/META-INF/nz/module.yaml)；默认启用。
- 前端清单：[nz-web/src/modules/generator/manifest.ts](https://github.com/Nicander93/nz-admin/blob/master/nz-web/src/modules/generator/manifest.ts)；组件前缀 `generator`。
- 自动配置：[NzGeneratorModuleAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-generator/src/main/java/com/nz/admin/modules/generator/config/NzGeneratorModuleAutoConfiguration.java)
- 公开接口：[GeneratorMetadataRepository](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-generator/src/main/java/com/nz/admin/modules/generator/repository/GeneratorMetadataRepository.java)、[GeneratorService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-generator/src/main/java/com/nz/admin/modules/generator/service/GeneratorService.java)
- 测试入口：[nz-server/nz-module/nz-generator/src/test/java/com/nz/admin/modules/generator/config/NzGeneratorModuleManifestTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-generator/src/test/java/com/nz/admin/modules/generator/config/NzGeneratorModuleManifestTest.java)（共 4 个测试源文件，不代表本次执行结果）。

### nz-job

任务管理。Independent scheduled-job module: job definitions, Quartz scheduling and execution logs.

- 指南：[任务管理](/platform/system/jobs-logs)
- POM：[nz-server/nz-module/nz-job/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-job/pom.xml)
- 显式生产依赖：`nz-common-core`、`nz-common-job`、`nz-starter-auth`、`nz-starter-log`、`nz-starter-mybatis`、`nz-starter-quartz`、`nz-starter-tenant`、`nz-starter-web`
- 业务清单：[nz-server/nz-module/nz-job/src/main/resources/META-INF/nz/module.yaml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-job/src/main/resources/META-INF/nz/module.yaml)；默认启用。
- 前端清单：[nz-web/src/modules/job/manifest.ts](https://github.com/Nicander93/nz-admin/blob/master/nz-web/src/modules/job/manifest.ts)；组件前缀 `job`。
- 自动配置：[NzJobModuleAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-job/src/main/java/com/nz/admin/modules/job/config/NzJobModuleAutoConfiguration.java)
- 公开接口：[JobLogMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-job/src/main/java/com/nz/admin/modules/job/mapper/job/JobLogMapper.java)、[JobMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-job/src/main/java/com/nz/admin/modules/job/mapper/job/JobMapper.java)、[JobLogService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-job/src/main/java/com/nz/admin/modules/job/service/job/JobLogService.java)、[JobService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-job/src/main/java/com/nz/admin/modules/job/service/job/JobService.java)
- 测试入口：[nz-server/nz-module/nz-job/src/test/java/com/nz/admin/modules/job/service/job/JobSchedulerInitializerTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-job/src/test/java/com/nz/admin/modules/job/service/job/JobSchedulerInitializerTest.java)（共 1 个测试源文件，不代表本次执行结果）。

### nz-system

系统管理。系统管理业务模块，承载用户、角色、菜单、部门、字典、文件、日志、任务等领域。

- 指南：[系统管理](/platform/system/)
- POM：[nz-server/nz-module/nz-system/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/pom.xml)
- 显式生产依赖：`nz-common-core`、`nz-common-module`、`nz-job`、`nz-starter-auth`、`nz-starter-datascope`、`nz-starter-encryption`、`nz-starter-excel`、`nz-starter-file`、`nz-starter-log`、`nz-starter-mail`、`nz-starter-monitor`、`nz-starter-mybatis`、`nz-starter-protection`、`nz-starter-realtime`、`nz-starter-sms`、`nz-starter-social`、`nz-starter-tenant`、`nz-starter-web`
- 业务清单：[nz-server/nz-module/nz-system/src/main/resources/META-INF/nz/module.yaml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/resources/META-INF/nz/module.yaml)；默认启用。
- 前端清单：[nz-web/src/modules/system/manifest.ts](https://github.com/Nicander93/nz-admin/blob/master/nz-web/src/modules/system/manifest.ts)；组件前缀 `system`。
- 自动配置：[NzSystemModuleAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/config/NzSystemModuleAutoConfiguration.java)
- 公开接口：[UserConvert](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/convert/user/UserConvert.java)、[ClientMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/client/ClientMapper.java)、[ConfigMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/config/ConfigMapper.java)、[DeptMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/dept/DeptMapper.java)、[PostMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/dept/PostMapper.java)、[DictDataMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/dict/DictDataMapper.java)、[DictTypeMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/dict/DictTypeMapper.java)、[FileConfigMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/file/FileConfigMapper.java)、[FileMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/file/FileMapper.java)、[LoginLogMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/log/LoginLogMapper.java)、[OperLogMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/log/OperLogMapper.java)、[MenuMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/menu/MenuMapper.java)、[MessageMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/message/MessageMapper.java)、[NoticeMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/notice/NoticeMapper.java)、[RoleDeptMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/role/RoleDeptMapper.java)、[RoleMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/role/RoleMapper.java)、[RoleMenuMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/role/RoleMenuMapper.java)、[SmsChannelMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/sms/SmsChannelMapper.java)、[SmsSendLogMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/sms/SmsSendLogMapper.java)、[SmsTemplateMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/sms/SmsTemplateMapper.java)、[SocialBindingMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/social/SocialBindingMapper.java)、[TenantMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/tenant/TenantMapper.java)、[TenantPackageMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/tenant/TenantPackageMapper.java)、[TenantPackageMenuMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/tenant/TenantPackageMenuMapper.java)、[UserMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/user/UserMapper.java)、[UserPostMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/user/UserPostMapper.java)、[UserRoleMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/mapper/user/UserRoleMapper.java)、[AuthenticationService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/auth/AuthenticationService.java)、[SmsVerificationCodeStore](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/auth/SmsVerificationCodeStore.java)、[SmsVerificationService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/auth/SmsVerificationService.java)、[ClientService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/client/ClientService.java)、[ConfigService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/config/ConfigService.java)、[DeptService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/dept/DeptService.java)、[PostService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/dept/PostService.java)、[DictDataService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/dict/DictDataService.java)、[DictTypeService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/dict/DictTypeService.java)、[FileConfigService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/file/FileConfigService.java)、[FileService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/file/FileService.java)、[LoginLogService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/log/LoginLogService.java)、[OperLogService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/log/OperLogService.java)、[MenuService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/menu/MenuService.java)、[MessageService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/message/MessageService.java)、[MonitorService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/monitor/MonitorService.java)、[NoticeService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/notice/NoticeService.java)、[OnlineSessionAccessor](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/online/OnlineSessionAccessor.java)、[OnlineUserService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/online/OnlineUserService.java)、[PermissionService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/permission/PermissionService.java)、[ProfileService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/profile/ProfileService.java)、[RealtimeService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/realtime/RealtimeService.java)、[RoleService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/role/RoleService.java)、[SmsService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/sms/SmsService.java)、[SocialAccountService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/social/SocialAccountService.java)、[TenantPackageService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/tenant/TenantPackageService.java)、[TenantService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/tenant/TenantService.java)、[UserService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/user/UserService.java)、[WorkbenchService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/workbench/WorkbenchService.java)
- 测试入口：[nz-server/nz-module/nz-system/src/test/java/com/nz/admin/modules/system/config/DataInitializerTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/test/java/com/nz/admin/modules/system/config/DataInitializerTest.java)（共 33 个测试源文件，不代表本次执行结果）。

### nz-workflow

流程中心与业务审批。工作流业务模块，提供官方设计器、流程中心和业务接入，不依赖系统模块。

- 指南：[流程中心与业务审批](/platform/workflow/)
- POM：[nz-server/nz-module/nz-workflow/pom.xml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/pom.xml)
- 显式生产依赖：`nz-common-core`、`nz-common-module`、`nz-starter-auth`、`nz-starter-excel`、`nz-starter-log`、`nz-starter-mybatis`、`nz-starter-protection`、`nz-starter-tenant`、`nz-starter-web`
- 业务清单：[nz-server/nz-module/nz-workflow/src/main/resources/META-INF/nz/module.yaml](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/resources/META-INF/nz/module.yaml)；默认启用。
- 前端清单：[nz-web/src/modules/workflow/manifest.ts](https://github.com/Nicander93/nz-admin/blob/master/nz-web/src/modules/workflow/manifest.ts)；组件前缀 `workflow`。
- 自动配置：[NzWorkflowModuleAutoConfiguration](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/java/com/nz/admin/modules/workflow/config/NzWorkflowModuleAutoConfiguration.java)
- 公开接口：[WorkflowCategoryMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/java/com/nz/admin/modules/workflow/mapper/WorkflowCategoryMapper.java)、[WorkflowDefinitionMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/java/com/nz/admin/modules/workflow/mapper/WorkflowDefinitionMapper.java)、[WorkflowHistoryTaskMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/java/com/nz/admin/modules/workflow/mapper/WorkflowHistoryTaskMapper.java)、[WorkflowInstanceEventMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/java/com/nz/admin/modules/workflow/mapper/WorkflowInstanceEventMapper.java)、[WorkflowInstanceMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/java/com/nz/admin/modules/workflow/mapper/WorkflowInstanceMapper.java)、[WorkflowTaskCopyMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/java/com/nz/admin/modules/workflow/mapper/WorkflowTaskCopyMapper.java)、[WorkflowTaskMapper](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/java/com/nz/admin/modules/workflow/mapper/WorkflowTaskMapper.java)、[WorkflowCategoryService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/java/com/nz/admin/modules/workflow/service/WorkflowCategoryService.java)、[WorkflowDefinitionReferenceChecker](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/java/com/nz/admin/modules/workflow/service/WorkflowDefinitionReferenceChecker.java)、[WorkflowDefinitionService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/java/com/nz/admin/modules/workflow/service/WorkflowDefinitionService.java)、[WorkflowDefinitionUsageChecker](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/java/com/nz/admin/modules/workflow/service/WorkflowDefinitionUsageChecker.java)、[WorkflowInstanceService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/java/com/nz/admin/modules/workflow/service/WorkflowInstanceService.java)、[WorkflowTaskService](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/main/java/com/nz/admin/modules/workflow/service/WorkflowTaskService.java)
- 测试入口：[nz-server/nz-module/nz-workflow/src/test/java/com/nz/admin/modules/workflow/config/NzWorkflowModuleManifestTest.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-workflow/src/test/java/com/nz/admin/modules/workflow/config/NzWorkflowModuleManifestTest.java)（共 11 个测试源文件，不代表本次执行结果）。

## 扩展协议

### AtomicStateStore

复用 TTL、一次消费与原子状态

接口：[nz-server/nz-framework/nz-starter-cache/src/main/java/com/nz/admin/framework/cache/core/AtomicStateStore.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-cache/src/main/java/com/nz/admin/framework/cache/core/AtomicStateStore.java)。接入指南：[阅读](/operations/cluster)。

### AuthUserResolver

解析可信登录身份

接口：[nz-server/nz-framework/nz-starter-auth/src/main/java/com/nz/admin/framework/auth/core/AuthUserResolver.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-auth/src/main/java/com/nz/admin/framework/auth/core/AuthUserResolver.java)。接入指南：[阅读](/platform/security/authentication)。

### DataScopeRuleCustomizer

注册部门与归属过滤规则

接口：[nz-server/nz-framework/nz-starter-datascope/src/main/java/com/nz/admin/framework/datascope/core/DataScopeRuleCustomizer.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-datascope/src/main/java/com/nz/admin/framework/datascope/core/DataScopeRuleCustomizer.java)。接入指南：[阅读](/platform/security/data-scope)。

### FileStorageAdapterFactory

按独立历史配置创建文件存储策略

接口：[nz-server/nz-framework/nz-starter-file/src/main/java/com/nz/admin/framework/file/FileStorageAdapterFactory.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-file/src/main/java/com/nz/admin/framework/file/FileStorageAdapterFactory.java)。接入指南：[阅读](/file-configuration)。

### FileStorageService

实现文件存储读写策略

接口：[nz-server/nz-framework/nz-starter-file/src/main/java/com/nz/admin/framework/file/FileStorageService.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-file/src/main/java/com/nz/admin/framework/file/FileStorageService.java)。接入指南：[阅读](/file-configuration)。

### IdentityValidator

统一核对会话与实时连接的身份状态和版本

接口：[nz-server/nz-common/nz-common-core/src/main/java/com/nz/admin/common/core/identity/IdentityValidator.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-core/src/main/java/com/nz/admin/common/core/identity/IdentityValidator.java)。接入指南：[阅读](/platform/security/session-lifecycle)。

### JobExecutionCoordinator

协调持久执行、业务事务与安全恢复

接口：[nz-server/nz-common/nz-common-job/src/main/java/com/nz/admin/common/job/JobExecutionCoordinator.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-job/src/main/java/com/nz/admin/common/job/JobExecutionCoordinator.java)。接入指南：[阅读](/platform/system/jobs/execution)。

### JobTenantProvider

为调度恢复和触发提供有效租户

接口：[nz-server/nz-common/nz-common-job/src/main/java/com/nz/admin/common/job/JobTenantProvider.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-job/src/main/java/com/nz/admin/common/job/JobTenantProvider.java)。接入指南：[阅读](/platform/system/jobs-logs)。

### MybatisPlusInterceptorCustomizer

按顺序定制 MyBatis 插件

接口：[nz-server/nz-framework/nz-starter-mybatis/src/main/java/com/nz/admin/framework/mybatis/plugin/MybatisPlusInterceptorCustomizer.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-mybatis/src/main/java/com/nz/admin/framework/mybatis/plugin/MybatisPlusInterceptorCustomizer.java)。接入指南：[阅读](/developer/extensions)。

### NzUserNotificationPublisher

跨模块发送逐用户通知

接口：[nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzUserNotificationPublisher.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzUserNotificationPublisher.java)。接入指南：[阅读](/message-center)。

### NzWorkflowBusinessHandler

按实例和序号回调业务

接口：[nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzWorkflowBusinessHandler.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzWorkflowBusinessHandler.java)。接入指南：[阅读](/platform/workflow/business-integration)。

### NzWorkflowBusinessLauncher

业务单据发起审批

接口：[nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzWorkflowBusinessLauncher.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzWorkflowBusinessLauncher.java)。接入指南：[阅读](/platform/workflow/business-integration)。

### NzWorkflowParticipantProvider

提供当前租户的有效办理人

接口：[nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzWorkflowParticipantProvider.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzWorkflowParticipantProvider.java)。接入指南：[阅读](/platform/workflow/designer)。

### OnlineSessionAccessor

适配可分页的在线会话访问

接口：[nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/online/OnlineSessionAccessor.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-module/nz-system/src/main/java/com/nz/admin/modules/system/service/online/OnlineSessionAccessor.java)。接入指南：[阅读](/online-user-management)。

### OperationLogRecorder

将框架日志落入业务存储

接口：[nz-server/nz-framework/nz-starter-log/src/main/java/com/nz/admin/framework/log/core/OperationLogRecorder.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-log/src/main/java/com/nz/admin/framework/log/core/OperationLogRecorder.java)。接入指南：[阅读](/developer/extensions)。

### RealtimeAccessPolicy

按当前权限与连接用途约束实时投递

接口：[nz-server/nz-framework/nz-starter-realtime/src/main/java/com/nz/admin/framework/realtime/core/RealtimeAccessPolicy.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-realtime/src/main/java/com/nz/admin/framework/realtime/core/RealtimeAccessPolicy.java)。接入指南：[阅读](/realtime-communication)。

### RealtimePublisher

向用户或租户发布在线消息

接口：[nz-server/nz-framework/nz-starter-realtime/src/main/java/com/nz/admin/framework/realtime/core/RealtimePublisher.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-realtime/src/main/java/com/nz/admin/framework/realtime/core/RealtimePublisher.java)。接入指南：[阅读](/realtime-communication)。

### SmsProvider

增加短信供应商

接口：[nz-server/nz-framework/nz-starter-sms/src/main/java/com/nz/admin/framework/sms/core/SmsProvider.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-sms/src/main/java/com/nz/admin/framework/sms/core/SmsProvider.java)。接入指南：[阅读](/sms-management)。

### TenantTableRuleCustomizer

在业务模块登记隔离表

接口：[nz-server/nz-framework/nz-starter-tenant/src/main/java/com/nz/admin/framework/tenant/core/TenantTableRuleCustomizer.java](https://github.com/Nicander93/nz-admin/blob/master/nz-server/nz-framework/nz-starter-tenant/src/main/java/com/nz/admin/framework/tenant/core/TenantTableRuleCustomizer.java)。接入指南：[阅读](/platform/security/tenant-isolation)。
