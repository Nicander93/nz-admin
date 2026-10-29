import { beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope } from 'vue'

import { canRetryExecution, useJobPage } from '@/views/job/hooks'

const api = vi.hoisted(() => ({
  createJob: vi.fn(), deleteJob: vi.fn(), getJob: vi.fn(), pageExecutions: vi.fn(),
  pageJobLogs: vi.fn(), pageJobs: vi.fn(), pageScheduleOutbox: vi.fn(), pauseJob: vi.fn(),
  resumeJob: vi.fn(), retryExecution: vi.fn(), retryScheduleOutbox: vi.fn(), runJob: vi.fn(),
  updateJob: vi.fn(), success: vi.fn(), confirm: vi.fn(),
}))
const auth = vi.hoisted(() => ({
  store: null as null | { token: string; permissions: string[] },
}))

vi.mock('@/api/job', () => ({
  createJob: api.createJob, deleteJob: api.deleteJob, getJob: api.getJob,
  pageExecutions: api.pageExecutions, pageJobLogs: api.pageJobLogs, pageJobs: api.pageJobs,
  pageScheduleOutbox: api.pageScheduleOutbox, pauseJob: api.pauseJob, resumeJob: api.resumeJob,
  retryExecution: api.retryExecution, retryScheduleOutbox: api.retryScheduleOutbox,
  runJob: api.runJob, updateJob: api.updateJob,
}))
vi.mock('element-plus', () => ({ ElMessage: { success: api.success }, ElMessageBox: { confirm: api.confirm } }))
vi.mock('@/stores/user', async () => {
  const { reactive } = await import('vue')
  const store = reactive({
    token: 'test-session',
    permissions: [] as string[],
    hasPermission(permission: string) { return this.permissions.includes(permission) },
  })
  auth.store = store
  return { useUserStore: () => store }
})

function page<T>(records: T[] = []) { return { data: { records, total: records.length } } }
function setup() {
  const scope = effectScope()
  const state = scope.run(() => useJobPage())
  if (!state) throw new Error('Job hook setup failed')
  return { scope, state }
}

