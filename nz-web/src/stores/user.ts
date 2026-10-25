import { defineStore } from 'pinia'
import { ref } from 'vue'

import { getModuleStatuses } from '@/api/system/modules'
import { getUserInfo, getUserMenus } from '@/api/system/user'
import type { CurrentTenant, SysUser, UserMenu } from '@/api/system/user'
import { getEnabledFrontendModuleCodes } from '@/core/modules/registry'
import type { BackendModuleStatus } from '@/core/modules/types'

let storageListenerInstalled = false

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<SysUser | null>(null)
  const tenant = ref<CurrentTenant | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const menus = ref<UserMenu[]>([])
  const moduleStatuses = ref<BackendModuleStatus[]>([])
  const enabledModuleCodes = ref<Set<string>>(new Set())
  const token = ref(localStorage.getItem('token') || '')
  const routesLoaded = ref(false)

  if (typeof window !== 'undefined' && !storageListenerInstalled) {
    storageListenerInstalled = true
    window.addEventListener('storage', (event) => {
      if (event.key !== 'token') return

      const currentStore = useUserStore()
      if (!event.newValue) {
        currentStore.token = ''
        currentStore.clearUserState()
        void import('@/router').then(({ default: router }) =>
          router.replace('/login'),
        )
        return
      }

      if (event.newValue === currentStore.token) return

      currentStore.token = event.newValue
      currentStore.clearUserState()
      window.location.reload()
    })
  }

  function setToken(val: string) {
    if (token.value !== val) clearUserState()
    token.value = val
    localStorage.setItem('token', val)
    if (localStorage.getItem('Warm-Authorization') !== null)
      localStorage.setItem('Warm-Authorization', val)
  }

  function clearUserState() {
    userInfo.value = null
    tenant.value = null
    roles.value = []
    permissions.value = []
    menus.value = []
    moduleStatuses.value = []
    enabledModuleCodes.value = new Set()
    routesLoaded.value = false
  }

  function logout() {
    token.value = ''
    clearUserState()
    localStorage.removeItem('token')
    localStorage.removeItem('Warm-Authorization')
  }

  function isCurrentToken(requestToken: string) {
    return token.value === requestToken && localStorage.getItem('token') === requestToken
  }

  async function fetchUserInfo() {
    const requestToken = token.value
    const res = await getUserInfo()
    if (!isCurrentToken(requestToken)) return

    userInfo.value = res.data.user
    tenant.value = res.data.tenant
    roles.value = res.data.roles
    permissions.value = res.data.permissions
  }

  async function fetchUserMenus() {
    const requestToken = token.value
    const res = await getUserMenus()
    if (!isCurrentToken(requestToken)) return

    menus.value = res.data || []
  }

  async function fetchModuleStatuses() {
    const requestToken = token.value
    const res = await getModuleStatuses()
    if (!isCurrentToken(requestToken)) return

    moduleStatuses.value = res.data || []
    enabledModuleCodes.value = getEnabledFrontendModuleCodes(
      moduleStatuses.value,
    )
  }

  async function initAuthData() {
    const requestToken = token.value
    if (!requestToken) return

    await Promise.all([
      fetchUserInfo(),
      fetchUserMenus(),
      fetchModuleStatuses(),
    ])
    return isCurrentToken(requestToken)
  }

  function setRoutesLoaded(val: boolean) {
    routesLoaded.value = val
  }

  function hasPermission(perm: string): boolean {
    return (
      permissions.value.includes('*:*:*') || permissions.value.includes(perm)
    )
  }

  return {
    userInfo,
    tenant,
    roles,
    permissions,
    menus,
    moduleStatuses,
    enabledModuleCodes,
    token,
    routesLoaded,
    setToken,
    logout,
    fetchUserInfo,
    fetchUserMenus,
    fetchModuleStatuses,
    initAuthData,
    setRoutesLoaded,
    hasPermission,
    clearUserState,
  }
})
