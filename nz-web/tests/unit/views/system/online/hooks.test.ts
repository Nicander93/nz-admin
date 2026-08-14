import { beforeEach, describe, expect, it, vi } from 'vitest'

const { listOnlineUsers, forceLogout, success } = vi.hoisted(() => ({
  listOnlineUsers: vi.fn(),
  forceLogout: vi.fn(),
  success: vi.fn(),
}))

vi.mock('@/api/system/online', () => ({ listOnlineUsers, forceLogout }))
vi.mock('element-plus', () => ({ ElMessage: { success } }))

import { useOnlineUsers } from '@/views/system/online/hooks'

describe('useOnlineUsers', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    listOnlineUsers.mockResolvedValue({
      data: [
        {
          tokenValue: 'token-a',
          userId: 10,
          tenantId: 1,
          tenantCode: 'default',
          username: 'admin',
          loginIp: '10.0.0.1',
          tokenTimeout: 3660,
        },
      ],
    })
    forceLogout.mockResolvedValue({ code: 200 })
  })

  it('loads online users with search conditions', async () => {
    const { table } = useOnlineUsers()
    table.query.username = 'admin'

    await table.load()

    expect(listOnlineUsers).toHaveBeenCalledWith({ username: 'admin', loginIp: '' })
    expect(table.rows).toHaveLength(1)
    expect(table.formatTimeout(3660)).toBe('1 小时 1 分')
  })

  it('forces logout and refreshes the list', async () => {
    const { table, actions } = useOnlineUsers()

    await actions.forceLogout('token-a')

    expect(forceLogout).toHaveBeenCalledWith('token-a')
    expect(success).toHaveBeenCalledWith('用户已强制退出')
    expect(listOnlineUsers).toHaveBeenCalledTimes(1)
    expect(table.rows[0].username).toBe('admin')
  })
})
