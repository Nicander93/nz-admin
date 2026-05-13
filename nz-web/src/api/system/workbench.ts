import request from '@/api/request'

export type LoginLogRow = {
  id: number
  username?: string
  ip?: string
  status?: number
  msg?: string
  loginTime?: string
}

export type OperLogRow = {
  id: number
  title?: string
  operName?: string
  operUrl?: string
  status?: number
  operTime?: string
}

export type WorkbenchSnapshot = {
  recentLoginLogs: LoginLogRow[]
  recentOperLogs: OperLogRow[]
}

export function getWorkbenchSnapshot() {
  return request.get<WorkbenchSnapshot>('/api/system/workbench/snapshot')
}
