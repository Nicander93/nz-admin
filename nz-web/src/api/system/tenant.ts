import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export type SysTenantPackage = {
  id: number
  packageName: string
  status: number
  remark?: string
  menuIds?: number[]
  createTime?: string
  updateTime?: string
}

export type SysTenant = {
  id: number
  tenantCode: string
  tenantName: string
  contactUser?: string
  contactPhone?: string
  packageId: number
  expireTime?: string
  accountCount: number
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export type TenantForm = Partial<SysTenant> & {
  adminUsername?: string
  adminPassword?: string
}

export interface TenantQuery extends PageQuery {
  tenantCode?: string
  tenantName?: string
  status?: number
}

export interface TenantPackageQuery extends PageQuery {
  packageName?: string
  status?: number
}

export function pageTenants(params: TenantQuery) {
  return request.get<PageResult<SysTenant>>('/api/system/tenant/page', {
    params,
  })
}

export function getTenant(id: number) {
  return request.get<SysTenant>(`/api/system/tenant/${id}`)
}

export function addTenant(data: TenantForm) {
  return request.post<number>('/api/system/tenant', data)
}

export function updateTenant(data: TenantForm) {
  return request.put<void>('/api/system/tenant', data)
}

export function deactivateTenant(id: number) {
  return request.delete<void>(`/api/system/tenant/${id}`)
}

export function pageTenantPackages(params: TenantPackageQuery) {
  return request.get<PageResult<SysTenantPackage>>(
    '/api/system/tenant-package/page',
    { params },
  )
}

export function listTenantPackages() {
  return request.get<SysTenantPackage[]>('/api/system/tenant-package/list-all')
}

export function getTenantPackage(id: number) {
  return request.get<SysTenantPackage>(`/api/system/tenant-package/${id}`)
}

export function addTenantPackage(data: Partial<SysTenantPackage>) {
  return request.post<number>('/api/system/tenant-package', data)
}

export function updateTenantPackage(data: Partial<SysTenantPackage>) {
  return request.put<void>('/api/system/tenant-package', data)
}

export function deleteTenantPackage(id: number) {
  return request.delete<void>(`/api/system/tenant-package/${id}`)
}
