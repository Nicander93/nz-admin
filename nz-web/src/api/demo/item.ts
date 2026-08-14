import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export type DemoItem = {
  id: number
  name: string
  category: string
  status: number
  sort: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface DemoItemQuery extends PageQuery {
  name?: string
  category?: string
  status?: number
}

export function pageDemoItems(params: DemoItemQuery) {
  return request.get<PageResult<DemoItem>>('/api/demo/item/page', { params })
}

export function getDemoItem(id: number) {
  return request.get<DemoItem>('/api/demo/item/' + id)
}

export function addDemoItem(data: Partial<DemoItem>) {
  return request.post<number>('/api/demo/item', data)
}

export function updateDemoItem(data: Partial<DemoItem>) {
  return request.put<void>('/api/demo/item', data)
}

export function deleteDemoItem(id: number) {
  return request.delete<void>('/api/demo/item/' + id)
}
