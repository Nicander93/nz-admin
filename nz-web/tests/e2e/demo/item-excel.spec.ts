import { readFile } from 'node:fs/promises'
import path from 'node:path'

import { expect, test } from '@nz/test/e2e'

test('示例条目 Excel 错误预检、确认导入、模板与筛选导出', async ({
  authenticatedPage: page,
}) => {
  const fixtureName = 'E2E-import-20261023'
  const token = await page.evaluate(() => localStorage.getItem('token') ?? '')
  async function cleanFixture() {
    const response = await page.request.get('/api/demo/item/page', {
      headers: { Authorization: token },
      params: { name: fixtureName, current: 1, size: 100 },
    })
    const result = await response.json()
    expect(result.code).toBe(200)
    for (const row of result.data.records as Array<{
      id: number
      name: string
    }>) {
      if (row.name !== fixtureName) continue
      const removal = await page.request.delete('/api/demo/item/' + row.id, {
        headers: { Authorization: token },
      })
      expect((await removal.json()).code).toBe(200)
    }
  }
  await cleanFixture()
  try {
    await page.goto('/demo/item')
    await page.getByRole('button', { name: '导入', exact: true }).click()
    const dialog = page.getByRole('dialog', { name: '导入示例条目' })
    await expect(dialog).toBeVisible()
    const input = dialog.locator('input[type="file"]')
    await input.setInputFiles(
      path.resolve(process.cwd(), 'tests/fixtures/demo-item-invalid.xlsx'),
    )
    const errorTable = dialog.getByRole('table').nth(1)
    const missingName = errorTable
      .getByRole('row')
      .filter({ hasText: '条目名称不能为空' })
    const invalidStatus = errorTable
      .getByRole('row')
      .filter({ hasText: '状态值不正确' })
    await expect(missingName).toBeVisible()
    await expect(missingName.getByRole('cell').first()).toHaveText('2')
    await expect(invalidStatus).toBeVisible()
    await expect(invalidStatus.getByRole('cell').first()).toHaveText('2')
    await expect(
      dialog.getByRole('button', { name: '确认导入' }),
    ).toBeDisabled()

    await input.setInputFiles(
      path.resolve(process.cwd(), 'tests/fixtures/demo-item-valid.xlsx'),
    )
    await expect(
      dialog.getByText('共 1 行，有效 1 行，错误 0 项'),
    ).toBeVisible()
    await expect(dialog.getByRole('button', { name: '确认导入' })).toBeEnabled()
    await dialog.getByRole('button', { name: '确认导入' }).click()
    await expect(dialog).toBeHidden()

    await page.getByPlaceholder('请输入条目名称').fill(fixtureName)
    await page.getByRole('button', { name: '查询', exact: true }).click()
    await expect(
      page.getByRole('cell', { name: fixtureName, exact: true }),
    ).toBeVisible()
    const templateDownload = page.waitForEvent('download')
    await page.getByRole('button', { name: '导入', exact: true }).click()
    await page.getByRole('button', { name: '下载导入模板' }).click()
    expect((await templateDownload).suggestedFilename()).toContain('.xlsx')
    await page.getByRole('button', { name: '关闭', exact: true }).click()
    const exportDownload = page.waitForEvent('download')
    await page.getByRole('button', { name: '按筛选导出' }).click()
    const exported = await exportDownload
    expect(exported.suggestedFilename()).toBe('示例条目.xlsx')
    expect(await exported.failure()).toBeNull()
    const filePath = await exported.path()
    expect(filePath).toBeTruthy()
    const contents = await readFile(filePath!)
    expect(contents.subarray(0, 2).toString()).toBe('PK')
  } finally {
    await cleanFixture()
  }
})
