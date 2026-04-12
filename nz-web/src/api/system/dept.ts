import request from '@/api/request'

export type SysDept = {
  id: number
  parentId: number
  name: string
  sort: number
  status: number
  createTime?: string
  children?: SysDept[]
}

export function listDepts() {
  return request.get<SysDept[]>('/api/system/dept/list')
}

export function getDept(id: number) {
  return request.get<SysDept>(`/api/system/dept/${id}`)
}

export function addDept(data: Partial<SysDept>) {
  return request.post<void>('/api/system/dept', data)
}

export function updateDept(data: Partial<SysDept>) {
  return request.put<void>('/api/system/dept', data)
}

export function deleteDept(id: number) {
  return request.delete<void>(`/api/system/dept/${id}`)
}
