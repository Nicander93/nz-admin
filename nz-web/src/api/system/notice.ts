import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export type SysNotice = {
  id: number
  title: string
  content: string
  type: number
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface NoticeQuery extends PageQuery {
  title?: string
  type?: number
  status?: number
}

export function pageNotices(params: NoticeQuery) {
  return request.get<PageResult<SysNotice>>('/api/system/notice/page', { params })
}

export function getNotice(id: number) {
  return request.get<SysNotice>(`/api/system/notice/${id}`)
}

export function addNotice(data: Partial<SysNotice>) {
  return request.post<void>('/api/system/notice', data)
}

export function updateNotice(data: Partial<SysNotice>) {
  return request.put<void>('/api/system/notice', data)
}

export function deleteNotice(id: number) {
  return request.delete<void>(`/api/system/notice/${id}`)
}
