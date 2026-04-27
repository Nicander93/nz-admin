import request from '@/utils/request'

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
  files.forEach((file, index) => {
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
export function listFiles(params: {
  current?: number
  size?: number
  originalName?: string
  fileExt?: string
}) {
  return request.get('/api/system/file/page', { params })
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
