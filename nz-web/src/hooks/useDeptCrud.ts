import { ref } from 'vue'
import { useForm } from '@/utils/CRUD'
import { listDepts, addDept, updateDept, deleteDept } from '@/api/system/dept'
import type { SysDept } from '@/api/system/dept'
import { buildTree } from '@/utils/tree'
import { ElMessage } from 'element-plus'

export interface UseDeptCrudOptions {
  listApi?: typeof listDepts
  addApi?: typeof addDept
  updateApi?: typeof updateDept
  deleteApi?: typeof deleteDept
}

export function useDeptCrud(options: UseDeptCrudOptions = {}) {
  const _listDepts = options.listApi ?? listDepts
  const _addDept = options.addApi ?? addDept
  const _updateDept = options.updateApi ?? updateDept
  const _deleteDept = options.deleteApi ?? deleteDept

  const loading = ref(false)
  const treeData = ref<SysDept[]>([])
  const flatData = ref<SysDept[]>([])
  const isExpand = ref(true)

  const formState = useForm<SysDept>({
    defaultForm: () => ({
      id: undefined as unknown as number,
      parentId: 0, name: '', sort: 0, status: 0,
      createTime: undefined as unknown as string,
    }),
    addApi: (data) => _addDept(data),
    updateApi: (data) => _updateDept(data),
  })

  async function loadData() {
    loading.value = true
    try {
      const res = await _listDepts()
      flatData.value = res.data
      treeData.value = buildTree(res.data)
    } finally {
      loading.value = false
    }
  }

  function toggleExpand() {
    isExpand.value = !isExpand.value
    loadData()
  }

  function openAdd(parentId?: number) {
    formState.openAdd()
    if (parentId !== undefined) {
      formState.form.parentId = parentId
    }
  }

  async function handleSubmit() {
    const result = await formState.submit()
    if (result.ok) {
      ElMessage.success(result.mode === 'add' ? '新增成功' : '更新成功')
      formState.close()
      await loadData()
    }
    return result
  }

  async function handleDelete(id: number) {
    await _deleteDept(id)
    ElMessage.success('删除成功')
    await loadData()
  }

  return {
    loading,
    treeData,
    flatData,
    isExpand,
    ...formState,
    loadData,
    toggleExpand,
    openAdd,
    handleSubmit,
    handleDelete,
  }
}
