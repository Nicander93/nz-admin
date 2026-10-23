import { expect, test } from '@playwright/test'

const base = process.env.DOCS_BASE ?? '/'

test('首页、开发指南深链、搜索与主题', async ({ page }) => {
  const failed: string[] = []
  page.on('response', (response) => {
    if (response.status() >= 400)
      failed.push(`${response.status()} ${response.url()}`)
  })
  await page.goto(base)
  await expect(
    page.getByRole('heading', { name: /从后台基础能力/ }),
  ).toBeVisible()
  await page.getByRole('link', { name: '阅读开发指南', exact: true }).click()
  await expect(page.locator('.vp-doc h1')).toHaveText('开发指南')
  await page.reload()
  await expect(page.locator('.vp-doc h1')).toHaveText('开发指南')
  await page.getByRole('button', { name: /搜索文档/ }).click()
  await page.locator('#localsearch-input').fill('数据权限')
  const result = page
    .locator('.VPLocalSearchBox')
    .getByRole('link')
    .filter({ hasText: '数据权限接入' })
    .first()
  await expect(result).toBeVisible()
  await result.click()
  await expect(page.locator('.vp-doc h1')).toContainText('数据权限')
  await page.getByRole('switch').click()
  await expect(page.locator('html')).toHaveClass(/dark/)
  await page.reload()
  await expect(page.locator('html')).toHaveClass(/dark/)
  expect(failed).toEqual([])
})

test('手机导航、功能文章与页面宽度', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 })
  await page.goto(`${base}getting-started`)
  await expect(page.locator('.vp-doc h1')).toHaveText('快速开始')
  await page.getByRole('button', { name: '文档导航', exact: true }).click()
  const sidebar = page.locator('.VPSidebar')
  await expect(sidebar).toBeVisible()
  await sidebar
    .getByRole('link', { name: '工作流与业务接入', exact: true })
    .click()
  await expect(page.locator('.vp-doc h1')).toContainText('工作流')
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true)
})

test('二级专题、三级页面与面包屑返回', async ({ page }) => {
  await page.goto(`${base}capabilities`)
  await page
    .locator('.vp-doc')
    .getByRole('link', { name: '后端开发专题', exact: true })
    .click()
  await expect(page.locator('.vp-doc h1')).toHaveText('后端开发')
  await expect(
    page
      .getByRole('navigation', { name: 'Pager' })
      .getByRole('link', { name: /下一篇.*接口与分页契约/ }),
  ).toBeVisible()
  await page
    .locator('.vp-doc')
    .getByRole('link', { name: '参数校验与异常', exact: true })
    .click()
  await expect(page).toHaveURL(/guide\/backend\/validation/)
  await expect(page.locator('.vp-doc h1')).toHaveText('参数校验与异常处理')
  const trail = page.getByRole('navigation', { name: '阅读路径' })
  await expect(
    trail.getByRole('link', { name: '后端开发', exact: true }),
  ).toBeVisible()
  await page.reload()
  await expect(trail.locator('[aria-current="page"]')).toHaveText(
    '参数校验与异常',
  )
  await trail.getByRole('link', { name: '后端开发', exact: true }).click()
  await expect(page.locator('.vp-doc h1')).toHaveText('后端开发')
  await page
    .locator('.VPSidebar')
    .getByRole('link', { name: '限流、防重与幂等', exact: true })
    .click()
  await expect(page.locator('.vp-doc h1')).toHaveText('限流、防重与业务幂等')
})

test('手机展开三级目录并进入业务回调', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 })
  await page.goto(`${base}platform/workflow/`)
  await page.getByRole('button', { name: '文档导航', exact: true }).click()
  const sidebar = page.locator('.VPSidebar')
  const section = sidebar.locator('.VPSidebarItem.level-1').filter({
    has: page.getByRole('link', { name: '工作流与业务接入', exact: true }),
  })
  const toggle = section
    .locator(':scope > .item')
    .getByRole('button', { name: 'toggle section', exact: true })
  const leaf = sidebar.getByRole('link', {
    name: '业务 SPI 与可靠回调',
    exact: true,
  })
  await expect(toggle).toBeVisible()
  if (await leaf.isVisible()) await toggle.click()
  await expect(leaf).not.toBeVisible()
  await toggle.click()
  await expect(leaf).toBeVisible()
  await leaf.click()
  await expect(page.locator('.vp-doc h1')).toHaveText(
    '业务 SPI、状态同步与重试',
  )
  await expect(
    page.getByRole('navigation', { name: '阅读路径' }),
  ).toContainText('工作流与业务接入')
  await page.reload()
  await expect(page.locator('.vp-doc h1')).toHaveText(
    '业务 SPI、状态同步与重试',
  )
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true)
})