describe('job page hooks', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useRealTimers()
    api.pageJobs.mockResolvedValue(page())
    api.pageExecutions.mockResolvedValue(page())
    api.pageJobLogs.mockResolvedValue(page())
    api.pageScheduleOutbox.mockResolvedValue(page())
    api.getJob.mockResolvedValue({ data: { id: 1, jobName: 'job one' } })
    api.createJob.mockResolvedValue({ data: { scheduleState: 'PENDING' } })
    api.runJob.mockResolvedValue({ data: { executionKey: 'manual:key', state: 'PENDING' } })
    api.retryExecution.mockResolvedValue({ data: { executionKey: 'manual:key', state: 'PENDING' } })
    api.retryScheduleOutbox.mockResolvedValue({ data: { scheduleState: 'PENDING' } })
    api.confirm.mockResolvedValue(undefined)
    auth.store!.token = 'test-session'
    auth.store!.permissions = []
  })

  it('keeps the editor open after a save failure so the user can retry', async () => {
    api.createJob.mockRejectedValueOnce(new Error('temporary failure'))
    const { scope, state } = setup()
    state.formRef.value = { validate: vi.fn().mockResolvedValue(true) } as never
    state.openCreate()
    await expect(state.save()).rejects.toThrow('temporary failure')
    expect(state.formVisible.value).toBe(true)
    expect(state.saving.value).toBe(false)
    scope.stop()
  })

  it('requires both edit and query permissions to show the edit action', () => {
    const { scope, state } = setup()
    auth.store!.permissions = ['system:job:edit']
    expect(state.canEditJob.value).toBe(false)
    auth.store!.permissions.push('system:job:query')
    expect(state.canEditJob.value).toBe(true)
    scope.stop()
  })

  it('ignores a successful execute receipt after its page scope is disposed', async () => {
    let resolveRun!: (value: unknown) => void
    api.runJob.mockReturnValueOnce(new Promise((resolve) => { resolveRun = resolve }))
    const { scope, state } = setup()
    const execution = state.execute({ id: 19 } as never)
    scope.stop()
    resolveRun({ data: { executionKey: 'late:key', state: 'PENDING' } })
    await execution

    expect(api.success).not.toHaveBeenCalled()
    expect(api.pageExecutions).not.toHaveBeenCalled()
    expect(api.pageJobLogs).not.toHaveBeenCalled()
  })

  it('discards a late receipt when the authenticated account changes', async () => {
    let resolveRun!: (value: unknown) => void
    api.runJob.mockReturnValueOnce(new Promise((resolve) => { resolveRun = resolve }))
    const { scope, state } = setup()
    const execution = state.execute({ id: 19 } as never)
    auth.store!.token = 'other-session'
    resolveRun({ data: { executionKey: 'old-account:key', state: 'PENDING' } })
    await execution

    expect(api.success).not.toHaveBeenCalled()
    expect(state.recordsVisible.value).toBe(false)
    expect(api.pageExecutions).not.toHaveBeenCalled()
    scope.stop()
  })

  it('does not let an old save receipt close or replace a newly opened editor', async () => {
    let resolveSave!: (value: unknown) => void
    api.createJob.mockReturnValueOnce(new Promise((resolve) => { resolveSave = resolve }))
    const { scope, state } = setup()
    state.formRef.value = { validate: vi.fn().mockResolvedValue(true) } as never
    state.openCreate()
    const save = state.save()
    await Promise.resolve()

    state.openCreate()
    state.form.jobName = 'new dialog value'
    resolveSave({ data: { scheduleState: 'SYNCED' } })
    await save

    expect(state.formVisible.value).toBe(true)
    expect(state.form.jobName).toBe('new dialog value')
    expect(api.success).not.toHaveBeenCalled()
    scope.stop()
  })

  it('keeps the newest selected job records when requests resolve out of order', async () => {
    let resolveOld!: (value: unknown) => void
    let resolveNew!: (value: unknown) => void
    api.pageExecutions
      .mockReturnValueOnce(new Promise((resolve) => { resolveOld = resolve }))
      .mockReturnValueOnce(new Promise((resolve) => { resolveNew = resolve }))
    api.pageJobLogs.mockResolvedValueOnce(page([{ id: 1 }])).mockResolvedValueOnce(page([{ id: 2 }]))
    const { scope, state } = setup()
    const oldSelection = state.openRecords(1)
    const newSelection = state.openRecords(2)
    resolveNew(page([{ id: 2, jobId: 2 }]))
    await newSelection
    resolveOld(page([{ id: 1, jobId: 1 }]))
    await oldSelection
    expect(state.recordsJobId.value).toBe(2)
    expect(state.executions.value[0]?.jobId).toBe(2)
    expect(state.logs.value[0]?.id).toBe(2)
    scope.stop()
  })

  it('does not let an older edit response replace the newly selected job', async () => {
    let resolveFirst!: (value: unknown) => void
    api.getJob
      .mockReturnValueOnce(new Promise((resolve) => { resolveFirst = resolve }))
      .mockResolvedValueOnce({ data: { id: 2, jobName: 'new job', invokeTarget: 'demoTask.run' } })
    const { scope, state } = setup()
    const first = state.openEdit(1)
    await state.openEdit(2)
    resolveFirst({ data: { id: 1, jobName: 'old job', invokeTarget: 'demoTask.run' } })
    await first

    expect(state.editingId.value).toBe(2)
    expect(state.form.jobName).toBe('new job')
    scope.stop()
  })

  it('reuses a run idempotency key after request failure and reports the async receipt as submitted', async () => {
    api.runJob.mockRejectedValueOnce(new Error('connection lost'))
    const { scope, state } = setup()
    const row = { id: 19 } as never
    await expect(state.execute(row)).rejects.toThrow('connection lost')
    await state.execute(row)
    const firstKey = api.runJob.mock.calls[0]?.[1]
    expect(firstKey).toBeTruthy()
    expect(api.runJob.mock.calls[1]?.[1]).toBe(firstKey)
    expect(api.success).toHaveBeenCalledWith(expect.stringContaining('Execution submitted'))
    scope.stop()
  })

  it('uses server retry eligibility instead of inferring it from recoverable metadata', () => {
    expect(canRetryExecution({ state: 'FAILED', retryable: true } as never)).toBe(true)
    expect(canRetryExecution({ state: 'FAILED', attempt: 2, recoverable: true, retryable: false } as never)).toBe(false)
    expect(canRetryExecution({ state: 'FAILED', attempt: 0, recoverable: false } as never)).toBe(false)
    expect(canRetryExecution({ state: 'SUCCEEDED', retryable: true } as never)).toBe(false)
  })

  it('submits outbox retry as pending and does not present it as synchronized', async () => {
    const { scope, state } = setup()
    await state.retryOutbox({ id: 4, state: 'ERROR', operation: 'DELETE' } as never)

    expect(api.retryScheduleOutbox).toHaveBeenCalledWith(4)
    expect(api.success).toHaveBeenCalledWith('Schedule retry submitted: PENDING')
    expect(api.success).not.toHaveBeenCalledWith(expect.stringContaining('synchronized'))
    scope.stop()
  })

  it('stops record polling when its owning scope is disposed', async () => {
    vi.useFakeTimers()
    const { scope, state } = setup()
    await state.openRecords(5)
    const callsBeforeDispose = api.pageExecutions.mock.calls.length
    scope.stop()
    await vi.advanceTimersByTimeAsync(3000)
    expect(api.pageExecutions).toHaveBeenCalledTimes(callsBeforeDispose)
    vi.useRealTimers()
  })
})
