import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import router from '@/router'

const mocks = vi.hoisted(() => ({
  auth: {
    token: '',
    routesLoaded: false,
    userInfo: { id: 1 },
    menus: [{ id: 1 }],
    enabledModuleCodes: new Set<string>(),
    initAuthData: vi.fn(),
    fetchUserMenus: vi.fn(),
    setRoutesLoaded: vi.fn(),
    setToken: vi.fn(),
    clearUserState: vi.fn(),
    logout: vi.fn(),
  },
  build: vi.fn(),
}))

vi.mock('@/stores/user', () => ({ useUserStore: () => mocks.auth }))
vi.mock('@/utils/routeHelper', () => ({
  buildDynamicChildrenRoutes: mocks.build,
  resolveFirstRoutePath: () => '/profile',
}))
vi.mock('vue-router', async (original) => {
  const actual = await original<typeof import('vue-router')>()
  return { ...actual, createWebHistory: actual.createMemoryHistory }
})
vi.mock('@/layout/index.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/profile/index.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/login/index.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/auth-unavailable/index.vue', () => ({ default: { template: '<div />' } }))

beforeEach(async () => {
  localStorage.clear()
  mocks.auth.token = ''
  mocks.auth.routesLoaded = false
  mocks.auth.userInfo = { id: 1 }
  mocks.auth.menus = [{ id: 1 }]
  mocks.auth.enabledModuleCodes = new Set()
  mocks.auth.initAuthData.mockReset().mockResolvedValue(true)
  mocks.auth.fetchUserMenus.mockReset().mockResolvedValue(undefined)
  mocks.auth.setRoutesLoaded.mockReset().mockImplementation((loaded: boolean) => {
    mocks.auth.routesLoaded = loaded
  })
  mocks.auth.setToken.mockReset().mockImplementation((token: string) => {
    mocks.auth.token = token
    mocks.auth.routesLoaded = false
    mocks.auth.userInfo = null
    mocks.auth.menus = []
  })
  mocks.auth.clearUserState.mockReset().mockImplementation(() => {
    mocks.auth.routesLoaded = false
    mocks.auth.userInfo = null
    mocks.auth.menus = []
  })
  mocks.auth.logout.mockReset()
  mocks.build.mockReset().mockReturnValue([])
  await router.replace('/login')
})

afterEach(async () => {
  localStorage.clear()
  await router.replace('/login')
})

describe('session route lifecycle', () => {
  it('removes the previous identities routes before loading the next account', async () => {
    mocks.auth.setRoutesLoaded.mockImplementation((loaded: boolean) => {
      mocks.auth.routesLoaded = loaded
    })
    mocks.build.mockReturnValue([
      {
        path: '/account-a',
        name: 'AccountA',
        component: { template: '<div />' },
      },
    ])
    localStorage.setItem('token', 'account-a')
    await router.push('/profile')
    expect(router.hasRoute('AccountA')).toBe(true)

    localStorage.removeItem('token')
    mocks.auth.routesLoaded = false
    await router.push('/login')
    expect(router.hasRoute('AccountA')).toBe(false)
    expect(router.hasRoute('Profile')).toBe(true)

    mocks.build.mockReturnValue([
      {
        path: '/account-b',
        name: 'AccountB',
        component: { template: '<div />' },
      },
    ])
    localStorage.setItem('token', 'account-b')
    await router.push('/profile')
    expect(router.hasRoute('AccountB')).toBe(true)
    expect(router.hasRoute('AccountA')).toBe(false)

    localStorage.removeItem('token')
    mocks.auth.routesLoaded = false
    await router.push('/login')
  })

  it('preserves the token on an auth data outage and retries initialization', async () => {
    mocks.auth.userInfo = null
    mocks.auth.menus = []
    mocks.auth.initAuthData
      .mockRejectedValueOnce(new Error('503 unavailable'))
      .mockImplementationOnce(async () => {
        mocks.auth.userInfo = { id: 2 }
        mocks.auth.menus = [{ id: 2 }]
        return true
      })
    localStorage.setItem('token', 'recovery-token')

    await router.push('/profile?first-load')

    expect(router.currentRoute.value.path).toBe('/auth-unavailable')
    expect(localStorage.getItem('token')).toBe('recovery-token')
    expect(mocks.auth.logout).not.toHaveBeenCalled()
    expect(mocks.auth.clearUserState).toHaveBeenCalledOnce()

    await router.replace('/')

    expect(router.currentRoute.value.path).toBe('/profile')
    expect(localStorage.getItem('token')).toBe('recovery-token')
    expect(mocks.auth.routesLoaded).toBe(true)
    expect(mocks.auth.initAuthData).toHaveBeenCalledTimes(2)
  })

  it('does not install routes from a successful stale identity initialization', async () => {
    mocks.auth.userInfo = null
    mocks.auth.menus = []
    let resolveOldInitialization!: (value: boolean) => void
    mocks.auth.initAuthData
      .mockImplementationOnce(() => new Promise((resolve) => {
        resolveOldInitialization = resolve
      }))
      .mockImplementationOnce(async () => {
        mocks.auth.userInfo = { id: 2 }
        mocks.auth.menus = [{ id: 2 }]
        return true
      })
    mocks.build.mockReturnValue([{
      path: '/account-b',
      name: 'AccountB',
      component: { template: '<div />' },
    }])
    localStorage.setItem('token', 'account-a')

    const oldNavigation = router.push('/profile?account-a')
    await vi.waitFor(() => expect(mocks.auth.initAuthData).toHaveBeenCalledOnce())
    localStorage.setItem('token', 'account-b')
    mocks.auth.token = 'account-b'
    resolveOldInitialization(true)
    await oldNavigation

    expect(mocks.auth.initAuthData).toHaveBeenCalledTimes(2)
    expect(mocks.build).toHaveBeenCalledOnce()
    expect(router.hasRoute('AccountB')).toBe(true)
    expect(mocks.auth.routesLoaded).toBe(true)
    expect(mocks.auth.logout).not.toHaveBeenCalled()
  })

  it('does not let a stale initialization failure log out the replacement identity', async () => {
    mocks.auth.userInfo = null
    mocks.auth.menus = []
    let rejectOldInitialization!: (error: Error) => void
    mocks.auth.initAuthData
      .mockImplementationOnce(() => new Promise((_resolve, reject) => {
        rejectOldInitialization = reject
      }))
      .mockImplementationOnce(async () => {
        mocks.auth.userInfo = { id: 2 }
        mocks.auth.menus = [{ id: 2 }]
        return true
      })
    localStorage.setItem('token', 'account-a')

    const oldNavigation = router.push('/profile?failed-account-a')
    await vi.waitFor(() => expect(mocks.auth.initAuthData).toHaveBeenCalledOnce())
    localStorage.setItem('token', 'account-b')
    mocks.auth.token = 'account-b'
    rejectOldInitialization(new Error('late 503'))
    await oldNavigation

    expect(mocks.auth.logout).not.toHaveBeenCalled()
    expect(mocks.auth.token).toBe('account-b')
    expect(localStorage.getItem('token')).toBe('account-b')
    expect(mocks.auth.initAuthData).toHaveBeenCalledTimes(2)
  })
})
