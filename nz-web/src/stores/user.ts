import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo, getUserMenus } from '@/api/system/user'
import { getModuleStatuses } from '@/api/system/modules'
import type { CurrentTenant, SysUser, UserMenu } from '@/api/system/user'
import { getEnabledFrontendModuleCodes } from '@/core/modules/registry'
import type { BackendModuleStatus } from '@/core/modules/types'

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

  function setToken(val: string) {
    token.value = val
    localStorage.setItem('token', val)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    tenant.value = null
    roles.value = []
    permissions.value = []
    menus.value = []
    moduleStatuses.value = []
    enabledModuleCodes.value = new Set()
    routesLoaded.value = false
    localStorage.removeItem('token')
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = res.data.user
    tenant.value = res.data.tenant
    roles.value = res.data.roles
    permissions.value = res.data.permissions
  }

  async function fetchUserMenus() {
    const res = await getUserMenus()
    menus.value = res.data || []
  }

  async function fetchModuleStatuses() {
    const res = await getModuleStatuses()
    moduleStatuses.value = res.data || []
    enabledModuleCodes.value = getEnabledFrontendModuleCodes(
      moduleStatuses.value,
    )
  }

  async function initAuthData() {
    await Promise.all([
      fetchUserInfo(),
      fetchUserMenus(),
      fetchModuleStatuses(),
    ])
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
  }
})
