import request from '@/api/request'

export type MonitorSummary = {
  databaseOk: boolean
  databaseMessage?: string
  heapUsedBytes: number
  heapMaxBytes: number
  uptimeMs: number
  availableProcessors: number
}

export function getMonitorSummary() {
  return request.get<MonitorSummary>('/api/system/monitor/summary')
}
