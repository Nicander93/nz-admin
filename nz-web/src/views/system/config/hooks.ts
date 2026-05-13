import { ref, reactive } from 'vue'
import { useForm } from '@/utils/CRUD'
import { listConfigs, addConfig, updateConfig, deleteConfig } from '@/api/system/config'
import type { SysConfig } from '@/api/system/config'
import { ElMessage } from 'element-plus'

export interface UseConfigCrudOptions {
  listApi?: typeof listConfigs
  addApi?: typeof addConfig
  updateApi?: typeof updateConfig
  deleteApi?: typeof deleteConfig
}

/** 参数管理页面的 CRUD 逻辑。 */
export function useConfigCrud(options: UseConfigCrudOptions = {}) {
  const _listConfigs = options.listApi ?? listConfigs
  const _addConfig = options.addApi ?? addConfig
  const _updateConfig = options.updateApi ?? updateConfig
  const _deleteConfig = options.deleteApi ?? deleteConfig

  const loading = ref(false)
  const tableData = ref<SysConfig[]>([])

  const formState = useForm<SysConfig>({
    defaultForm: () => ({
      id: undefined as unknown as number,
      configName: '',
      configKey: '',
      configValue: '',
      configType: 2,
      status: 0,
      remark: '',
      createTime: undefined as unknown as string,
    }),
    addApi: (data) => _addConfig(data),
    updateApi: (data) => _updateConfig(data),
  })

  async function loadData() {
    loading.value = true
    try {
      const res = await _listConfigs()
      tableData.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function submit() {
    const result = await formState.submit()
    if (result.ok) {
      ElMessage.success(result.mode === 'add' ? '新增成功' : '更新成功')
      formState.close()
      await loadData()
    }
    return result
  }

  async function remove(id: number) {
    await _deleteConfig(id)
    ElMessage.success('删除成功')
    await loadData()
  }

  const table = reactive({
    loading,
    data: tableData,
    loadData,
  })

  const form = reactive({
    model: formState.form,
    visible: formState.visible,
    title: formState.title,
    mode: formState.mode,
    openAdd: () => formState.openAdd(),
    openEdit: (row: SysConfig) => formState.openEdit(row),
    close: formState.close,
  })

  const actions = reactive({
    submit,
    remove,
  })

  return { table, form, actions }
}
