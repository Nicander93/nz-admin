import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'

vi.mock('@/api/system/user', () => ({
  getUserInfo: vi.fn().mockResolvedValue({
    data: {
      user: {
        id: 1,
        username: 'admin',
        nickname: '管理员',
        email: 'admin@test.com',
        phone: '13800000000',
        status: 0,
      },
      roles: ['admin'],
      permissions: ['system:user:list', 'system:user:add'],
    },
  }),
  getUserMenus: vi.fn().mockResolvedValue({
    data: [
      { id: 1, name: '系统管理', path: '/system', parentId: 0 },
      { id: 2, name: '用户管理', path: 'user', parentId: 1, component: '@/views/system/user/index.vue' },
    ],
  }),
}))

import { useUserStore } from '@/stores/user'

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('initAuthData 能正确设置 token、用户信息、菜单、权限', async () => {
    const store = useUserStore()

    store.setToken('token-123')
    await store.initAuthData()

    expect(store.token).toBe('token-123')
    expect(localStorage.getItem('token')).toBe('token-123')
    expect(store.userInfo?.username).toBe('admin')
    expect(store.roles).toEqual(['admin'])
    expect(store.permissions).toEqual(['system:user:list', 'system:user:add'])
    expect(store.menus).toHaveLength(2)
  })

  it('logout 清除所有状态', async () => {
    const store = useUserStore()

    store.setToken('token-123')
    await store.initAuthData()
    store.setRoutesLoaded(true)

    store.logout()

    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.roles).toEqual([])
    expect(store.permissions).toEqual([])
    expect(store.menus).toEqual([])
    expect(store.routesLoaded).toBe(false)
    expect(localStorage.getItem('token')).toBeNull()
  })
})
