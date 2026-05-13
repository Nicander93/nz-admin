import { reactive } from 'vue'
import { useCrud } from '@/utils/CRUD'
import { fileApi, type FileQuery, type SysFile } from '@/api/system'

/** 文件管理页面的 CRUD 逻辑。 */
export function useFileCrud() {
  const { table, actions } = useCrud<SysFile, SysFile, FileQuery & Record<string, unknown>>({
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

  async function uploadFile(file: File) {
    await fileApi.uploadFile(file)
    table.refresh()
  }

  async function uploadFiles(files: File[]) {
    await fileApi.uploadFiles(files)
    table.refresh()
  }

  function handleResetQuery() {
    table.resetQuery()
    table.refresh()
  }

  function loadData() {
    table.refresh()
  }

  const tableView = reactive({
    data: table.data,
    loading: table.loading,
    pagination: table.pagination,
    query: table.query,
    refresh: table.refresh,
    handleResetQuery,
    loadData,
  })

  const actionsView = reactive({
    remove: actions.remove,
    uploadFile,
    uploadFiles,
  })

  return {
    table: tableView,
    actions: actionsView,
  }
}
