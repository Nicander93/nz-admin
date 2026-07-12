import { describe, expect, it, vi } from 'vitest'

vi.mock('@/api/system/monitor', () => ({
  getMonitorSummary: vi.fn().mockResolvedValue({
    data: {
      databaseOk: true,
      heapUsedBytes: 1024,
      heapMaxBytes: 2048,
      uptimeMs: 1000,
      availableProcessors: 4,
      redisAvailable: true,
      redisOk: true,
      redisVersion: '7.2.5',
      redisMode: 'standalone',
      redisConnectedClients: 3,
      redisUsedMemoryBytes: 4096,
      redisKeyCount: 12,
    },
  }),
}))

import { useMonitorSummary } from '@/views/system/monitor/hooks'

describe('useMonitorSummary', () => {
  it('loads Redis metrics as part of the monitor summary', async () => {
    const { table } = useMonitorSummary()
    await table.load()

    expect(table.summary?.redisOk).toBe(true)
    expect(table.summary?.redisKeyCount).toBe(12)
    expect(table.formatBytes(table.summary?.redisUsedMemoryBytes ?? 0)).toBe('4.0 KB')
  })
})