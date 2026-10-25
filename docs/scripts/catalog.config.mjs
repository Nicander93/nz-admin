// 这里只维护阅读入口和能力说明；模块、依赖、入口与接口来自实际源码。
export const moduleGuides = {
  'nz-server': ['后端父工程', '/developer/architecture'],
  'nz-common': ['公共协议聚合', '/developer/architecture'],
  'nz-framework': ['框架聚合', '/developer/architecture'],
  'nz-module': ['业务聚合', '/developer/module-lifecycle'],
  'nz-app': ['应用装配', '/developer/architecture'],
  'nz-common-core': ['统一响应与基础对象', '/guide/backend/api-contract'],
  'nz-common-job': ['任务执行协议', '/platform/system/jobs-logs'],
  'nz-common-module': ['模块与业务协作协议', '/developer/extensions'],
  'nz-starter-web': ['Web 入口', '/guide/backend/validation'],
  'nz-starter-auth': ['认证授权', '/platform/security/authentication'],
  'nz-starter-cache': ['缓存与原子共享状态', '/operations/cluster'],
  'nz-starter-datascope': ['功能数据范围', '/platform/security/data-scope'],
  'nz-starter-encryption': ['字段加密与脱敏', '/field-encryption'],
  'nz-starter-excel': ['Excel 数据交换', '/guide/backend/excel'],
  'nz-starter-file': ['文件存储', '/file-configuration'],
  'nz-starter-log': ['操作日志', '/platform/system/jobs-logs'],
  'nz-starter-mail': ['邮件发送', '/mail'],
  'nz-starter-monitor': ['运行状态', '/operations/troubleshooting'],
  'nz-starter-mybatis': ['MyBatis 与分页', '/guide/backend/persistence'],
  'nz-starter-protection': ['限流、防重与幂等', '/guide/backend/protection'],
  'nz-starter-quartz': ['Quartz 调度', '/platform/system/jobs-logs'],
  'nz-starter-realtime': ['SSE 与 WebSocket', '/realtime-communication'],
  'nz-starter-sms': ['短信发送', '/sms-management'],
  'nz-starter-social': ['OAuth2 / OIDC', '/social-login'],
  'nz-starter-tenant': ['可信租户隔离', '/platform/security/tenant-isolation'],
  'nz-starter-test': ['后端测试支持', '/guide/testing/backend'],
  'nz-system': ['系统管理', '/platform/system/'],
  'nz-job': ['任务管理', '/platform/system/jobs-logs'],
  'nz-demo': ['纵向业务示例', '/demo-module'],
  'nz-generator': ['CRUD 代码生成', '/code-generator'],
  'nz-workflow': ['流程中心与业务审批', '/platform/workflow/'],
}

export const extensionSpecs = [
  ['IdentityValidator', '统一核对会话与实时连接的身份状态和版本', '/platform/security/session-lifecycle'],
  ['JobTenantProvider', '为调度恢复和触发提供有效租户', '/platform/system/jobs-logs'],
  ['AuthUserResolver', '解析可信登录身份', '/platform/security/authentication'],
  [
    'TenantTableRuleCustomizer',
    '在业务模块登记隔离表',
    '/platform/security/tenant-isolation',
  ],
  [
    'DataScopeRuleCustomizer',
    '注册部门与归属过滤规则',
    '/platform/security/data-scope',
  ],
  [
    'MybatisPlusInterceptorCustomizer',
    '按顺序定制 MyBatis 插件',
    '/developer/extensions',
  ],
  ['OperationLogRecorder', '将框架日志落入业务存储', '/developer/extensions'],
  ['SmsProvider', '增加短信供应商', '/sms-management'],
  ['FileStorageService', '替换文件存储实现', '/file-configuration'],
  ['RealtimePublisher', '向用户或租户发布在线消息', '/realtime-communication'],
  ['AtomicStateStore', '复用 TTL、一次消费与原子状态', '/operations/cluster'],
  ['NzUserNotificationPublisher', '跨模块发送逐用户通知', '/message-center'],
  [
    'NzWorkflowBusinessLauncher',
    '业务单据发起审批',
    '/platform/workflow/business-integration',
  ],
  [
    'NzWorkflowBusinessHandler',
    '按实例和序号回调业务',
    '/platform/workflow/business-integration',
  ],
  [
    'NzWorkflowParticipantProvider',
    '提供当前租户的有效办理人',
    '/platform/workflow/designer',
  ],
  [
    'OnlineSessionAccessor',
    '适配可分页的在线会话访问',
    '/online-user-management',
  ],
]

