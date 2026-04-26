import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo, getUserMenus } from '@/api/system/user'
import type { SysUser, UserMenu } from '@/api/system/user'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<SysUser | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const menus = ref<UserMenu[]>([])
  const token = ref(localStorage.getItem('token') || '')
  const routesLoaded = ref(false)

  function setToken(val: string) {
    token.value = val
    localStorage.setItem('token', val)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    menus.value = []
    routesLoaded.value = false
    localStorage.removeItem('token')
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = res.data.user
    roles.value = res.data.roles
    permissions.value = res.data.permissions
  }

  async function fetchUserMenus() {
    const res = await getUserMenus()
    menus.value = res.data || []
  }

  async function initAuthData() {
    await Promise.all([fetchUserInfo(), fetchUserMenus()])
  }

  function setRoutesLoaded(val: boolean) {
    routesLoaded.value = val
  }

  function hasPermission(perm: string): boolean {
    return permissions.value.includes('*:*:*') || permissions.value.includes(perm)
  }

  return {
    userInfo,
    roles,
    permissions,
    menus,
    token,
    routesLoaded,
    setToken,
    logout,
    fetchUserInfo,
    fetchUserMenus,
    initAuthData,
    setRoutesLoaded,
    hasPermission,
  }
})
