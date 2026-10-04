import { defineConfig } from 'vitepress'
import { sidebar } from './sidebar'

const repository = 'https://github.com/Nicander93/nz-admin'
const base = process.env.DOCS_BASE ?? '/'
if (!/^\/(?:[A-Za-z0-9_-]+\/)*$/.test(base)) {
  throw new Error('DOCS_BASE 必须是 / 或 /nz-admin/ 这样的路径')
}

export default defineConfig({
  lang: 'zh-CN',
  title: 'NZ Admin',
  description:
    '面向业务开发的 Java / Vue 后台脚手架：项目能力、开发指南与部署手册。',
  base,
  cleanUrls: true,
  lastUpdated: true,
  srcExclude: [
    'exec-plans/**',
    'agent-task-recipes.md',
    'harness-engineering.md',
  ],
  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: `${base}logo.svg` }],
  ],
  themeConfig: {
    logo: '/logo.svg',
    siteTitle: 'NZ Admin',
    nav: [
      { text: '开始使用', link: '/getting-started' },
      { text: '开发指南', link: '/developer-guide' },
      { text: '项目能力', link: '/capabilities' },
      { text: '部署运维', link: '/operations/' },
    ],
    sidebar,
    outline: { level: [2, 3], label: '本页目录' },
    docFooter: { prev: '上一篇', next: '下一篇' },
    lastUpdated: { text: '最近更新', formatOptions: { dateStyle: 'medium' } },
    editLink: {
      pattern: `${repository}/edit/master/docs/:path`,
      text: '在 GitHub 上编辑此页',
    },
    socialLinks: [{ icon: 'github', link: repository }],
    search: {
      provider: 'local',
      options: {
        locales: {
          root: {
            translations: {
              button: { buttonText: '搜索文档', buttonAriaLabel: '搜索文档' },
              modal: {
                displayDetails: '显示详情',
                resetButtonTitle: '清空搜索',
                backButtonTitle: '返回',
                noResultsText: '没有找到相关文档',
                footer: {
                  selectText: '选择',
                  navigateText: '切换',
                  closeText: '关闭',
                },
              },
            },
          },
        },
      },
    },
    sidebarMenuLabel: '文档导航',
    returnToTopLabel: '返回顶部',
    darkModeSwitchLabel: '切换主题',
    lightModeSwitchTitle: '切换到浅色主题',
    darkModeSwitchTitle: '切换到深色主题',
    footer: {
      message: '文档与源码同步维护 · 能力以当前实现和验收边界为准',
      copyright: 'NZ Admin',
    },
  },
  markdown: {
    config(md) {
      // 仓库内部规范和历史计划保留在 GitHub，正文仍以同一份 Markdown 维护。
      md.core.ruler.after('inline', 'repository-links', (state) => {
        for (const block of state.tokens)
          for (const token of block.children ?? []) {
            if (token.type !== 'link_open') continue
            const href = token.attrGet('href')
            if (href?.startsWith('../'))
              token.attrSet(
                'href',
                `${repository}/blob/master/${href.slice(3)}`,
              )
            else if (
              href?.startsWith('exec-plans/') ||
              href === 'agent-task-recipes.md' ||
              href === 'harness-engineering.md'
            )
              token.attrSet('href', `${repository}/blob/master/docs/${href}`)
          }
      })
    },
  },
})
