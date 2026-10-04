import type { DefaultTheme } from 'vitepress'

export const sidebar: DefaultTheme.SidebarItem[] = [
  {
    text: '了解项目',
    items: [
      { text: '项目介绍', link: '/overview' },
      { text: '能力总览与边界', link: '/capabilities' },
      { text: '与 RuoYi-Plus 文档对照', link: '/overview/ruoyi-comparison' },
    ],
  },
  {
    text: '开始使用',
    link: '/getting-started',
    items: [
      { text: '快速开始', link: '/getting-started' },
      { text: '环境准备', link: '/start/environment' },
      { text: '配置文件与生效方式', link: '/start/configuration' },
      { text: '首次登录与验收', link: '/start/first-login' },
    ],
  },
  {
    text: '开发指南',
    link: '/developer-guide',
    items: [
      { text: '开发路线', link: '/developer-guide' },
      { text: '架构与模块边界', link: '/architecture' },
      {
        text: '模块与工具',
        collapsed: true,
        items: [
          { text: '创建业务模块', link: '/module-development-guide' },
          { text: '扩展模块体系', link: '/extension-module-system' },
          { text: '示例模块', link: '/demo-module' },
          { text: '项目 CLI', link: '/cli' },
          { text: '代码生成器', link: '/code-generator' },
          { text: '类型转换', link: '/convert' },
        ],
      },
      {
        text: '后端开发',
        link: '/guide/backend/',
        collapsed: true,
        items: [
          { text: '接口与分页契约', link: '/guide/backend/api-contract' },
          { text: '参数校验与异常', link: '/guide/backend/validation' },
          { text: '持久化与事务', link: '/guide/backend/persistence' },
          { text: '限流、防重与幂等', link: '/guide/backend/protection' },
          { text: '完整 CRUD 范式', link: '/crud-paradigm' },
        ],
      },
      {
        text: '前端开发',
        link: '/guide/frontend/',
        collapsed: true,
        items: [
          { text: '动态路由与模块清单', link: '/guide/frontend/routing' },
          { text: 'CRUD 页面与 hooks', link: '/guide/frontend/crud-page' },
          { text: '请求、下载与错误', link: '/guide/frontend/request' },
          { text: '前端编码规范', link: '/frontend-coding-conventions' },
        ],
      },
      {
        text: '数据库开发',
        link: '/guide/database/',
        collapsed: true,
        items: [
          { text: '迁移与升级步骤', link: '/guide/database/migrations' },
          { text: '迁移版本手册', link: '/database-migrations' },
        ],
      },
      {
        text: '测试与验收',
        link: '/guide/testing/',
        collapsed: true,
        items: [
          { text: '后端与真实数据库测试', link: '/guide/testing/backend' },
          { text: '浏览器验收', link: '/guide/testing/browser' },
          { text: '通用测试支持', link: '/testing' },
        ],
      },
    ],
  },
  {
    text: '平台能力',
    items: [
      {
        text: '系统管理',
        link: '/platform/system/',
        collapsed: true,
        items: [
          { text: '用户与角色', link: '/platform/system/users-roles' },
          { text: '菜单与按钮', link: '/platform/system/menus' },
          { text: '字典、参数与公告', link: '/platform/system/dictionaries' },
          { text: '任务与日志', link: '/platform/system/jobs-logs' },
          { text: '在线用户', link: '/online-user-management' },
          { text: '个人中心', link: '/user-profile' },
        ],
      },
      {
        text: '权限与隔离',
        link: '/platform/security/',
        collapsed: true,
        items: [
          { text: '登录与可信身份', link: '/platform/security/authentication' },
          {
            text: '租户管理与表隔离',
            link: '/platform/security/tenant-isolation',
          },
          { text: '角色范围与接入', link: '/platform/security/data-scope' },
          { text: '多租户技术手册', link: '/multi-tenancy' },
          { text: '数据权限技术手册', link: '/data-permission' },
          { text: '字段加密', link: '/field-encryption' },
        ],
      },
      {
        text: '工作流与业务接入',
        link: '/platform/workflow/',
        collapsed: true,
        items: [
          { text: '官方设计器与版本', link: '/platform/workflow/designer' },
          {
            text: '审批中心与请假',
            link: '/platform/workflow/approval-center',
          },
          {
            text: '业务 SPI 与可靠回调',
            link: '/platform/workflow/business-integration',
          },
          { text: '工作流排错', link: '/platform/workflow/troubleshooting' },
          { text: '完整工作流技术手册', link: '/workflow' },
        ],
      },
      {
        text: '存储与通知',
        collapsed: true,
        items: [
          { text: '文件配置与存储', link: '/file-configuration' },
          { text: '站内消息', link: '/message-center' },
          { text: '实时通信', link: '/realtime-communication' },
          { text: '短信与验证码', link: '/sms-management' },
          { text: '邮件', link: '/mail' },
          { text: '第三方登录', link: '/social-login' },
          { text: 'Redis 监控', link: '/redis-monitor' },
        ],
      },
    ],
  },
  {
    text: '部署运维',
    link: '/operations/',
    items: [
      { text: '运维路线', link: '/operations/' },
      { text: 'Compose 生产部署', link: '/deployment' },
      {
        text: '运行与恢复',
        collapsed: true,
        items: [
          { text: '生产配置清单', link: '/operations/configuration' },
          { text: '双节点配置与验收', link: '/operations/cluster' },
          { text: '备份、恢复与回滚', link: '/operations/backup' },
          { text: '按症状排错', link: '/operations/troubleshooting' },
        ],
      },
      { text: '常见问题', link: '/faq' },
      { text: '维护与部署文档站', link: '/documentation-site' },
    ],
  },
]
