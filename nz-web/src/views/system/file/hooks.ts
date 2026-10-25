import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, reactive, ref } from 'vue'

import {
  bindFileStorage,
  deleteFile,
  downloadFile,
  pageFiles,
  uploadFile,
  uploadFiles,
  type BatchUploadResult,
  type FailedFileUpload,
  type FileQuery,
  type SysFile,
} from '@/api/system/file'
import { pageFileConfigs, type SysFileConfig } from '@/api/system/fileConfig'
import { useCrud } from '@/utils/CRUD'

/** 系统文件列表页的查询与操作逻辑。 */
export function useFileCrud() {
  const { table } = useCrud<SysFile, SysFile, FileQuery & Record<string, unknown>>({
    name: '文件',
    api: {
      page: pageFiles,
      add: async () => Promise.reject(new Error('文件页面不支持表单新增')),
      update: async () => Promise.reject(new Error('文件页面不支持表单编辑')),
      delete: (ids: number[]) => deleteFile(ids[0]),
    },
    defaultForm: () => ({
      id: undefined as unknown as number,
      originalName: '',
      filePath: '',
      fileSize: 0,
      fileExt: '',
      storageType: 'local',
      storageBindingRequired: false,
      lifecycleStatus: 'READY',
      uploaderName: '',
      createTime: '',
    }),
    immediate: false,
  })

  const bindingVisible = ref(false)
  const bindingLoading = ref(false)
  const bindingSubmitting = ref(false)
  const bindingFile = ref<SysFile>()
  const bindingConfigs = ref<SysFileConfig[]>([])
  const bindingConfigId = ref<number>()
  let bindingRequestSequence = 0

  function handleSearch() {
    table.pagination.current = 1
    return table.refresh()
  }

  function handleResetQuery() {
    table.resetQuery()
    return table.refresh()
  }

  async function download(row: SysFile) {
    if (row.lifecycleStatus !== 'READY' || row.storageBindingRequired) return
    const response = await downloadFile(row.id)
    const url = URL.createObjectURL(response.data)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = row.originalName || row.fileName
    try {
      anchor.click()
    } finally {
      window.setTimeout(() => URL.revokeObjectURL(url), 0)
    }
  }

  async function remove(id: number) {
    await deleteFile(id)
    ElMessage.success('文件清理请求已提交')
    await table.refresh()
  }

  async function openBinding(row: SysFile) {
    if (!row.storageBindingRequired || !row.storageType) return
    const sequence = ++bindingRequestSequence
    bindingFile.value = row
    bindingConfigId.value = undefined
    bindingConfigs.value = []
    bindingVisible.value = true
    bindingLoading.value = true
    try {
      const response = await pageFileConfigs({
        pageNum: 1,
        pageSize: 100,
        storageType: row.storageType,
      })
      if (sequence !== bindingRequestSequence) return
      bindingConfigs.value = response.data.records.filter(
        (config) => config.storageType === row.storageType,
      )
    } catch {
      if (sequence === bindingRequestSequence)
        ElMessage.error('读取同类型存储配置失败')
    } finally {
      if (sequence === bindingRequestSequence) bindingLoading.value = false
    }
  }

  function closeBinding() {
    bindingRequestSequence += 1
    bindingVisible.value = false
    bindingFile.value = undefined
    bindingConfigId.value = undefined
    bindingConfigs.value = []
    bindingLoading.value = false
  }

  async function bindHistoricalFile() {
    const file = bindingFile.value
    const configId = bindingConfigId.value
    const config = bindingConfigs.value.find((item) => item.id === configId)
    if (!file || !config || bindingSubmitting.value) return
    try {
      await ElMessageBox.confirm(
        '将“' + file.originalName + '”绑定到配置“' + config.configName + '”（'
          + storageTypeLabel(config.storageType)
          + '）。此操作只记录原存储位置，不会迁移文件。确认继续？',
        '确认历史文件绑定',
        { type: 'warning', confirmButtonText: '确认绑定', cancelButtonText: '取消' },
      )
    } catch {
      return
    }

    bindingSubmitting.value = true
    try {
      await bindFileStorage(file.id, config.id)
      ElMessage.success('历史文件绑定成功')
      closeBinding()
      await table.refresh()
    } finally {
      bindingSubmitting.value = false
    }
  }

  const tableView = reactive({
    data: table.data,
    loading: table.loading,
    pagination: table.pagination,
    query: table.query,
    refresh: table.refresh,
    handleResetQuery,
    handleSearch,
  })

  const actionsView = reactive({
    remove,
    download,
    openBinding,
  })

  return {
    table: tableView,
    actions: actionsView,
    binding: reactive({
      visible: bindingVisible,
      loading: bindingLoading,
      submitting: bindingSubmitting,
      file: bindingFile,
      configs: bindingConfigs,
      configId: bindingConfigId,
      close: closeBinding,
      submit: bindHistoricalFile,
    }),
  }
}

