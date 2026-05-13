import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export type SysRole = {
  id: number
  name: string
  roleKey: string
  sort: number
  status: number
  remark?: string
  dataScope?: number
  createTime?: string
}

export interface RoleQuery extends PageQuery {
  name?: string
  roleKey?: string
  status?: number
  dataScope?: number
}

export function pageRoles(params: RoleQuery) {
  return request.get<PageResult<SysRole>>('/api/system/role/page', { params })
}

export function listAllRoles() {
  return request.get<SysRole[]>('/api/system/role/listAll')
}

export function getRole(id: number) {
  return request.get<SysRole>(`/api/system/role/${id}`)
}

export function addRole(data: Partial<SysRole>) {
  return request.post<void>('/api/system/role', data)
}

export function updateRole(data: Partial<SysRole>) {
  return request.put<void>('/api/system/role', data)
}

export function deleteRole(id: number) {
  return request.delete<void>(`/api/system/role/${id}`)
}

export function getRoleMenuIds(roleId: number) {
  return request.get<number[]>(`/api/system/role/${roleId}/menuIds`)
}

export function assignRoleMenus(roleId: number, menuIds: number[]) {
  return request.put<void>(`/api/system/role/${roleId}/menus`, menuIds)
}
