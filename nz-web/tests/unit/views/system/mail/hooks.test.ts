import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { useMailTest } from '@/views/system/mail/hooks'
import { sendTestMail } from '@/api/system/mail'

vi.mock('@/api/system/mail', () => ({ sendTestMail: vi.fn() }))
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn() } }))

describe('useMailTest', () => {
  beforeEach(() => vi.clearAllMocks())

  it('发送表单并在完成后恢复 loading', async () => {
    let resolveRequest: () => void = () => undefined
    vi.mocked(sendTestMail).mockReturnValue(new Promise<void>((resolve) => { resolveRequest = resolve }))
    const mail = useMailTest()
    mail.form.to = 'user@example.com'

    const pending = mail.send()
    await nextTick()
    expect(mail.loading.value).toBe(true)
    expect(sendTestMail).toHaveBeenCalledWith(expect.objectContaining({ to: 'user@example.com', html: false }))

    resolveRequest()
    await pending
    expect(mail.loading.value).toBe(false)
  })

  it('发送失败后也恢复 loading', async () => {
    vi.mocked(sendTestMail).mockRejectedValue(new Error('smtp unavailable'))
    const mail = useMailTest()
    await expect(mail.send()).rejects.toThrow('smtp unavailable')
    expect(mail.loading.value).toBe(false)
  })
})