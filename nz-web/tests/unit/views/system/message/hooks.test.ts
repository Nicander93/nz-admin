import { beforeEach, describe, expect, it, vi } from 'vitest'

const api = vi.hoisted(() => ({
  pageMessages: vi.fn(),
  getMessage: vi.fn(),
  markMessageRead: vi.fn(),
  markAllMessagesRead: vi.fn(),
  deleteMessage: vi.fn(),
  sendMessage: vi.fn(),
  pageUsers: vi.fn(),
  success: vi.fn(),
  warning: vi.fn(),
  confirm: vi.fn(),
}))

vi.mock('@/api/system/message', () => ({
  pageMessages: api.pageMessages,
  getMessage: api.getMessage,
  markMessageRead: api.markMessageRead,
  markAllMessagesRead: api.markAllMessagesRead,
  deleteMessage: api.deleteMessage,
  sendMessage: api.sendMessage,
}))
vi.mock('@/api/system/user', () => ({ pageUsers: api.pageUsers }))
vi.mock('element-plus', () => ({
  ElMessage: { success: api.success, warning: api.warning },
  ElMessageBox: { confirm: api.confirm },
}))

import {
  useMessageCenter,
  validateMessageSendForm,
} from '@/views/system/message/hooks'

describe('message center hooks', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.pageMessages.mockResolvedValue({
      data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
    })
    api.markMessageRead.mockResolvedValue({ data: undefined })
  })

  it('validates selected receivers, internal path and JSON payload', () => {
    const base = {
      category: 'notice' as const,
      title: '通知',
      content: '内容',
      targetType: 'USERS' as const,
      userIds: [] as number[],
    }

    expect(validateMessageSendForm(base)).toBe('请选择接收用户')
    expect(validateMessageSendForm({ ...base, userIds: [1], path: '//evil.test' }))
      .toBe('跳转路径必须是站内绝对路径')
    expect(validateMessageSendForm({ ...base, userIds: [1], dataJson: '{broken' }))
      .toBe('扩展数据必须是有效 JSON')
    expect(validateMessageSendForm({ ...base, userIds: [1], dataJson: '{"id":1}' }))
      .toBeNull()
  })

  it('opens an unread message, marks it read and refreshes the inbox', async () => {
    api.getMessage.mockResolvedValue({
      data: {
        id: 8,
        category: 'system',
        type: 'message',
        source: 'backend',
        title: '维护',
        content: '今晚维护',
        readStatus: 0,
      },
    })
    const dispatch = vi.spyOn(window, 'dispatchEvent')
    const { actions, detail } = useMessageCenter()

    await actions.openDetail({
      id: 8,
      category: 'system',
      type: 'message',
      source: 'backend',
      title: '维护',
      content: '今晚维护',
      readStatus: 0,
    })

    expect(api.markMessageRead).toHaveBeenCalledWith(8)
    expect(detail.data?.readStatus).toBe(1)
    expect(api.pageMessages).toHaveBeenCalledTimes(1)
    expect(dispatch).toHaveBeenCalled()
  })
})
