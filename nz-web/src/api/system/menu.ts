import request from '@/api/request'

export type SysMenu = {
  id: number
  parentId: number
  name: string
  path?: string
  component?: string
  icon?: string
  sort: number
  type: string
  perm?: string
  visible: number
  status: number
  createTime?: string
  children?: SysMenu[]
}

export function listMenus() {
  return request.get<SysMenu[]>('/api/system/menu/list')
}

export function getMenu(id: number) {
  return request.get<SysMenu>(`/api/system/menu/${id}`)
}

export function addMenu(data: Partial<SysMenu>) {
  return request.post<void>('/api/system/menu', data)
}

export function updateMenu(data: Partial<SysMenu>) {
  return request.put<void>('/api/system/menu', data)
}

export function deleteMenu(id: number) {
  return request.delete<void>(`/api/system/menu/${id}`)
}
