import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useFileCrud, useFileUploadDialog } from '@/views/system/file/hooks'

const api = vi.hoisted(() => ({
  pageFiles: vi.fn(),
  deleteFile: vi.fn(),
  downloadFile: vi.fn(),
  uploadFile: vi.fn(),
  uploadFiles: vi.fn(),
  bindFileStorage: vi.fn(),
  pageFileConfigs: vi.fn(),
  success: vi.fn(),
  warning: vi.fn(),
  error: vi.fn(),
  confirm: vi.fn(),
}))

vi.mock('@/api/system/file', () => ({
  pageFiles: api.pageFiles,
  deleteFile: api.deleteFile,
  downloadFile: api.downloadFile,
  uploadFile: api.uploadFile,
  uploadFiles: api.uploadFiles,
  bindFileStorage: api.bindFileStorage,
}))
vi.mock('@/api/system/fileConfig', () => ({
  pageFileConfigs: api.pageFileConfigs,
}))
vi.mock('element-plus', () => ({
  ElMessage: {
    success: api.success,
    warning: api.warning,
    error: api.error,
  },
  ElMessageBox: { confirm: api.confirm },
}))

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((done) => {
    resolve = done
  })
  return { promise, resolve }
}

describe('file upload dialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.pageFiles.mockResolvedValue({
      data: { records: [], total: 0, current: 1, size: 10 },
    })
    api.uploadFile.mockResolvedValue({ data: { id: 1 } })
    api.uploadFiles.mockResolvedValue({
      data: {
        totalCount: 0,
        successCount: 0,
        failCount: 0,
        successFiles: [],
        failedFiles: [],
      },
    })
  })

  it('keeps only partial failures with their server reasons for a safe retry', async () => {
    const onUploaded = vi.fn()
    const upload = useFileUploadDialog(onUploaded)
    const files = [
      new File(['a'], 'a.txt', { lastModified: 1 }),
      new File(['b'], 'b.txt', { lastModified: 2 }),
      new File(['c'], 'c.txt', { lastModified: 3 }),
    ]
    api.uploadFiles.mockResolvedValue({
      data: {
        totalCount: 3,
        successCount: 2,
        failCount: 1,
        successFiles: [{ id: 1 }, { id: 3 }],
        failedFiles: [{ index: 1, fileName: 'b.txt', reason: '文件类型不支持' }],
      },
    })

    upload.open('batch')
    upload.selectFiles(files)
    await upload.submit()

    expect(api.uploadFiles).toHaveBeenCalledWith(files)
    expect(upload.files.value).toEqual([files[1]])
    expect(upload.failedFiles.value).toEqual([
      { index: 1, fileName: 'b.txt', reason: '文件类型不支持' },
    ])
    expect(api.warning).toHaveBeenCalledWith('批量上传完成：成功 2 个，失败 1 个')
    expect(api.success).not.toHaveBeenCalled()
    expect(onUploaded).toHaveBeenCalledOnce()
    expect(upload.visible.value).toBe(true)
  })

  it('prevents duplicate single upload while the first request is pending', async () => {
    const pending = deferred<{ data: { id: number } }>()
    api.uploadFile.mockReturnValue(pending.promise)
    const upload = useFileUploadDialog()
    const file = new File(['document'], 'report.pdf')

    upload.open('single')
    upload.selectFiles([file])
    const first = upload.submit()
    const duplicate = upload.submit()

    expect(api.uploadFile).toHaveBeenCalledOnce()
    expect(upload.uploading.value).toBe(true)
    expect(upload.canSubmit.value).toBe(false)
    pending.resolve({ data: { id: 7 } })
    await Promise.all([first, duplicate])

    expect(upload.uploading.value).toBe(false)
    expect(upload.visible.value).toBe(false)
  })
})

describe('file storage actions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.pageFiles.mockResolvedValue({
      data: { records: [], total: 0, current: 1, size: 10 },
    })
    api.pageFileConfigs.mockResolvedValue({
      data: {
        records: [
          { id: 4, configName: '旧 MinIO', storageType: 's3', status: 1 },
          { id: 5, configName: '本地配置', storageType: 'local', status: 0 },
        ],
        total: 2,
      },
    })
    api.bindFileStorage.mockResolvedValue({ data: undefined })
    api.confirm.mockResolvedValue(undefined)
  })

  it('downloads READY files through the authenticated blob API and releases the object URL', async () => {
    vi.useFakeTimers()
    const response = { data: new Blob(['stored bytes']) }
    api.downloadFile.mockResolvedValue(response)
    const createUrl = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:file-download')
    const revokeUrl = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const { actions } = useFileCrud()

    try {
      await actions.download({
        id: 12,
        originalName: 'report.txt',
        fileName: 'stored.txt',
        filePath: '/opaque',
        fileSize: 12,
        fileExt: 'txt',
        storageBindingRequired: false,
        lifecycleStatus: 'READY',
        storageType: 's3',
      })

      expect(api.downloadFile).toHaveBeenCalledWith(12)
      expect(createUrl).toHaveBeenCalledWith(response.data)
      expect(click).toHaveBeenCalledOnce()
      await vi.runAllTimersAsync()
      expect(revokeUrl).toHaveBeenCalledWith('blob:file-download')
    } finally {
      vi.useRealTimers()
      createUrl.mockRestore()
      revokeUrl.mockRestore()
      click.mockRestore()
    }
  })

  it('does not download a pending cleanup or unbound historical file', async () => {
    const { actions } = useFileCrud()
    const base = {
      id: 13,
      originalName: 'legacy.txt',
      fileName: 'legacy.txt',
      filePath: '/legacy',
      fileSize: 12,
      fileExt: 'txt',
      storageType: 'local' as const,
    }

    await actions.download({
      ...base,
      storageBindingRequired: false,
      lifecycleStatus: 'DELETE_PENDING',
    })
    await actions.download({
      ...base,
      storageBindingRequired: true,
      lifecycleStatus: 'READY',
    })

    expect(api.downloadFile).not.toHaveBeenCalled()
  })

  it('loads only same-type tenant configs and explicitly confirms the historical binding', async () => {
    const { actions, binding } = useFileCrud()
    await actions.openBinding({
      id: 20,
      originalName: 'legacy.bin',
      fileName: 'legacy.bin',
      filePath: 's3://old-bucket/legacy.bin',
      fileSize: 20,
      fileExt: 'bin',
      storageType: 's3',
      storageBindingRequired: true,
      lifecycleStatus: 'READY',
    })

    expect(api.pageFileConfigs).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 100,
      storageType: 's3',
    })
    expect(binding.configs).toHaveLength(1)
    expect(binding.configs[0].id).toBe(4)
    binding.configId = 4
    await binding.submit()

    expect(api.confirm).toHaveBeenCalledOnce()
    expect(api.confirm.mock.calls[0][0]).toContain('不会迁移文件')
    expect(api.bindFileStorage).toHaveBeenCalledWith(20, 4)
    expect(api.pageFiles).toHaveBeenCalled()
  })
})
