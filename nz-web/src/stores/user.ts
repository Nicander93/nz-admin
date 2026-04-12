import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo } from '@/api/system/user'
import type { SysUser } from '@/api/system/user'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<SysUser | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])
  const token = ref(localStorage.getItem('token') || '')

  function setToken(val: string) {
    token.value = val
    localStorage.setItem('token', val)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    localStorage.removeItem('token')
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = res.data.user
    roles.value = res.data.roles
    permissions.value = res.data.permissions
  }

  function hasPermission(perm: string): boolean {
    return permissions.value.includes('*:*:*') || permissions.value.includes(perm)
  }

  return { userInfo, roles, permissions, token, setToken, logout, fetchUserInfo, hasPermission }
})
