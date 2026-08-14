import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export type SysUser = {
  id: number
  tenantId?: number
  deptId?: number
  username: string
  password?: string
  nickname: string
  email?: string
  phone?: string
  emailMasked?: string
  phoneMasked?: string
  status: number
  gender?: '0' | '1' | '2'
  avatarFileId?: number
  postIds?: number[]
  createTime?: string
  updateTime?: string
}

export type CurrentTenant = {
  id: number
  tenantCode: string
  tenantName: string
  packageId: number
  expireTime?: string
  status: number
}

export interface UserQuery extends PageQuery {
  username?: string
  status?: number
  revealContacts?: boolean
}

export type UserInfo = {
  user: SysUser
  roles: string[]
  tenant: CurrentTenant
  permissions: string[]
}

export type UserMenu = {
  id: number
  name: string
  path: string
  component?: string
  parentId?: number | null
  meta?: {
    title?: string
    icon?: string
    [key: string]: unknown
  }
  children?: UserMenu[]
}

export function pageUsers(params: UserQuery) {
  return request.get<PageResult<SysUser>>('/api/system/user/page', { params })
}

export function getUser(id: number, revealContacts = false) {
  return request.get<SysUser>(`/api/system/user/${id}`, {
    params: { revealContacts },
  })
}

export function addUser(data: Partial<SysUser>) {
  return request.post<void>('/api/system/user', data)
}

export function updateUser(data: Partial<SysUser>) {
  return request.put<void>('/api/system/user', data)
}

export function deleteUser(id: number) {
  return request.delete<void>(`/api/system/user/${id}`)
}

export function getUserRoleIds(userId: number) {
  return request.get<number[]>(`/api/system/user/${userId}/roleIds`)
}

export function assignUserRoles(userId: number, roleIds: number[]) {
  return request.put<void>(`/api/system/user/${userId}/roles`, roleIds)
}

export function resetUserPassword(userId: number) {
  return request.put<void>(`/api/system/user/${userId}/password/reset`)
}

export function reEncryptUserContacts() {
  return request.put<number>('/api/system/user/contacts/re-encrypt')
}
export function login(data: {
  tenantCode: string
  clientId: string
  username: string
  password: string
}) {
  return request.post<string>('/api/auth/login', data)
}

export function sendSmsLoginCode(data: {
  tenantCode: string
  clientId: string
  phone: string
}) {
  return request.post<void>('/api/auth/sms/code', data)
}

export function smsLogin(data: {
  tenantCode: string
  clientId: string
  phone: string
  code: string
}) {
  return request.post<string>('/api/auth/sms/login', data)
}

export function logout() {
  return request.post<void>('/api/auth/logout')
}

export function getUserInfo() {
  return request.get<UserInfo>('/api/auth/info')
}

export function getUserMenus() {
  return request.get<UserMenu[]>('/api/auth/menus')
}
