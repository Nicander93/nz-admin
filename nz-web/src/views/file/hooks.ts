import { reactive } from 'vue'
import { useCrud } from '@/utils/CRUD'
import { fileApi, type FileQuery, type SysFile } from '@/api/system'

/**
 * 文件管理页面的 CRUD 逻辑。
 */
export function useFileCrud() {
  const { table, form, actions } = useCrud<SysFile, SysFile, FileQuery & Record<string, unknown>>({
    name: '文件',
    api: {
      page: fileApi.pageFiles,
      delete: (ids: number[]) => fileApi.deleteFile(ids[0]),
    },
    defaultForm: () => ({
      id: undefined as unknown as number,
      originalName: '',
      fileName: '',
      filePath: '',
      fileUrl: '',
      fileSize: 0,
      fileExt: '',
      mimeType: '',
      uploaderId: undefined as unknown as number,
      uploaderName: '',
      createTime: undefined as unknown as string,
      updateTime: undefined as unknown as string,
    }),
    immediate: false,
  })

  // 上传文件
  async function uploadFile(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    await fileApi.uploadFile(formData)
    table.refresh()
  }

  // 上传多个文件
  async function uploadFiles(files: File[]) {
    const formData = new FormData()
    files.forEach((f) => formData.append('files', f))
    await fileApi.uploadFiles(formData)
    table.refresh()
  }

  // 重置查询条件后重新查一遍列表。
  function handleResetQuery() {
    table.resetQuery()
    table.refresh()
  }

  // 页面初始化时拉第一页。
  function init() {
    table.refresh()
  }

  const lifecycle = reactive({
    init,
  })

  const tableView = reactive({
    data: table.data,
    loading: table.loading,
    pagination: table.pagination,
    query: table.query,
    refresh: table.refresh,
    handleResetQuery,
  })

  const actionsView = reactive({
    remove: actions.remove,
    uploadFile,
    uploadFiles,
  })

  return {
    table: tableView,
    actions: actionsView,
    lifecycle,
  }
}