export type FileUploadMode = 'single' | 'batch'

/** 上传对话框状态，批量失败时只保留失败项以便安全重试。 */
export function useFileUploadDialog(onUploaded: () => void = () => {}) {
  const visible = ref(false)
  const mode = ref<FileUploadMode>('single')
  const uploading = ref(false)
  const files = ref<File[]>([])
  const failedFiles = ref<FailedFileUpload[]>([])

  function open(nextMode: FileUploadMode) {
    if (uploading.value) return
    mode.value = nextMode
    files.value = []
    failedFiles.value = []
    visible.value = true
  }

  function selectFiles(selected: File[]) {
    if (uploading.value) return
    const nextFiles = mode.value === 'single' ? selected.slice(0, 1) : selected
    files.value = mode.value === 'batch'
      ? [...files.value, ...nextFiles.filter((file) => !files.value.some(
          (current) => current.name === file.name
            && current.size === file.size
            && current.lastModified === file.lastModified,
        ))]
      : nextFiles
    failedFiles.value = []
  }

  function removeSelectedFile(file: File) {
    if (uploading.value) return
    files.value = files.value.filter((item) => item !== file)
  }

  function reset() {
    if (uploading.value) return
    visible.value = false
    files.value = []
    failedFiles.value = []
  }

  function getFailureIndexes(
    requestFiles: File[],
    failures: FailedFileUpload[],
  ): Set<number> {
    const indexes = new Set<number>()
    for (const failure of failures) {
      const candidates = [failure.index]
      let matched = candidates.find(
        (index) => index >= 0 && index < requestFiles.length
          && requestFiles[index].name === failure.fileName
          && !indexes.has(index),
      )
      if (matched === undefined) {
        matched = requestFiles.findIndex(
          (file, index) => file.name === failure.fileName && !indexes.has(index),
        )
      }
      if (matched >= 0) indexes.add(matched)
    }
    return indexes
  }

  async function submit() {
    if (uploading.value || files.value.length === 0) return
    uploading.value = true
    const submittedFiles = [...files.value]
    try {
      if (mode.value === 'single') {
        await uploadFile(submittedFiles[0])
        files.value = []
        failedFiles.value = []
        visible.value = false
        ElMessage.success('文件上传成功')
        onUploaded()
        return
      }

      const response = await uploadFiles(submittedFiles)
      const result: BatchUploadResult = response.data
      const failures = result.failedFiles ?? []
      failedFiles.value = failures
      if (failures.length > 0 || result.failCount > 0) {
        const failedIndexes = getFailureIndexes(submittedFiles, failures)
        files.value = failedIndexes.size > 0
          ? submittedFiles.filter((_, index) => failedIndexes.has(index))
          : submittedFiles
        ElMessage.warning(
          '批量上传完成：成功 ' + result.successCount + ' 个，失败 ' + result.failCount + ' 个',
        )
        if (result.successCount > 0) onUploaded()
        return
      }

      files.value = []
      visible.value = false
      ElMessage.success('成功上传 ' + result.successCount + ' 个文件')
      onUploaded()
    } catch {
      ElMessage.error('文件上传失败，请检查文件后重试')
    } finally {
      uploading.value = false
    }
  }

  return {
    visible,
    mode,
    uploading,
    files,
    failedFiles,
    open,
    selectFiles,
    removeSelectedFile,
    reset,
    submit,
    canSubmit: computed(() => files.value.length > 0 && !uploading.value),
  }
}

export function storageTypeLabel(storageType?: string): string {
  if (storageType === 'oss') return 'OSS'
  if (storageType === 's3') return 'S3 / MinIO'
  if (storageType === 'local') return '本地'
  return '未知类型'
}
