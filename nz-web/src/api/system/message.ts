import request from '@/api/request'
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

export function pageMessages(params: MessageQuery) {
  return request.get<PageResult<SystemMessage>>('/api/system/message/page', { params })
}

export function getMessage(id: number) {
  return request.get<SystemMessage>(`/api/system/message/${id}`)
}

export function getUnreadMessageCount() {
  return request.get<number>('/api/system/message/unread-count')
}

export function markMessageRead(id: number) {
  return request.put<void>(`/api/system/message/${id}/read`)
}

export function markAllMessagesRead() {
  return request.put<number>('/api/system/message/read-all')
}

export function deleteMessage(id: number) {
  return request.delete<void>(`/api/system/message/${id}`)
}

export function sendMessage(data: MessageSendRequest) {
  return request.post<number>('/api/system/message/send', data)
}
