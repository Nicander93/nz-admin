import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  createJob,
  pageExecutions,
  pageJobLogs,
  runJob,
  updateJob,
} from '@/api/job'

const api = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}))

vi.mock('@/api/request', () => ({ default: api }))

describe('job API', () => {
  beforeEach(() => {
    api.get.mockReset()
    api.post.mockReset()
    api.put.mockReset()
    api.delete.mockReset()
  })

  it('sends the caller-owned idempotency key when queueing a manual execution', async () => {
    api.put.mockResolvedValue({
      code: 200,
      data: { executionId: 81, executionKey: 'manual:job-4:run-1', state: 'PENDING' },
    })

    await expect(runJob(4, 'run-1')).resolves.toMatchObject({
      data: { executionId: 81, executionKey: 'manual:job-4:run-1', state: 'PENDING' },
    })
    expect(api.put).toHaveBeenCalledWith('/api/system/job/run/4', undefined, {
      headers: { 'Idempotency-Key': 'run-1' },
    })
  })

  it('keeps create and update on the existing resource contract', async () => {
    api.post.mockResolvedValue({ code: 200, data: { jobId: 10, revision: 1, scheduleState: 'PENDING' } })
    api.put.mockResolvedValue({ code: 200, data: { jobId: 10, revision: 2, scheduleState: 'SYNCED' } })
    const input = {
      jobName: 'fixture',
      jobGroup: 'E2E',
      invokeTarget: 'demoTask.run',
      cronExpression: '0 0 0 1 1 ? 2099',
      status: 0,
      concurrent: 1,
      misfirePolicy: 3,
      remark: '',
    }

    await createJob(input)
    await updateJob({ ...input, id: 10 })

    expect(api.post).toHaveBeenCalledWith('/api/system/job', input)
    expect(api.put).toHaveBeenCalledWith('/api/system/job', { ...input, id: 10 })
  })

  it('filters executions and logs by the selected job', async () => {
    api.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })

    await pageExecutions({ jobId: 10, executionKey: 'manual:run-1', pageNum: 1, pageSize: 50 })
    await pageJobLogs({ jobId: 10, pageNum: 1, pageSize: 50 })

    expect(api.get).toHaveBeenNthCalledWith(1, '/api/system/job/execution/page', {
      params: { jobId: 10, executionKey: 'manual:run-1', pageNum: 1, pageSize: 50 },
    })
    expect(api.get).toHaveBeenNthCalledWith(2, '/api/system/job/log/page', {
      params: { jobId: 10, pageNum: 1, pageSize: 50 },
    })
  })
})
