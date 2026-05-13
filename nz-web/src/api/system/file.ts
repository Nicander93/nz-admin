import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export type SysFile = {
  id: number
  originalName: string
  fileName: string
  filePath: string
  fileUrl?: string
  fileSize: number
  fileExt: string
  mimeType?: string
  storageType?: string
  uploaderId?: number
  uploaderName?: string
  createTime?: string
  updateTime?: string
  createdAt?: string
}

export interface FileQuery extends PageQuery {
  originalName?: string
  fileExt?: string
}

/**
 * 上传单个文件
 * @param file File 对象
 * @param storageType 存储类型（可选，默认使用后端配置）
 */
export function uploadFile(file: File, storageType?: 'local' | 'oss') {
  const formData = new FormData()
  formData.append('file', file)
  if (storageType) {
    formData.append('storageType', storageType)
  }
  return request.post('/api/system/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 批量上传文件
 * @param files File 对象数组
 * @param storageType 存储类型（可选）
 */
export function uploadFiles(files: File[], storageType?: 'local' | 'oss') {
  const formData = new FormData()
  files.forEach((file) => {
    formData.append('files', file)
  })
  if (storageType) {
    formData.append('storageType', storageType)
  }
  return request.post('/api/system/file/uploads', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 分页查询文件列表
 * @param params 查询参数
 */
export function pageFiles(params: FileQuery) {
  return request.get<PageResult<SysFile>>('/api/system/file/page', { params })
}

export function listFiles(params: FileQuery) {
  return pageFiles(params)
}

/**
 * 删除文件
 * @param id 文件 ID
 */
export function deleteFile(id: number) {
  return request.delete(`/api/system/file/${id}`)
}

/**
 * 下载文件（直接打开 URL）
 * @param id 文件 ID
 */
export function downloadFileUrl(id: number) {
  return `/api/system/file/download/${id}`
}

/**
 * 获取当前存储类型
 */
export function getStorageType() {
  return request.get('/api/system/file/storage-type')
}
