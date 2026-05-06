import request from '@/api/request'

export type SysConfig = {
  id: number
  configName: string
  configKey: string
  configValue: string
  configType: number
  status: number
  remark?: string
  createTime?: string
}

export function listConfigs(params?: { configName?: string; configKey?: string; status?: number }) {
  return request.get<SysConfig[]>('/api/system/config/list', { params })
}

export function getConfig(id: number) {
  return request.get<SysConfig>(`/api/system/config/${id}`)
}

export function addConfig(data: Partial<SysConfig>) {
  return request.post<void>('/api/system/config', data)
}

export function updateConfig(data: Partial<SysConfig>) {
  return request.put<void>('/api/system/config', data)
}

export function deleteConfig(id: number) {
  return request.delete<void>(`/api/system/config/${id}`)
}
