import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export type SysClient = {
  id: number
  clientId: string
  clientName: string
  loginType: string
  tokenTimeout: number
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ClientQuery extends PageQuery {
  clientId?: string
  clientName?: string
  status?: number
}

export function pageClients(params: ClientQuery) {
  return request.get<PageResult<SysClient>>('/api/system/client/page', { params })
}

export function getClient(id: number) {
  return request.get<SysClient>('/api/system/client/' + id)
}

export function addClient(data: Partial<SysClient>) {
  return request.post<number>('/api/system/client', data)
}

export function updateClient(data: Partial<SysClient>) {
  return request.put<void>('/api/system/client', data)
}

export function deleteClient(id: number) {
  return request.delete<void>('/api/system/client/' + id)
}
