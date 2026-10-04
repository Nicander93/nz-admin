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
