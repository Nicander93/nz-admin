import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export type SysDictType = {
  id: number
  name: string
  type: string
  status: number
  remark?: string
  createTime?: string
}

export type SysDictData = {
  id: number
  dictType: string
  label: string
  value: string
  sort: number
  status: number
  remark?: string
  createTime?: string
}

export interface DictTypeQuery extends PageQuery {
  name?: string
  type?: string
  status?: number
}

export function pageDictTypes(params: DictTypeQuery) {
  return request.get<PageResult<SysDictType>>('/api/system/dict/type/page', { params })
}

export function getDictType(id: number) {
  return request.get<SysDictType>(`/api/system/dict/type/${id}`)
}

export function addDictType(data: Partial<SysDictType>) {
  return request.post<void>('/api/system/dict/type', data)
}

export function updateDictType(data: Partial<SysDictType>) {
  return request.put<void>('/api/system/dict/type', data)
}

export function deleteDictType(id: number) {
  return request.delete<void>(`/api/system/dict/type/${id}`)
}

export function listDictDataByType(dictType: string) {
  return request.get<SysDictData[]>(`/api/system/dict/data/type/${dictType}`)
}

export function getDictData(id: number) {
  return request.get<SysDictData>(`/api/system/dict/data/${id}`)
}

export function addDictData(data: Partial<SysDictData>) {
  return request.post<void>('/api/system/dict/data', data)
}

export function updateDictData(data: Partial<SysDictData>) {
  return request.put<void>('/api/system/dict/data', data)
}

export function deleteDictData(id: number) {
  return request.delete<void>(`/api/system/dict/data/${id}`)
}
