import request from '@/api/request'
import type { PageQuery, PageResult, R } from '@/api/types'

import type { AxiosResponse } from 'axios'

export type FileStorageType = 'local' | 'oss' | 's3'
export type FileLifecycleStatus = 'READY' | 'DELETE_PENDING'

export type SysFile = {
  id: number
  originalName: string
  fileName: string
  filePath: string
  fileUrl?: string
  fileSize: number
  fileExt: string
  mimeType?: string
  storageType?: FileStorageType
  storageBindingRequired: boolean
  lifecycleStatus: FileLifecycleStatus
  cleanupError?: string
  cleanupAttempts?: number
  uploaderId?: number
  uploaderName?: string
  createTime?: string
  updateTime?: string
}

export interface FailedFileUpload {
  /** Zero-based position in the submitted batch. */
  index: number
  fileName: string
  reason: string
}

export interface BatchUploadResult {
  totalCount: number
  successCount: number
  failCount: number
  successFiles: SysFile[]
  failedFiles: FailedFileUpload[]
}

export interface FileQuery extends PageQuery {
  originalName?: string
  fileExt?: string
}

export function uploadFile(file: File): Promise<R<SysFile>> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<SysFile>('/api/system/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function uploadFiles(files: File[]): Promise<R<BatchUploadResult>> {
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  return request.post<BatchUploadResult>('/api/system/file/uploads', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function pageFiles(params: FileQuery): Promise<R<PageResult<SysFile>>> {
  return request.get<PageResult<SysFile>>('/api/system/file/page', { params })
}

export function listFiles(params: FileQuery): Promise<R<PageResult<SysFile>>> {
  return pageFiles(params)
}

export function deleteFile(id: number): Promise<R<void>> {
  return request.delete<void>('/api/system/file/' + id)
}

export function bindFileStorage(id: number, configId: number): Promise<R<void>> {
  return request.put<void>(
    '/api/system/file/' + id + '/storage-binding/' + configId,
  )
}

export function downloadFile(id: number): Promise<AxiosResponse<Blob>> {
  return request.getBlob<Blob>('/api/system/file/download/' + id)
}

export function getStorageType(): Promise<R<FileStorageType>> {
  return request.get<FileStorageType>('/api/system/file/storage-type')
}
