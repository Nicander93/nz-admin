import request from '@/api/request'
import type { PageResult } from '@/api/types'

export interface SysFileConfig {
  id: number
  configName: string
  storageType: 'local' | 'oss' | 's3'
  basePath?: string
  endpoint?: string
  accessKeyIdMasked?: string
  accessKeySecretConfigured: boolean
  bucketName?: string
  region?: string
  domain?: string
  pathPrefix?: string
  localAccessUrlPrefix?: string
  maxFileSizeBytes: number
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface FileConfigForm {
  id?: number
  configName: string
  storageType: 'local' | 'oss' | 's3'
  basePath?: string
  endpoint?: string
  accessKeyId?: string
  accessKeySecret?: string
  bucketName?: string
  region?: string
  domain?: string
  pathPrefix?: string
  localAccessUrlPrefix?: string
  maxFileSizeBytes: number
  remark?: string
}

export interface FileConfigQuery {
  pageNum: number
  pageSize: number
  configName?: string
  storageType?: string
  status?: number
}

export function pageFileConfigs(params: FileConfigQuery) {
  return request.get<PageResult<SysFileConfig>>(
    '/api/system/file-config/page',
    { params },
  )
}

export function addFileConfig(data: FileConfigForm) {
  return request.post<number>('/api/system/file-config', data)
}

export function updateFileConfig(data: FileConfigForm) {
  return request.put<void>('/api/system/file-config', data)
}

export function deleteFileConfig(id: number) {
  return request.delete<void>(`/api/system/file-config/${id}`)
}

export function activateFileConfig(id: number) {
  return request.put<void>(`/api/system/file-config/${id}/activate`)
}

export function testFileConfig(id: number) {
  return request.post<void>(`/api/system/file-config/${id}/test`)
}
