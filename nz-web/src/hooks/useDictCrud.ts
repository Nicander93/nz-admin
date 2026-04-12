import { ref } from 'vue'
import { useCrud } from '@/utils/CRUD'
import { useForm } from '@/utils/CRUD'
import {
  pageDictTypes, addDictType, updateDictType, deleteDictType,
  listDictDataByType, addDictData, updateDictData, deleteDictData,
} from '@/api/system/dict'
import type { SysDictType, SysDictData, DictTypeQuery } from '@/api/system/dict'
import { ElMessage } from 'element-plus'

export function useDictCrud() {
  // 字典类型 CRUD
  const typeCrud = useCrud<SysDictType, SysDictType, DictTypeQuery>({
    name: '字典类型',
    pageApi: (params) => pageDictTypes(params),
    addApi: (data) => addDictType(data),
    updateApi: (data) => updateDictType(data),
    deleteApi: (ids) => deleteDictType(ids[0] as number),
    defaultForm: () => ({
      id: undefined as unknown as number,
      name: '', type: '', status: 0, remark: '',
      createTime: undefined as unknown as string,
    }),
    immediate: false,
  })

  // 字典数据
  const dataDrawerVisible = ref(false)
  const dataLoading = ref(false)
  const dataList = ref<SysDictData[]>([])
  const currentDictType = ref('')

  const dataForm = useForm<SysDictData>({
    defaultForm: () => ({
      id: undefined as unknown as number,
      dictType: '', label: '', value: '', sort: 0, status: 0, remark: '',
      createTime: undefined as unknown as string,
    }),
    addApi: (data) => addDictData(data),
    updateApi: (data) => updateDictData(data),
  })

  async function loadDataList() {
    dataLoading.value = true
    try {
      const res = await listDictDataByType(currentDictType.value)
      dataList.value = res.data
    } finally {
      dataLoading.value = false
    }
  }

  async function openDataPanel(row: SysDictType) {
    currentDictType.value = row.type
    dataDrawerVisible.value = true
    await loadDataList()
  }

  function openDataAdd() {
    dataForm.openAdd()
    dataForm.form.dictType = currentDictType.value
  }

  async function handleDataSubmit() {
    const result = await dataForm.submit()
    if (result.ok) {
      ElMessage.success(result.mode === 'add' ? '新增成功' : '更新成功')
      dataForm.close()
      await loadDataList()
    }
    return result
  }

  async function handleDeleteData(id: number) {
    await deleteDictData(id)
    ElMessage.success('删除成功')
    await loadDataList()
  }

  function handleTypeResetQuery() {
    typeCrud.resetQuery()
    typeCrud.refresh()
  }

  return {
    typeCrud,
    dataDrawerVisible,
    dataLoading,
    dataList,
    currentDictType,
    dataForm,
    loadDataList,
    openDataPanel,
    openDataAdd,
    handleDataSubmit,
    handleDeleteData,
    handleTypeResetQuery,
  }
}
