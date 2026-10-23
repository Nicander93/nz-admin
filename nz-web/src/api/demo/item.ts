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

export interface DemoItemImportError {
  rowNum: number
  message: string
}

export interface DemoItemImportPreview {
  total: number
  validCount: number
  rows: Array<
    Pick<DemoItem, 'name' | 'category' | 'status' | 'sort' | 'remark'>
  >
  errors: DemoItemImportError[]
}

export function downloadDemoItemTemplate() {
  return request.getBlob('/api/demo/item/import/template')
}

export function previewDemoItemImport(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<DemoItemImportPreview>(
    '/api/demo/item/import/preview',
    form,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  )
}

export function importDemoItems(
  rows: DemoItemImportPreview['rows'],
  key: string,
) {
  return request.post<number>('/api/demo/item/import', rows, {
    headers: { 'Idempotency-Key': key },
  })
}

export function exportDemoItems(
  params: Pick<DemoItemQuery, 'name' | 'category' | 'status'>,
) {
  return request.getBlob('/api/demo/item/export', { params })
}
