import { reactive } from 'vue'
import { useCrud } from '@/utils/CRUD'
import { pageFiles, deleteFile, downloadFileUrl, type FileQuery, type SysFile } from '@/api/system/file'

/** 系统文件列表页的查询与操作逻辑。 */
export function useFileCrud() {
  const { table, actions } = useCrud<SysFile, SysFile, FileQuery & Record<string, unknown>>({
    name: '文件',
    api: {
      page: pageFiles,
      add: async () => Promise.reject(new Error('文件页面不支持表单新增')),
      update: async () => Promise.reject(new Error('文件页面不支持表单编辑')),
      delete: (ids: number[]) => deleteFile(ids[0]),
    },
    defaultForm: () => ({
      id: undefined as unknown as number,
      originalName: '', filePath: '', fileSize: 0,
      fileExt: '', storageType: 'local', uploaderName: '', createdAt: '',
    }),
    immediate: false,
  })

  function handleSearch() {
    table.pagination.current = 1
    table.refresh()
  }

  function handleResetQuery() {
    table.resetQuery()
    table.refresh()
  }

  function download(row: SysFile) {
    window.open(downloadFileUrl(row.id), '_blank')
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
    remove: actions.remove,
    download,
  })

  return {
    table: tableView,
    actions: actionsView,
  }
}
