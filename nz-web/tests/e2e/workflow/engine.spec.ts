import { expect, test } from '@nz/test/e2e'

test('新引擎定义发布、发起与审批', async ({ authenticatedPage: page }) => {
  test.skip(process.env.E2E_WARM_FLOW_ENABLED !== 'true', '需要启用新引擎')
  await page.goto('/workflow/engine')
  await expect(page.getByText('定义导入与发布', { exact: true })).toBeVisible()
  await expect(
    page.getByRole('img', { name: '流程节点和连线预览' }),
  ).toBeVisible()
  await page.getByRole('button', { name: '编辑节点 审核', exact: true }).click()
  const editor = page.locator('.model-editor')
  await editor.getByLabel('节点名称', { exact: true }).fill('浏览器审核')
  await editor.getByLabel('节点名称', { exact: true }).press('Tab')
  await editor.getByLabel('节点编码', { exact: true }).fill('browser_review')
  await editor.getByLabel('节点编码', { exact: true }).press('Tab')
  await page.getByRole('tab', { name: 'JSON 导入' }).click()
  const code = `browser_${Date.now()}`
  const model = page.getByRole('textbox', { name: '流程模型 JSON' })
  const data = JSON.parse(await model.inputValue()) as Record<string, unknown>
  expect(
    (data.nodeList as { skipList: { nextNodeCode: string }[] }[])[0]
      ?.skipList[0]?.nextNodeCode,
  ).toBe('browser_review')
  data.flowCode = code
  await model.fill(JSON.stringify(data))
  let definitionId = ''
  let instanceId = ''
  try {
    await page.getByRole('button', { name: '导入新版本', exact: true }).click()
    await expect(
      page.getByRole('textbox', { name: '定义 ID' }),
    ).not.toHaveValue('')
    definitionId = await page
      .getByRole('textbox', { name: '定义 ID' })
      .inputValue()
    await page.getByRole('button', { name: '发布定义', exact: true }).click()
    await expect(page.getByText('发布成功', { exact: true })).toBeVisible()
    await page.getByPlaceholder('填写业务单据的唯一编号').fill(code)
    await page.getByRole('button', { name: '发起流程', exact: true }).click()
    await expect(
      page.getByRole('textbox', { name: '实例 ID' }),
    ).not.toHaveValue('')
    instanceId = await page
      .getByRole('textbox', { name: '实例 ID' })
      .inputValue()
    await page.getByRole('button', { name: '通过', exact: true }).click()
    await expect(
      page.getByRole('button', { name: '通过', exact: true }),
    ).toHaveCount(0)
    await expect(page.getByText(/状态：\s*已完成/)).toBeVisible()
    await expect(
      page.locator('.el-table').last().getByText('浏览器审核', { exact: true }),
    ).toBeVisible()
  } finally {
    const token = await page.evaluate(() => localStorage.getItem('token'))
    const headers = { Authorization: token ?? '' }
    if (instanceId) {
      const response = await page.request.delete(
        `/api/workflow/engine/instances/${instanceId}`,
        { headers },
      )
      expect((await response.json()).code).toBe(200)
    }
    if (definitionId) {
      const response = await page.request.delete(
        `/api/workflow/engine/definitions/${definitionId}`,
        { headers },
      )
      expect((await response.json()).code).toBe(200)
    }
  }
})
