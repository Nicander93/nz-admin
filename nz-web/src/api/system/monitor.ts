import request from '@/api/request'

export type MonitorSummary = {
  databaseOk: boolean
  databaseMessage?: string
  heapUsedBytes: number
  heapMaxBytes: number
  uptimeMs: number
  availableProcessors: number
  redisAvailable: boolean
  redisOk: boolean
  redisMessage?: string
  redisVersion?: string
  redisMode?: string
  redisConnectedClients: number
  redisUsedMemoryBytes: number
  redisKeyCount: number
}

export function getMonitorSummary() {
  return request.get<MonitorSummary>('/api/system/monitor/summary')
}
