import { describe, expect, it, vi } from 'vitest'

vi.mock('@/api/system/sms', () => ({
  pageSmsChannels: vi.fn().mockResolvedValue({ data: { records: [{ id: 1, channelCode: 'local', channelName: '本地', providerCode: 'log', status: 0 }], total: 1 } }),
  pageSmsTemplates: vi.fn().mockResolvedValue({ data: { records: [{ id: 2, channelId: 1, templateCode: 'code', templateName: '验证码', content: '{{code}}', status: 0 }], total: 1 } }),
  pageSmsLogs: vi.fn().mockResolvedValue({ data: { records: [], total: 0 } }),
  addSmsChannel: vi.fn(), updateSmsChannel: vi.fn(), deleteSmsChannel: vi.fn(),
  addSmsTemplate: vi.fn(), updateSmsTemplate: vi.fn(), deleteSmsTemplate: vi.fn(),
  sendTestSms: vi.fn().mockResolvedValue({ data: 10 }),
}))

import { sendTestSms } from '@/api/system/sms'
import { parseParameters, useSmsManagement } from '@/views/system/sms/hooks'

describe('useSmsManagement', () => {
  it('loads channels and templates', async () => {
    const sms = useSmsManagement()
    await sms.loadChannels()
    await sms.loadTemplates()
    expect(sms.channels.records[0].channelCode).toBe('local')
    expect(sms.templates.records[0].templateCode).toBe('code')
  })

  it('accepts only JSON objects as template parameters', () => {
    expect(parseParameters('{"code":"123456"}')).toEqual({ code: '123456' })
    expect(() => parseParameters('[]')).toThrow('JSON 对象')
  })

  it('sends parsed template parameters', async () => {
    const sms = useSmsManagement()
    sms.sendForm.templateId = 2
    sms.sendForm.phoneNumber = '13800138000'
    sms.sendForm.parametersText = '{"code":"654321"}'
    await sms.send()
    expect(sendTestSms).toHaveBeenCalledWith({
      templateId: 2,
      phoneNumber: '13800138000',
      parameters: { code: '654321' },
    })
  })
})