export const capabilitySpecs = [
  {
    id: 'modules',
    title: '可装配的业务模块',
    status: 'available',
    summary:
      '后端清单与自动配置、前端 manifest 与动态菜单协同；模块开关重启生效。',
    modules: ['nz-common-module', 'nz-app'],
    guide: '/developer/module-lifecycle',
    sources: [
      'nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzModuleRegistry.java',
      'nz-web/src/core/modules/registry.ts',
    ],
  },
  {
    id: 'isolation',
    title: '功能权限、租户与数据范围',
    status: 'available',
    summary:
      '按功能筛选角色范围，多角色并集，查询和更新删除都过滤；归属由服务端生成。',
    modules: ['nz-starter-auth', 'nz-starter-tenant', 'nz-starter-datascope'],
    guide: '/platform/security/',
    sources: [
      'nz-server/nz-framework/nz-starter-datascope/src/main/java/com/nz/admin/framework/datascope/support/DataScopeHandler.java',
    ],
  },
  {
    id: 'excel',
    title: 'Excel 预检导入与权限导出',
    status: 'available',
    summary:
      '模板、错误行预检、确认后重新校验与原子新增；导出限定当前功能和租户的数据。',
    modules: ['nz-starter-excel', 'nz-demo'],
    guide: '/guide/backend/excel',
    sources: [
      'nz-server/nz-module/nz-demo/src/main/java/com/nz/admin/modules/demo/controller/DemoItemController.java',
    ],
  },
  {
    id: 'workflow',
    title: '官方设计器与业务审批',
    status: 'conditional',
    summary:
      'V30 后启用新引擎，复用经典设计器；业务 SPI、事务事件、失败重试与历史快照。',
    modules: ['nz-workflow', 'nz-common-module', 'nz-demo'],
    guide: '/platform/workflow/',
    sources: [
      'nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzWorkflowBusinessHandler.java',
    ],
  },
  {
    id: 'cluster',
    title: '共享状态与跨节点验证',
    status: 'conditional',
    summary:
      '共享 PostgreSQL / Redis；幂等、会话、票据与退出有真实两节点验收入口。',
    modules: [
      'nz-starter-cache',
      'nz-starter-realtime',
      'nz-starter-protection',
    ],
    guide: '/operations/cluster',
    sources: ['tools/tests/workflow-cluster.mjs'],
  },
  {
    id: 'generator',
    title: 'CLI 与安全 CRUD 起点',
    status: 'available',
    summary:
      '创建模块、迁移检查、代码预览和下载；生成访问规则，业务状态仍需领域校验。',
    modules: ['nz-generator'],
    guide: '/code-generator',
    sources: ['tools/nz-cli/src/module-add.mjs'],
  },
  {
    id: 'source-docs',
    title: '源码驱动的开发者目录',
    status: 'available',
    summary:
      '依赖、清单、接口与指南可追溯；构建检查依赖越界、无效声明和文档快照漂移。',
    modules: ['nz-server'],
    guide: '/developer/catalog',
    sources: ['docs/scripts/source-catalog.mjs'],
  },
  {
    id: 'notifications',
    title: '可扩展通知与登录',
    status: 'conditional',
    summary: '站内消息、短信、邮件与 OAuth2/OIDC；供应商真实账号需要分别验收。',
    modules: [
      'nz-starter-sms',
      'nz-starter-mail',
      'nz-starter-social',
      'nz-system',
    ],
    guide: '/developer/extensions',
    sources: [
      'nz-server/nz-common/nz-common-module/src/main/java/com/nz/admin/common/module/NzUserNotificationPublisher.java',
    ],
  },
  {
    id: 'dynamic-datasource',
    title: '动态数据源与更多数据库',
    status: 'planned',
    summary: '当前以 PostgreSQL 为主，尚未交付动态切换与异构数据库验收。',
    modules: [],
    guide: '/developer/roadmap',
    sources: [],
  },
  {
    id: 'distributed-jobs',
    title: '分布式调度与全局任务运营',
    status: 'planned',
    summary:
      '现有 Quartz 管理不等于全局只执行一次；需独立验证调度竞争和业务幂等。',
    modules: [],
    guide: '/developer/roadmap',
    sources: [],
  },
]
