import request from '@/api/request'
import type { RequestOptions } from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export type MessageCategory = 'system' | 'notice' | 'workflow'

export interface SystemMessage {
  id: number
  category: MessageCategory
  type: string
  source: string
  title: string
  summary?: string
  content: string
  data?: unknown
  path?: string
  readStatus: number
  readTime?: string
  createTime?: string
}

export interface MessageQuery extends PageQuery {
  category?: MessageCategory | ''
  title?: string
  readStatus?: number
}

export interface MessageSendRequest {
  category: MessageCategory
  type?: string
  source?: string
  title: string
  summary?: string
  content: string
  dataJson?: string
  path?: string
  targetType: 'ALL' | 'USERS'
  userIds: number[]
}

export function pageMessages(params: MessageQuery, config?: RequestOptions) {
  return request.get<PageResult<SystemMessage>>('/api/system/message/page', { ...config, params })
}

export function getMessage(id: number, config?: RequestOptions) {
  return request.get<SystemMessage>(`/api/system/message/${id}`, config)
}

export function getUnreadMessageCount(config?: RequestOptions) {
  return request.get<number>('/api/system/message/unread-count', config)
}

export function markMessageRead(id: number, config?: RequestOptions) {
  return request.put<void>(`/api/system/message/${id}/read`, undefined, config)
}

export function markAllMessagesRead(config?: RequestOptions) {
  return request.put<number>('/api/system/message/read-all', undefined, config)
}

export function deleteMessage(id: number, config?: RequestOptions) {
  return request.delete<void>(`/api/system/message/${id}`, config)
}

export function sendMessage(data: MessageSendRequest, config?: RequestOptions) {
  return request.post<number>('/api/system/message/send', data, config)
}
