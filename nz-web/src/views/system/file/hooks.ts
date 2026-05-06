import { reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCrud } from '@/utils/CRUD'
import { listFiles, deleteFile, downloadFileUrl } from '@/api/system/file'

export interface FileRecord {
  id: number
  originalName: string
  filePath: string
  fileSize: number
  fileExt: string
  storageType: string
  uploaderName: string
  createdAt: string
}

interface FileQuery {
  originalName?: string
  fileExt?: string
}

export function useFileCrud() {
  const { table, actions } = useCrud<FileRecord, FileRecord, FileQuery & Record<string, unknown>>({
    name: '文件',
    api: {
      page: listFiles,
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

  async function handleDelete(id: number) {
    try {
      await ElMessageBox.confirm('确认删除该文件？', '提示', { type: 'warning' })
      await actions.remove(id)
    } catch {
      // 用户取消时不提示
    }
  }

  function handleDownload(row: FileRecord) {
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

  return {
    table: tableView,
    handleDelete,
    handleDownload,
  }
}