test('源码目录按架构层检索模块并追踪依赖', async ({ page }) => {
  await page.goto(`${base}developer/catalog`)
  const catalog = page.getByRole('region', { name: '源码能力浏览器' })
  const search = catalog.getByRole('searchbox', {
    name: '搜索模块、能力或接口',
  })
  await search.fill('tenant')
  await catalog.getByLabel('架构层').selectOption('framework')
  const results = catalog.getByRole('navigation', { name: '模块结果' })
  await expect(results.getByRole('button', { name: /datascope/ })).toBeVisible()
  await expect(results.getByRole('button', { name: /excel/ })).toHaveCount(0)
  await search.fill('')
  await catalog.getByLabel('架构层').selectOption('all')
  await results.getByRole('button', { name: /nz-demo/ }).click()
  const details = catalog.getByRole('article', { name: '模块详情' })
  await expect(
    details.getByRole('heading', { name: /纵向业务示例/ }),
  ).toBeVisible()
  await details
    .getByRole('button', { name: 'nz-starter-excel', exact: true })
    .click()
  await expect(details.getByRole('heading', { name: /Excel/ })).toBeVisible()
  await expect(details.getByRole('link', { name: '查看 POM ↗' })).toBeVisible()
  await expect(
    details.getByRole('heading', { name: '自动配置入口' }),
  ).toBeVisible()
  const guide = details.getByRole('link', { name: '阅读接入指南' })
  await expect(guide).toHaveAttribute('href', /guide\/backend\/excel/)
  await page.reload()
  await expect(details.getByRole('heading', { name: /Excel/ })).toBeVisible()
  await details.getByRole('link', { name: '阅读接入指南' }).click()
  await expect(page.locator('.vp-doc h1')).toHaveText('Excel 数据交换')
})

test('扩展协议可检索并从手机视口进入所属模块', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto(`${base}developer/catalog`)
  const catalog = page.getByRole('region', { name: '源码能力浏览器' })
  await catalog.getByRole('button', { name: '扩展协议' }).click()
  await catalog
    .getByRole('searchbox', { name: '搜索模块、能力或接口' })
    .fill('TenantTableRuleCustomizer')
  const entry = catalog
    .locator('.catalog__entry')
    .filter({ hasText: 'TenantTableRuleCustomizer' })
  await expect(entry).toBeVisible()
  await entry.getByRole('button', { name: /查看所在模块及依赖/ }).click()
  await expect(
    catalog
      .getByRole('article', { name: '模块详情' })
      .getByText('nz-starter-tenant', { exact: true }),
  ).toBeVisible()
  const trail = page.getByRole('navigation', { name: '阅读路径' })
  await expect(trail).toContainText('开发者中心能力与模块目录')
  await expect(trail.locator('[aria-current="page"]')).toHaveText(
    '能力与模块目录',
  )
  await expect
    .poll(
      async () =>
        page.evaluate(() => {
          const offenders = Array.from(document.querySelectorAll('body *'))
            .filter((el) => {
              const rect = el.getBoundingClientRect()
              const style = getComputedStyle(el)
              return (
                rect.width > 0 &&
                rect.height > 0 &&
                rect.right > innerWidth &&
                style.display !== 'none' &&
                style.visibility !== 'hidden' &&
                !el.closest('.VPSidebar')
              )
            })
            .map((el) => {
              const rect = el.getBoundingClientRect()
              return {
                tag: el.tagName,
                className: typeof el.className === 'string' ? el.className : '',
                text: (el.textContent || '').trim().slice(0, 100),
                right: rect.right,
                width: rect.width,
              }
            })
            .slice(0, 12)
          return {
            fits: document.documentElement.scrollWidth <= innerWidth,
            scrollWidth: document.documentElement.scrollWidth,
            innerWidth,
            offenders,
          }
        }),
      { timeout: 5000 },
    )
    .toMatchObject({ fits: true })
})

test('全站搜索可命中生成的源码协议索引', async ({ page }) => {
  await page.goto(base)
  await page.getByRole('button', { name: /搜索文档/ }).click()
  await page.locator('#localsearch-input').fill('TenantTableRuleCustomizer')
  const result = page
    .locator('.VPLocalSearchBox')
    .getByRole('link')
    .filter({ hasText: 'TenantTableRuleCustomizer' })
    .first()
  await expect(result).toBeVisible()
  await result.click()
  await expect(page).toHaveURL(/reference\/source-index/)
  await expect(page.locator('.vp-doc')).toContainText(
    'TenantTableRuleCustomizer',
  )
})
