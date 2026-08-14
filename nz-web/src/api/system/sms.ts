import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export interface SmsChannel {
  id: number
  channelCode: string
  channelName: string
  providerCode: string
  endpoint?: string
  accessKeyId?: string
  accessKeySecret?: string
  accessKeySecretConfigured?: boolean
  signature?: string
  status: number
  remark?: string
  createTime?: string
}

export interface SmsTemplate {
  id: number
  channelId: number
  channelName?: string
  templateCode: string
  templateName: string
  providerTemplateId?: string
  content: string
  status: number
  remark?: string
  createTime?: string
}

export interface SmsSendLog {
  id: number
  channelId: number
  templateId: number
  phoneNumberMasked: string
  templateCode: string
  content: string
  requestParams: string
  sendStatus: 'PENDING' | 'SUCCESS' | 'FAILED'
  providerMessageId?: string
  errorMessage?: string
  sendTime?: string
  createTime?: string
}

export interface SmsChannelQuery extends PageQuery { keyword?: string; status?: number }
export interface SmsTemplateQuery extends PageQuery { channelId?: number; keyword?: string; status?: number }
export interface SmsLogQuery extends PageQuery { sendStatus?: string }

export function pageSmsChannels(params: SmsChannelQuery) {
  return request.get<PageResult<SmsChannel>>('/api/system/sms/channels/page', { params })
}
export function addSmsChannel(data: Partial<SmsChannel>) {
  return request.post<number>('/api/system/sms/channels', data)
}
export function updateSmsChannel(data: Partial<SmsChannel>) {
  return request.put<void>('/api/system/sms/channels', data)
}
export function deleteSmsChannel(id: number) {
  return request.delete<void>('/api/system/sms/channels/' + id)
}
export function pageSmsTemplates(params: SmsTemplateQuery) {
  return request.get<PageResult<SmsTemplate>>('/api/system/sms/templates/page', { params })
}
export function addSmsTemplate(data: Partial<SmsTemplate>) {
  return request.post<number>('/api/system/sms/templates', data)
}
export function updateSmsTemplate(data: Partial<SmsTemplate>) {
  return request.put<void>('/api/system/sms/templates', data)
}
export function deleteSmsTemplate(id: number) {
  return request.delete<void>('/api/system/sms/templates/' + id)
}
export function pageSmsLogs(params: SmsLogQuery) {
  return request.get<PageResult<SmsSendLog>>('/api/system/sms/logs/page', { params })
}
export function sendTestSms(data: { templateId: number; phoneNumber: string; parameters: Record<string, unknown> }) {
  return request.post<number>('/api/system/sms/send-test', data)
}
