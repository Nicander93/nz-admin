# 架构速览

nz-server/nz-app 负责启动；nz-common 提供公共模型；nz-framework/nz-starter-* 提供跨业务能力；nz-module/nz-system 承载系统管理；nz-web 提供 API、页面和测试。项目不采用复杂 DDD 目录，业务按领域、技术按 starter 聚合。

