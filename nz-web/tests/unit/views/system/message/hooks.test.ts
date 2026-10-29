import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  useMessageCenter,
  validateMessageSendForm,
} from '@/views/system/message/hooks'

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
  hasPermission: vi.fn(),
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
vi.mock('@/stores/user', () => ({ useUserStore: () => ({ hasPermission: api.hasPermission }) }))
vi.mock('element-plus', () => ({
  ElMessage: { success: api.success, warning: api.warning },
  ElMessageBox: { confirm: api.confirm },
}))

describe('message center hooks', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.hasPermission.mockImplementation((permission: string) => permission === 'system:message:read')
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

    expect(api.markMessageRead).toHaveBeenCalledWith(8, expect.objectContaining({ signal: expect.any(AbortSignal) }))
    expect(detail.data?.readStatus).toBe(1)
    expect(api.pageMessages).toHaveBeenCalledTimes(1)
    expect(dispatch).toHaveBeenCalled()
  })
  it('shows unread detail without marking it read when the user lacks read permission', async () => {
    api.hasPermission.mockReturnValue(false)
    api.getMessage.mockResolvedValue({
      data: {
        id: 9,
        category: 'notice',
        type: 'message',
        source: 'backend',
        title: '只读通知',
        content: '仅查看',
        readStatus: 0,
      },
    })
    const { actions, detail } = useMessageCenter()

    await actions.openDetail({ id: 9 } as never)

    expect(detail.visible).toBe(true)
    expect(detail.data?.title).toBe('只读通知')
    expect(detail.data?.readStatus).toBe(0)
    expect(api.markMessageRead).not.toHaveBeenCalled()
  })

  it('keeps the newest message detail when detail requests resolve out of order', async () => {
    let resolveFirst!: (value: unknown) => void
    let resolveSecond!: (value: unknown) => void
    api.getMessage
      .mockReturnValueOnce(new Promise((resolve) => { resolveFirst = resolve }))
      .mockReturnValueOnce(new Promise((resolve) => { resolveSecond = resolve }))
    const { actions, detail } = useMessageCenter()
    const first = actions.openDetail({ id: 1 } as never)
    const second = actions.openDetail({ id: 2 } as never)
    resolveSecond({ data: { id: 2, readStatus: 1, title: '新详情' } })
    await second
    resolveFirst({ data: { id: 1, readStatus: 1, title: '旧详情' } })
    await first

    expect(detail.data?.id).toBe(2)
    expect(detail.data?.title).toBe('新详情')
  })

  it('ignores repeated send actions while a request is already pending', async () => {
    let resolveSend!: (value: unknown) => void
    api.sendMessage.mockReturnValue(new Promise((resolve) => { resolveSend = resolve }))
    const { actions, sendDialog, sendForm } = useMessageCenter()
    sendForm.title = '通知'
    sendForm.content = '内容'

    const first = actions.submitSend()
    const second = actions.submitSend()
    expect(api.sendMessage).toHaveBeenCalledTimes(1)
    resolveSend({ data: 1 })
    await Promise.all([first, second])
    expect(api.sendMessage).toHaveBeenCalledTimes(1)
    expect(sendDialog.loading).toBe(false)
  })

  it('does not apply an inbox response after the identity token changes', async () => {
    localStorage.setItem('token', 'first-user')
    let resolvePage!: (value: unknown) => void
    api.pageMessages.mockReturnValueOnce(new Promise((resolve) => { resolvePage = resolve }))
    const { table } = useMessageCenter()
    const pending = table.load()
    localStorage.setItem('token', 'second-user')
    resolvePage({ data: { records: [{ id: 1 }], total: 1 } })
    await pending

    expect(table.rows).toEqual([])
    expect(table.total).toBe(0)
  })

})
