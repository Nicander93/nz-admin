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

/**
 * 部门页面的 CRUD 逻辑都收在这里。
 */
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

  // 拉部门列表，然后顺手组装成树。
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

  // 切一下展开状态，并重新拉一遍数据。
  function toggleExpand() {
    isExpand.value = !isExpand.value
    loadData()
  }

  // 打开新增弹窗；如果传了父 id，就默认挂到这个父节点下。
  function openAdd(parentId?: number) {
    formState.openAdd()
    if (parentId !== undefined) {
      formState.form.parentId = parentId
    }
  }

  // 提交表单，成功后关窗并刷新列表。
  async function handleSubmit() {
    const result = await formState.submit()
    if (result.ok) {
      ElMessage.success(result.mode === 'add' ? '新增成功' : '更新成功')
      formState.close()
      await loadData()
    }
    return result
  }

  // 删掉部门后再刷新列表。
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
