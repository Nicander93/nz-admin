import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useUserStore } from '@/stores/user'

const apiMocks = vi.hoisted(() => ({
  getUserInfo: vi.fn(),
  getUserMenus: vi.fn(),
  getModuleStatuses: vi.fn(),
}))

vi.mock('@/api/system/user', () => ({
  getUserInfo: apiMocks.getUserInfo,
  getUserMenus: apiMocks.getUserMenus,
}))

vi.mock('@/router', () => ({
  default: { replace: vi.fn(), currentRoute: { value: { fullPath: '/profile' } } },
}))

vi.mock('@/api/system/modules', () => ({
  getModuleStatuses: apiMocks.getModuleStatuses,
}))

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    apiMocks.getUserInfo.mockResolvedValue({
      data: {
        user: { id: 1, username: 'admin', nickname: '管理员', status: 0 },
        tenant: null,
        roles: ['admin'],
        permissions: ['system:user:list', 'system:user:add'],
      },
    })
    apiMocks.getUserMenus.mockResolvedValue({
      data: [
        { id: 1, name: '系统管理', path: '/system', parentId: 0 },
        { id: 2, name: '用户管理', path: '/user', parentId: 1 },
      ],
    })
    apiMocks.getModuleStatuses.mockResolvedValue({
      data: [{ code: 'system', name: 'system', version: '1', frontendModule: 'system', state: 'ENABLED' }],
    })
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
    expect(store.enabledModuleCodes).toEqual(new Set(['system']))
  })


  it('ignores late identity responses after the token changes', async () => {
    const store = useUserStore()
    let resolveOldResponse!: (response: unknown) => void
    const oldResponse = new Promise<unknown>((resolve) => {
      resolveOldResponse = resolve
    })
    apiMocks.getUserInfo.mockReturnValueOnce(oldResponse)

    store.setToken('session-a')
    const oldInitialization = store.initAuthData()
    store.setToken('session-b')
    apiMocks.getUserInfo.mockResolvedValueOnce({
      data: {
        user: { id: 2, username: 'user-b', nickname: 'User B', status: 0 },
        tenant: null,
        roles: ['role-b'],
        permissions: ['system:profile:view'],
      },
    })
    apiMocks.getUserMenus.mockResolvedValueOnce({
      data: [{ id: 2, name: 'B menu', path: '/b', parentId: 0 }],
    })
    apiMocks.getModuleStatuses.mockResolvedValueOnce({
      data: [{ code: 'module-b', name: 'Module B', version: '1', frontendModule: 'module-b', state: 'ENABLED' }],
    })

    await expect(store.initAuthData()).resolves.toBe(true)
    resolveOldResponse({
      data: {
        user: { id: 1, username: 'user-a', nickname: 'User A', status: 0 },
        tenant: null,
        roles: ['role-a'],
        permissions: ['system:user:remove'],
      },
    })
    await expect(oldInitialization).resolves.toBe(false)

    expect(store.userInfo?.username).toBe('user-b')
    expect(store.roles).toEqual(['role-b'])
    expect(store.permissions).toEqual(['system:profile:view'])
    expect(store.menus).toEqual([{ id: 2, name: 'B menu', path: '/b', parentId: 0 }])
    expect(store.moduleStatuses.map((module) => module.code)).toEqual(['module-b'])
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
    expect(store.enabledModuleCodes).toEqual(new Set())
    expect(store.routesLoaded).toBe(false)
    expect(localStorage.getItem('token')).toBeNull()
  })
})


describe('useUserStore cross-tab session sync', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('clears permissions when another tab removes the token', async () => {
    const store = useUserStore()
    store.setToken('tab-token')
    store.permissions = ['system:user:list']
    store.setRoutesLoaded(true)
    localStorage.removeItem('token')

    window.dispatchEvent(new StorageEvent('storage', {
      key: 'token',
      oldValue: 'tab-token',
      newValue: null,
    }))
    await Promise.resolve()

    expect(store.token).toBe('')
    expect(store.permissions).toEqual([])
    expect(store.routesLoaded).toBe(false)
  })

  it('keeps the replacement token and reloads when another tab logs in', () => {
    const store = useUserStore()
    store.setToken('old-token')
    store.permissions = ['system:user:list']
    store.setRoutesLoaded(true)
    localStorage.setItem('token', 'new-token')

    const reload = vi.spyOn(window.location, 'reload').mockImplementation(() => {})
    window.dispatchEvent(new StorageEvent('storage', {
      key: 'token',
      oldValue: 'old-token',
      newValue: 'new-token',
    }))

    expect(reload).toHaveBeenCalledOnce()
    expect(store.token).toBe('new-token')
    expect(localStorage.getItem('token')).toBe('new-token')
    expect(store.permissions).toEqual([])
    expect(store.routesLoaded).toBe(false)
  })
})
