import request from '@/api/request'

export interface MailTestRequest {
  to: string
  subject: string
  content: string
  html: boolean
}

export function sendTestMail(data: MailTestRequest) {
  return request.post<void>('/api/system/mail/test', data)
}