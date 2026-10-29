import { randomUUID } from 'node:crypto'

import { expect, test } from '@nz/test/e2e'

type ApiResult<T> = { code: number; msg?: string; data: T }
type JobReceipt = { jobId: number; revision: number; scheduleState: string }
type Execution = {
  id: number
  jobId: number
  executionKey: string
  state: 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED'
  triggerSource: string
  nodeId?: string
  recovered: boolean
  attempt: number
  lastError?: string
}
type ExecutionReceipt = { executionId: number; executionKey: string; state: Execution['state'] }
type JobRecord = {
  id: number
  jobName: string
  jobGroup: string
  invokeTarget: string
  cronExpression: string
  status: number
  revision: number
  scheduleState: string
  scheduleError?: string
}
type JobLog = {
  id: number
  jobId: number
  executionId?: number
  executionKey?: string
  status: number
  jobMessage: string
}
type Page<T> = { records: T[]; total: number }

test.describe('定时任务真实生命周期', () => {
  test.setTimeout(180_000)
  test.use({ actionTimeout: 10_000 })

  test('创建、编辑、暂停恢复、异步运行、核对日志并删除本次任务', async ({ authenticatedPage: page }) => {
    const suffix = randomUUID().replaceAll('-', '').slice(0, 16)
    const originalName = `e2e_job_${suffix}`
    const editedName = `${originalName}_edited`
    const group = `E2E_${suffix}`
    const cron = '0 0 0 1 1 ? 2099'
    const token = await page.evaluate(() => localStorage.getItem('token') ?? '')
    expect(token).toBeTruthy()

    let jobId: number | undefined
    let deleted = false
    let bodyError: unknown
    let bodyFailed = false
    let cleanupError: unknown

    async function api<T>(method: string, path: string, data?: unknown) {
      const response = await page.request.fetch(path, {
        method,
        headers: { Authorization: token },
        data,
        timeout: 10_000,
      })
      const result = (await response.json()) as ApiResult<T>
      return { response, result }
    }

    async function requireApi<T>(method: string, path: string, data?: unknown) {
      const { response, result } = await api<T>(method, path, data)
      expect(response.ok(), `${method} ${path}: ${result.msg ?? response.status()}`).toBeTruthy()
      expect(result.code, result.msg).toBe(200)
      return result.data
    }

    async function readJob(id: number) {
      return requireApi<JobRecord>('GET', `/api/system/job/${id}`)
    }

    async function waitForSchedule(id: number, revision: number) {
      let latest: JobRecord | undefined
      await expect.poll(async () => {
        latest = await readJob(id)
        if (latest.revision !== revision) return 'REVISION_CHANGED'
        return latest.scheduleState
      }, { timeout: 30_000, intervals: [250, 500, 1000] }).toBe('SYNCED')
      return latest!
    }

    async function cleanup() {
      if (!jobId) return
      const { response, result } = await api<Page<JobLog>>(
        'GET',
        `/api/system/job/log/page?pageNum=1&pageSize=100&jobId=${jobId}`,
      )
      if (response.ok() && result.code === 200 && result.data.records.length) {
        const ids = result.data.records.map((log) => log.id)
        await requireApi<void>('DELETE', '/api/system/job/log', ids)
      }
      const current = await api<JobRecord>('GET', `/api/system/job/${jobId}`)
      if (current.response.ok() && current.result.code === 200) {
        await requireApi<JobReceipt>('DELETE', `/api/system/job/${jobId}`)
      }
    }

    try {
      await page.getByRole('menuitem', { name: '定时任务', exact: true }).click()
      await expect(page.getByRole('button', { name: 'Add job' })).toBeVisible()
      await page.getByRole('button', { name: 'Add job' }).click()
      const form = page.getByRole('dialog')
      await form.getByRole('textbox', { name: /Job name$/ }).fill(originalName)
      await form.getByRole('textbox', { name: /Group$/ }).fill(group)
      await form.getByRole('textbox', { name: /Invoke target$/ }).fill('demoTask.run')
      await form.getByRole('textbox', { name: /Cron$/ }).fill(cron)
      await form.getByRole('textbox', { name: /Remark$/ }).fill(`fixture ${suffix}`)
      const createResponse = page.waitForResponse((response) =>
        new URL(response.url()).pathname === '/api/system/job' &&
        response.request().method() === 'POST',
      )
      await form.getByTestId('job-save-button').click()
      const created = await createResponse
      expect(created.ok()).toBeTruthy()
      const createResult = (await created.json()) as ApiResult<JobReceipt>
      expect(createResult.code, createResult.msg).toBe(200)
      jobId = createResult.data.jobId
      expect(jobId).toBeGreaterThan(0)
      expect(createResult.data.revision).toBeGreaterThan(0)
      const createdJob = await waitForSchedule(jobId, createResult.data.revision)
      expect(createdJob).toMatchObject({
        id: jobId,
        jobName: originalName,
        jobGroup: group,
        invokeTarget: 'demoTask.run',
        cronExpression: cron,
        status: 0,
        scheduleState: 'SYNCED',
      })

      const nameFilter = page.getByRole('textbox', { name: 'Job name', exact: true })
      await nameFilter.fill(originalName)
      await page.getByRole('button', { name: 'Search' }).click()
      const actions = page.getByTestId(`job-row-${jobId}`)
      await expect(actions).toBeVisible()
      await actions.getByRole('button', { name: 'Edit' }).click()
      const editForm = page.getByRole('dialog')
      await editForm.getByRole('textbox', { name: /Job name$/ }).fill(editedName)
      const updateResponse = page.waitForResponse((response) =>
        new URL(response.url()).pathname === '/api/system/job' &&
        response.request().method() === 'PUT',
      )
      await editForm.getByTestId('job-save-button').click()
      const updated = await updateResponse
      expect(updated.ok()).toBeTruthy()
      const updateResult = (await updated.json()) as ApiResult<JobReceipt>
      expect(updateResult.code, updateResult.msg).toBe(200)
      expect(updateResult.data.jobId).toBe(jobId)
      expect(updateResult.data.revision).toBeGreaterThan(createdJob.revision)
      expect(await waitForSchedule(jobId, updateResult.data.revision)).toMatchObject({
        jobName: editedName,
        scheduleState: 'SYNCED',
      })

      await nameFilter.fill(editedName)
      await page.getByRole('button', { name: 'Search' }).click()
      await expect(actions).toBeVisible()
      const pauseResponse = page.waitForResponse((response) =>
        new URL(response.url()).pathname === `/api/system/job/pause/${jobId}`,
      )
      await actions.getByRole('button', { name: 'Pause' }).click()
      const paused = await pauseResponse
      expect(paused.ok()).toBeTruthy()
      const pauseResult = (await paused.json()) as ApiResult<JobReceipt>
      expect(pauseResult.code, pauseResult.msg).toBe(200)
      expect((await waitForSchedule(jobId, pauseResult.data.revision)).status).toBe(1)

      const resumeResponse = page.waitForResponse((response) =>
        new URL(response.url()).pathname === `/api/system/job/resume/${jobId}`,
      )
      await actions.getByRole('button', { name: 'Resume' }).click()
      const resumed = await resumeResponse
      expect(resumed.ok()).toBeTruthy()
      const resumeResult = (await resumed.json()) as ApiResult<JobReceipt>
      expect(resumeResult.code, resumeResult.msg).toBe(200)
      expect((await waitForSchedule(jobId, resumeResult.data.revision)).status).toBe(0)

      const runResponse = page.waitForResponse((response) =>
        new URL(response.url()).pathname === `/api/system/job/run/${jobId}` &&
        response.request().method() === 'PUT',
      )
      await actions.getByRole('button', { name: 'Run' }).click()
      const runResultResponse = await runResponse
      expect(runResultResponse.ok()).toBeTruthy()
      const runResult = (await runResultResponse.json()) as ApiResult<ExecutionReceipt>
      expect(runResult.code, runResult.msg).toBe(200)
      const receipt = runResult.data
      expect(receipt.executionId).toBeGreaterThan(0)
      expect(receipt.executionKey).toBeTruthy()
      expect(['PENDING', 'RUNNING', 'SUCCEEDED']).toContain(receipt.state)
      const requestKey = runResultResponse.request().headers()['idempotency-key']
      expect(requestKey).toBeTruthy()

      let terminal: Execution | undefined
      await expect.poll(async () => {
        const execution = await requireApi<Execution>(
          'GET', `/api/system/job/execution/${receipt.executionId}`,
        )
        terminal = execution
        return execution.state
      }, { timeout: 45_000, intervals: [250, 500, 1000, 2000] }).toMatch(/^(SUCCEEDED|FAILED|SKIPPED)$/)
      expect(terminal).toMatchObject({
        id: receipt.executionId,
        jobId,
        executionKey: receipt.executionKey,
      })
      expect(terminal?.state, terminal?.lastError).toBe('SUCCEEDED')

      await expect(page.getByRole('dialog', { name: 'Job executions and logs' })).toBeVisible()
      const recordsDialog = page.getByRole('dialog', { name: 'Job executions and logs' })
      await expect(recordsDialog.getByRole('tabpanel', { name: 'Executions', exact: true })
        .getByText(receipt.executionKey, { exact: true })).toBeVisible({ timeout: 15_000 })
      await expect.poll(async () => {
        const logs = await requireApi<Page<JobLog>>(
          'GET', `/api/system/job/log/page?pageNum=1&pageSize=100&jobId=${jobId}`,
        )
        return logs.records.some((log) =>
          log.executionId === receipt.executionId && log.executionKey === receipt.executionKey,
        )
      }, { timeout: 30_000, intervals: [250, 500, 1000] }).toBe(true)
      await recordsDialog.getByRole('tab', { name: 'Logs' }).click()
      await expect(recordsDialog.getByRole('tabpanel', { name: 'Logs', exact: true }).getByRole('row').filter({ hasText: receipt.executionKey })).toBeVisible()
      await expect(recordsDialog.getByRole('tabpanel', { name: 'Logs', exact: true }).getByRole('row').filter({ hasText: receipt.executionKey }).filter({ hasText: String(receipt.executionId) })).toBeVisible()

      await page.keyboard.press('Escape')
      await expect(recordsDialog).toBeHidden()
      const deleteResponse = page.waitForResponse((response) =>
        new URL(response.url()).pathname === `/api/system/job/${jobId}` &&
        response.request().method() === 'DELETE',
      )
      await actions.getByRole('button', { name: 'Delete' }).click()
      await page.getByRole('dialog', { name: 'Delete job', exact: true })
        .getByRole('button', { name: 'OK', exact: true }).click()
      const deletedResponse = await deleteResponse
      expect(deletedResponse.ok()).toBeTruthy()
      const deleteResult = (await deletedResponse.json()) as ApiResult<JobReceipt>
      expect(deleteResult.code, deleteResult.msg).toBe(200)
      deleted = true

      const remaining = await requireApi<Page<JobRecord>>(
        'GET', `/api/system/job/page?pageNum=1&pageSize=100&jobName=${encodeURIComponent(editedName)}`,
      )
      expect(remaining.records.some((job) => job.id === jobId)).toBe(false)
    } catch (error) {
      bodyFailed = true
      bodyError = error
    } finally {
      if (!deleted && jobId) {
        try {
          await cleanup()
        } catch (error) {
          cleanupError = error
        }
      }
    }
    if (bodyFailed) throw bodyError
    if (cleanupError) throw cleanupError
  })
})
