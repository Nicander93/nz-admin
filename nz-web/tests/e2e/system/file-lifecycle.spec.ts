import { randomUUID } from 'node:crypto'
import { readFile } from 'node:fs/promises'

import { expect, test } from '@nz/test/e2e'

import type { Locator } from '@playwright/test'

type FileRow = {
  id: number
  originalName: string
  lifecycleStatus: 'READY' | 'DELETE_PENDING'
}

type ApiResult<T> = {
  code: number
  msg?: string
  data: T
}

test.use({ screenshot: 'off' })

test.describe('系统文件生命周期', () => {
  test.setTimeout(90_000)

  test('上传、批量部分失败、鉴权下载、删除与 API 清理闭环', async ({
    authenticatedPage: page,
  }) => {
    const suffix = randomUUID().replaceAll('-', '')
    const prefix = 'E2E_FILE_LIFECYCLE_' + suffix
    const singleName = prefix + '_single.txt'
    const batchGoodName = prefix + '_batch.txt'
    const batchBadName = prefix + '_blocked.exe'
    const ownNames = new Set([singleName, batchGoodName, batchBadName])
    const createdIds = new Set<number>()
    const singleBody = 'authenticated file body ' + suffix
    let singleResult: ApiResult<FileRow> | undefined
    let singleRow: Locator | undefined
    let batchDialog: Locator | undefined
    const token = await page.evaluate(() => localStorage.getItem('token') ?? '')
    expect(token).toBeTruthy()

    async function fileRows(): Promise<FileRow[]> {
      const response = await page.request.get('/api/system/file/page', {
        headers: { Authorization: token },
        params: { pageNum: 1, pageSize: 100, originalName: prefix },
        timeout: 10_000,
      })
      expect(response.ok()).toBeTruthy()
      const result = (await response.json()) as ApiResult<{ records: FileRow[] }>
      expect(result.code).toBe(200)
      return result.data.records.filter((row) => ownNames.has(row.originalName))
    }

    async function cleanOwnFiles() {
      const rows = await fileRows()
      for (const row of rows) {
        if (!ownNames.has(row.originalName) && !createdIds.has(row.id)) continue
        const response = await page.request.delete('/api/system/file/' + row.id, {
          headers: { Authorization: token },
          timeout: 10_000,
        })
        expect(response.ok()).toBeTruthy()
        const result = (await response.json()) as ApiResult<void>
        expect(result.code).toBe(200)
        createdIds.add(row.id)
      }
      await expect.poll(async () => (await fileRows()).length).toBe(0)
    }

    await cleanOwnFiles()
    let primaryError: unknown
    let cleanupError: unknown
    try {
      await test.step('旧地址跳转到统一文件管理页', async () => {
        await page.goto('/file')
        await expect(page).toHaveURL(/\/system\/file$/)
        await expect(page.getByRole('button', { name: '上传文件', exact: true })).toBeVisible()
      })

      await test.step('上传并查询单个文件', async () => {
      await page.getByRole('button', { name: '上传文件', exact: true }).click()
      const singleDialog = page.getByRole('dialog', { name: '上传文件' })
      const singleInput = singleDialog.locator('input[type="file"]')
      const singleResponsePromise = page.waitForResponse((response) =>
        new URL(response.url()).pathname === '/api/system/file/upload'
        && response.request().method() === 'POST',
      )
      await singleInput.setInputFiles({
        name: singleName,
        mimeType: 'text/plain',
        buffer: Buffer.from(singleBody),
      })
      await singleDialog.getByRole('button', { name: '开始上传' }).click()
      const singleResponse = await singleResponsePromise
      expect(singleResponse.ok()).toBeTruthy()
      singleResult = (await singleResponse.json()) as ApiResult<FileRow>
      expect(singleResult!.code).toBe(200)
      expect(singleResult!.data.originalName).toBe(singleName)
      createdIds.add(singleResult!.data.id)
      await expect(singleDialog).toBeHidden()
      })

      await test.step('搜索并确认文件可用', async () => {
      await page.getByPlaceholder('搜索文件名...').fill(singleName)
      await page.getByRole('button', { name: '搜索', exact: true }).click()
      singleRow = page.getByRole('row').filter({ hasText: singleName })
      await expect(singleRow!).toBeVisible()
      await expect(singleRow!.getByText('可用')).toBeVisible()
      })

      await test.step('验证下载接口响应与浏览器保存内容', async () => {
      const downloadResponsePromise = page.waitForResponse((response) =>
        new URL(response.url()).pathname === '/api/system/file/download/' + singleResult!.data.id
        && response.request().method() === 'GET', { timeout: 10_000 },
      )
      const downloadPromise = page.waitForEvent('download', { timeout: 10_000 })
      await singleRow!.getByRole('button', { name: '下载' }).click()
      const downloadResponse = await downloadResponsePromise
      expect(downloadResponse.status()).toBe(200)
      expect(await downloadResponse.text()).toBe(singleBody)
      const download = await downloadPromise
      expect(download.suggestedFilename()).toBe(singleName)
      const downloadedPath = await download.path()
      expect(downloadedPath).toBeTruthy()
      expect(await readFile(downloadedPath!, 'utf8')).toBe(singleBody)
      })

      await test.step('删除单个文件并确认服务端记录消失', async () => {
      await singleRow!.getByRole('button', { name: '删除', exact: true }).click()
      const deleteConfirm = page.locator('.el-message-box:visible')
      await deleteConfirm.getByRole('button', { name: '确认删除' }).click({ timeout: 10_000 })
      await expect(singleRow!).toBeHidden()
      await expect.poll(async () =>
        (await fileRows()).some((row) => row.id === singleResult!.data.id),
      ).toBe(false)
      })

      await test.step('批量上传并保留失败项与原因', async () => {
      await page.getByRole('button', { name: '批量上传', exact: true }).click()
      batchDialog = page.getByRole('dialog', { name: '批量上传' })
      const batchInput = batchDialog!.locator('input[type="file"]')
      const batchResponsePromise = page.waitForResponse((response) =>
        new URL(response.url()).pathname === '/api/system/file/uploads'
        && response.request().method() === 'POST',
      )
      await batchInput.setInputFiles([
        {
          name: batchGoodName,
          mimeType: 'text/plain',
          buffer: Buffer.from('valid batch payload ' + suffix),
        },
        {
          name: batchBadName,
          mimeType: 'application/x-msdownload',
          buffer: Buffer.from('must be rejected ' + suffix),
        },
      ])
      await batchDialog!.getByRole('button', { name: '上传所选文件' }).click()
      const batchResponse = await batchResponsePromise
      expect(batchResponse.ok()).toBeTruthy()
      const batchResult = (await batchResponse.json()) as ApiResult<{
        successCount: number
        failCount: number
        successFiles: FileRow[]
        failedFiles: Array<{ index: number; fileName: string; reason: string }>
      }>
      expect(batchResult.code).toBe(200)
      expect(batchResult.data.successCount).toBe(1)
      expect(batchResult.data.failCount).toBe(1)
      expect(batchResult.data.successFiles.map((row) => row.originalName)).toContain(batchGoodName)
      expect(batchResult.data.failedFiles).toHaveLength(1)
      expect(batchResult.data.failedFiles[0].index).toBe(1)
      expect(batchResult.data.failedFiles[0].fileName).toBe(batchBadName)
      expect(batchResult.data.failedFiles[0].reason).toMatch(/类型|扩展名|不支持/)
      for (const row of batchResult.data.successFiles) createdIds.add(row.id)

      const selectedFiles = batchDialog!.locator('ul').first()
      await expect(selectedFiles.locator('li').filter({ hasText: batchBadName })).toBeVisible()
      const failureAlert = batchDialog!.locator('.el-alert--warning')
      await expect(failureAlert.locator('li')).toContainText(batchBadName)
      await expect(failureAlert.locator('li')).toContainText(/类型|扩展名|不支持/)
      await expect(page.getByText('批量上传完成：成功 1 个，失败 1 个')).toBeVisible()
      })

      await test.step('核对批量结果并检查非法文件未入库', async () => {
      await batchDialog!.getByRole('button', { name: '关闭' }).click()
      await page.getByPlaceholder('搜索文件名...').fill(batchGoodName)
      await page.getByRole('button', { name: '搜索', exact: true }).click()
      const batchGoodRow = page.getByRole('row').filter({ hasText: batchGoodName })
      await expect(batchGoodRow).toBeVisible()
      await expect(batchGoodRow.getByText('可用')).toBeVisible()

      const finalRows = await fileRows()
      expect(finalRows.some((row) => row.originalName === batchGoodName)).toBe(true)
      expect(finalRows.some((row) => row.originalName === batchBadName)).toBe(false)
      })
    } catch (error) {
      primaryError = error
      throw error
    } finally {
      try {
        await cleanOwnFiles()
      } catch (error) {
        cleanupError = error
        if (primaryError) {
          console.error('File E2E cleanup failed after the primary test error:', error)
        }
      }
    }
    if (cleanupError && !primaryError) throw cleanupError
  })
})
