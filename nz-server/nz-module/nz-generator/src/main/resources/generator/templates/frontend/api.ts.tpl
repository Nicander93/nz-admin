import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export interface @@CLASS@@ {
@@TS_FIELDS@@
}

export interface @@CLASS@@Query extends PageQuery {
@@TS_QUERY_FIELDS@@
}

export type @@CLASS@@Form = Partial<@@CLASS@@>

export function page@@CLASS@@s(params: @@CLASS@@Query) {
  return request.get<PageResult<@@CLASS@@>>('/api/@@MODULE@@/@@BUSINESS@@/page', { params })
}

export function get@@CLASS@@(@@PK_FIELD@@: @@CLASS@@['@@PK_FIELD@@']) {
  return request.get<@@CLASS@@>('/api/@@MODULE@@/@@BUSINESS@@/' + @@PK_FIELD@@)
}

export function add@@CLASS@@(data: @@CLASS@@Form) {
  return request.post<@@CLASS@@['@@PK_FIELD@@']>('/api/@@MODULE@@/@@BUSINESS@@', data)
}

export function update@@CLASS@@(data: @@CLASS@@Form) {
  return request.put<void>('/api/@@MODULE@@/@@BUSINESS@@', data)
}

export function delete@@CLASS@@(@@PK_FIELD@@: @@CLASS@@['@@PK_FIELD@@']) {
  return request.delete<void>('/api/@@MODULE@@/@@BUSINESS@@/' + @@PK_FIELD@@)
}
