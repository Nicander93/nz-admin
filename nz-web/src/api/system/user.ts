import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export type SysUser = {
  id: number
  deptId?: number
  username: string
  password?: string
  nickname: string
  email: string
  phone: string
  status: number
  createTime?: string
  updateTime?: string
}

export interface UserQuery extends PageQuery {
  username?: string
  status?: number
}

export type UserInfo = {
  user: SysUser
  roles: string[]
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
}

export function pageUsers(params: UserQuery) {
  return request.get<PageResult<SysUser>>('/api/system/user/page', { params })
}

export function getUser(id: number) {
  return request.get<SysUser>(`/api/system/user/${id}`)
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

export function login(data: { username: string; password: string }) {
  return request.post<string>('/api/auth/login', data)
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
