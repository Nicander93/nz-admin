import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createWorkflowCategory,
  deleteWorkflowCategory,
  exportWorkflowCategories,
  getWorkflowCategory,
  getWorkflowCategoryTree,
  updateWorkflowCategory,
  type WorkflowCategory,
  type WorkflowCategoryForm,
} from '@/api/workflow/category'

export interface CategoryTreeOption extends WorkflowCategory {
  disabled?: boolean
  children?: CategoryTreeOption[]
}

/**
 * 构建父分类选项，并禁用当前分类及其所有下级。
 */
export function buildParentOptions(categories: WorkflowCategory[], currentId?: number): CategoryTreeOption[] {
  const mapNode = (category: WorkflowCategory, blocked: boolean): CategoryTreeOption => {
    const disabled = blocked || category.categoryId === currentId
    return {
      ...category,
      disabled,
      children: (category.children ?? []).map((child) => mapNode(child, disabled)),
    }
  }
  return [
    {
      categoryId: 0,
      parentId: 0,
      ancestors: '0',
      categoryName: '顶级分类',
      orderNum: 0,
      builtIn: 1,
      children: categories.map((category) => mapNode(category, false)),
    },
  ]
}

export function useWorkflowCategory() {
  const loading = ref(false)
  const categories = ref<WorkflowCategory[]>([])
  const dialogVisible = ref(false)
  const dialogMode = ref<'create' | 'update'>('create')
  const query = reactive({ categoryName: '' })
  const form = reactive<WorkflowCategoryForm>({
    parentId: 0,
    categoryName: '',
    orderNum: 0,
  })

  const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增流程分类' : '修改流程分类'))
  const parentOptions = computed(() => buildParentOptions(categories.value, form.categoryId))

  async function load() {
    loading.value = true
    try {
      const response = await getWorkflowCategoryTree({ categoryName: query.categoryName || undefined })
      categories.value = response.data ?? []
    } finally {
      loading.value = false
    }
  }

  function resetForm(parentId = 0) {
    form.categoryId = undefined
    form.parentId = parentId
    form.categoryName = ''
    form.orderNum = 0
  }

  function openCreate(parentId = 0) {
    resetForm(parentId)
    dialogMode.value = 'create'
    dialogVisible.value = true
  }

  async function openEdit(categoryId: number) {
    const response = await getWorkflowCategory(categoryId)
    Object.assign(form, response.data)
    dialogMode.value = 'update'
    dialogVisible.value = true
  }

  async function submit() {
    if (!form.categoryName.trim()) {
      ElMessage.warning('请输入分类名称')
      return false
    }
    if (dialogMode.value === 'create') {
      await createWorkflowCategory({ ...form, categoryName: form.categoryName.trim() })
    } else {
      await updateWorkflowCategory({ ...form, categoryName: form.categoryName.trim() })
    }
    ElMessage.success(dialogMode.value === 'create' ? '新增成功' : '修改成功')
    dialogVisible.value = false
    await load()
    return true
  }

  async function remove(categoryId: number) {
    await deleteWorkflowCategory(categoryId)
    ElMessage.success('删除成功')
    await load()
  }

  async function exportFile() {
    const response = await exportWorkflowCategories({ categoryName: query.categoryName || undefined })
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = 'workflow-category.xlsx'
    link.click()
    URL.revokeObjectURL(url)
  }

  function resetQuery() {
    query.categoryName = ''
    void load()
  }

  return {
    loading,
    categories,
    query,
    form,
    dialogVisible,
    dialogTitle,
    parentOptions,
    load,
    resetQuery,
    openCreate,
    openEdit,
    submit,
    remove,
    exportFile,
  }
}
