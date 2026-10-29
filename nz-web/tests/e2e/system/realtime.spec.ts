import { randomUUID } from 'node:crypto'

import { expect, test } from '@nz/test/e2e'

test('实时诊断台可通过 SSE 和 WebSocket 接收定向消息并主动断开', async ({ authenticatedPage: page }) => {
  await page.getByRole('menuitem', { name: '实时通信', exact: true }).click()
  await expect(page).toHaveURL(/realtime$/)
  const consolePage = page.locator('.realtime-page')
  const status = consolePage.locator('.card-header .el-tag')
  await expect(status).toHaveText('未连接')
  await page.reload()
  await expect(status).toHaveText('未连接')

  for (const transport of ['SSE', 'WebSocket']) {
    await test.step(transport, async () => {
      await consolePage.getByText(transport, { exact: true }).click()
      await consolePage.getByRole('button', { name: '建立连接', exact: true }).click()
      await expect(status).toHaveText('已连接')
      const marker = `e2e-${transport}-${randomUUID()}`
      await consolePage.locator('textarea').fill(marker)
      await consolePage.getByRole('button', { name: '发送给我的连接', exact: true }).click()
      const event = consolePage.getByRole('row').filter({ hasText: marker })
      await expect(event).toHaveCount(1)
      await expect(event).toContainText('test')
      await consolePage.getByRole('button', { name: '断开', exact: true }).click()
      await expect(status).toHaveText('未连接')
      await expect(consolePage.getByRole('button', { name: '发送给我的连接', exact: true })).toBeDisabled()
    })
  }
})
