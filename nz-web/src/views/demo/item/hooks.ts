import { ElMessage } from 'element-plus'
import { reactive, ref, watch } from 'vue'

import {
  addDemoItem,
  deleteDemoItem,
  pageDemoItems,
  updateDemoItem,
  type DemoItem,
  type DemoItemQuery,
  downloadDemoItemTemplate,
  exportDemoItems,
  importDemoItems,
  previewDemoItemImport,
  type DemoItemImportPreview,
} from '@/api/demo/item'
import { useCrud } from '@/utils/CRUD'

/**
 * 示例条目页面的 CRUD 逻辑。
 */
export function useDemoItemCrud() {
  const { table, form, actions } = useCrud<DemoItem, DemoItem, DemoItemQuery>({
    name: '示例条目',
    api: {
      page: pageDemoItems,
      add: addDemoItem,
      update: updateDemoItem,
      delete: (ids: number[]) => deleteDemoItem(ids[0]),
    },
    defaultForm: () => ({
      name: '',
      category: 'general',
      status: 0,
      sort: 10,
      remark: '',
    }),
    immediate: false,
  })

  function handleResetQuery() {
    table.resetQuery()
    void table.refresh()
  }

  return {
    table: reactive({
      data: table.data,
      loading: table.loading,
      pagination: table.pagination,
      query: table.query,
      refresh: table.refresh,
      handleResetQuery,
    }),
    form: reactive({
      model: form.form,
      visible: form.visible,
      mode: form.mode,
      title: form.title,
      openAdd: form.toAdd,
      openEdit: form.toEdit,
      close: form.close,
    }),
    actions: reactive({
      submit: form.submit,
      remove: actions.remove,
    }),
    excel: reactive(useDemoItemExcel(table.query, table.refresh)),
  }
}

/**
 * 示例条目 Excel 导入导出状态与请求。
 */
export function useDemoItemExcel(
  query: Pick<DemoItemQuery, 'name' | 'category' | 'status'>,
  refresh: () => Promise<unknown>,
) {
  const importVisible = ref(false)
  const importing = ref(false)
  const previewing = ref(false)
  const selectedFile = ref<File>()
  const preview = ref<DemoItemImportPreview>()
  const retryKeys = new Map<string, string>()
  let previewSequence = 0

  function invalidatePreview() {
    previewSequence += 1
    previewing.value = false
    selectedFile.value = undefined
    preview.value = undefined
  }

  watch(
    importVisible,
    (visible) => {
      if (!visible) invalidatePreview()
    },
    { flush: 'sync' },
  )

  function openImport() {
    invalidatePreview()
    importVisible.value = true
  }

  function closeImport() {
    if (importing.value) return
    importVisible.value = false
  }

  function beforeClose(done: () => void) {
    if (importing.value) return
    closeImport()
    done()
  }

  async function selectFile(uploadFile: { raw?: File }) {
    if (importing.value) return
    const file = uploadFile.raw
    if (!file) return
    const sequence = ++previewSequence
    selectedFile.value = undefined
    preview.value = undefined
    previewing.value = false
    if (!importVisible.value) importVisible.value = true
    if (!file.name.toLowerCase().endsWith('.xlsx')) {
      ElMessage.warning('请选择 .xlsx 文件')
      return
    }
    selectedFile.value = file
    previewing.value = true
    try {
      const response = await previewDemoItemImport(file)
      if (sequence === previewSequence && importVisible.value)
        preview.value = response.data
    } catch {
      if (sequence === previewSequence && importVisible.value)
        ElMessage.error('Excel 预检失败，请重新选择文件')
    } finally {
      if (sequence === previewSequence) previewing.value = false
    }
  }

  function downloadBlob(response: { data: Blob }, filename: string) {
    const url = URL.createObjectURL(response.data)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = filename
    anchor.click()
    window.setTimeout(() => URL.revokeObjectURL(url), 0)
  }

  async function downloadTemplate() {
    downloadBlob(await downloadDemoItemTemplate(), '示例条目导入模板.xlsx')
  }

  async function exportCurrent() {
    downloadBlob(
      await exportDemoItems({
        name: query.name,
        category: query.category,
        status: query.status,
      }),
      '示例条目.xlsx',
    )
  }

  async function confirmImport() {
    const currentPreview = preview.value
    if (
      importing.value ||
      !currentPreview ||
      currentPreview.errors.length ||
      !currentPreview.rows.length
    )
      return
    const payload = JSON.stringify(currentPreview.rows)
    if (!retryKeys.has(payload)) retryKeys.set(payload, crypto.randomUUID())
    importing.value = true
    try {
      const response = await importDemoItems(
        currentPreview.rows,
        retryKeys.get(payload)!,
      )
      retryKeys.delete(payload)
      ElMessage.success('成功导入 ' + response.data + ' 条')
      importVisible.value = false
    } catch {
      ElMessage.error('导入失败，请重试；相同数据会复用本次请求标识')
      return
    } finally {
      importing.value = false
    }
    try {
      await refresh()
    } catch {
      ElMessage.warning('导入已成功，列表刷新失败，请重新查询')
    }
  }

  return {
    importVisible,
    importing,
    previewing,
    selectedFile,
    preview,
    openImport,
    closeImport,
    beforeClose,
    selectFile,
    downloadTemplate,
    exportCurrent,
    confirmImport,
  }
}
