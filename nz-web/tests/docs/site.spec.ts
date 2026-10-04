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
    .filter({ hasText: '数据权限' })
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
