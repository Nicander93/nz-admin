import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  copyWorkflowDefinition,
  createWorkflowDefinition,
  deleteWorkflowDefinition,
  exportWorkflowDefinition,
  getWorkflowDefinition,
  importWorkflowDefinition,
  pageWorkflowDefinitions,
  publishWorkflowDefinition,
  setWorkflowDefinitionActive,
  unpublishWorkflowDefinition,
  updateWorkflowDefinition,
  type WorkflowDefinition,
  type WorkflowDefinitionCopyForm,
  type WorkflowDefinitionForm,
  type WorkflowDefinitionQuery,
} from '@/api/workflow/definition'
import { getWorkflowCategoryTree, type WorkflowCategory } from '@/api/workflow/category'

export const DEFAULT_WORKFLOW_MODEL = JSON.stringify(
  {
    nodes: [
      { id: 'start', type: 'start', name: '开始' },
      { id: 'approve', type: 'task', name: '审批', assignee: 'role:manager' },
      { id: 'end', type: 'end', name: '结束' },
    ],
    edges: [
      { source: 'start', target: 'approve' },
      { source: 'approve', target: 'end' },
    ],
  },
  null,
  2,
)

export function formatModelJson(modelJson: string) {
  return JSON.stringify(JSON.parse(modelJson), null, 2)
}

export function useWorkflowDefinition() {
  const loading = ref(false)
  const definitions = ref<WorkflowDefinition[]>([])
  const categories = ref<WorkflowCategory[]>([])
  const total = ref(0)
  const editorVisible = ref(false)
  const editorMode = ref<'create' | 'update'>('create')
  const copyVisible = ref(false)
  const query = reactive<WorkflowDefinitionQuery>({ pageNum: 1, pageSize: 10 })
  const form = reactive<WorkflowDefinitionForm>({
    flowCode: '',
    flowName: '',
    categoryId: undefined,
    formPath: '',
    modelJson: DEFAULT_WORKFLOW_MODEL,
    remark: '',
  })
  const copyForm = reactive<WorkflowDefinitionCopyForm>({
    sourceDefinitionId: 0,
    flowCode: '',
    flowName: '',
  })

  async function load() {
    loading.value = true
    try {
      const response = await pageWorkflowDefinitions({ ...query })
      definitions.value = response.data.records
      total.value = response.data.total
    } finally {
      loading.value = false
    }
  }

  async function loadCategories() {
    const response = await getWorkflowCategoryTree()
    categories.value = response.data ?? []
  }

  function resetForm() {
    Object.assign(form, {
      definitionId: undefined,
      flowCode: '',
      flowName: '',
      categoryId: undefined,
      formPath: '',
      modelJson: DEFAULT_WORKFLOW_MODEL,
      remark: '',
    })
  }

  function openCreate() {
    resetForm()
    editorMode.value = 'create'
    editorVisible.value = true
  }

  async function openEdit(definitionId: number) {
    const response = await getWorkflowDefinition(definitionId)
    Object.assign(form, response.data, { modelJson: formatModelJson(response.data.modelJson) })
    editorMode.value = 'update'
    editorVisible.value = true
  }

  async function submit() {
    if (!form.flowCode.trim() || !form.flowName.trim() || !form.categoryId) {
      ElMessage.warning('请填写流程编码、流程名称和分类')
      return false
    }
    try {
      form.modelJson = formatModelJson(form.modelJson)
    } catch {
      ElMessage.warning('流程模型不是有效的 JSON')
      return false
    }
    if (editorMode.value === 'create') {
      await createWorkflowDefinition({ ...form })
    } else {
      await updateWorkflowDefinition({ ...form })
    }
    ElMessage.success(editorMode.value === 'create' ? '草稿已创建' : '草稿已保存')
    editorVisible.value = false
    await load()
    return true
  }

  async function publish(definitionId: number) {
    await publishWorkflowDefinition(definitionId)
    ElMessage.success('流程定义已发布')
    await load()
  }

  async function unpublish(definitionId: number) {
    await unpublishWorkflowDefinition(definitionId)
    ElMessage.success('流程定义已转为草稿')
    await load()
  }

  async function setActive(definitionId: number, active: boolean) {
    await setWorkflowDefinitionActive(definitionId, active)
    ElMessage.success(active ? '流程定义已激活' : '流程定义已挂起')
    await load()
  }

  function openCopy(definition: WorkflowDefinition) {
    copyForm.sourceDefinitionId = definition.definitionId
    copyForm.flowCode = `${definition.flowCode}_copy`
    copyForm.flowName = `${definition.flowName}（副本）`
    copyVisible.value = true
  }

  async function submitCopy() {
    await copyWorkflowDefinition({ ...copyForm })
    ElMessage.success('流程定义已复制为草稿')
    copyVisible.value = false
    await load()
  }

  async function importFile(file: Blob) {
    await importWorkflowDefinition(file)
    ElMessage.success('流程定义已导入为草稿')
    await load()
  }

  async function exportFile(definition: WorkflowDefinition) {
    const response = await exportWorkflowDefinition(definition.definitionId)
    const url = URL.createObjectURL(response.data)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `${definition.flowCode}-v${definition.versionNo}.json`
    anchor.click()
    URL.revokeObjectURL(url)
  }

  async function remove(definitionId: number) {
    await deleteWorkflowDefinition(definitionId)
    ElMessage.success('流程定义已删除')
    await load()
  }

  function resetQuery() {
    Object.assign(query, { pageNum: 1, pageSize: 10, flowCode: undefined, flowName: undefined,
      categoryId: undefined, publishStatus: undefined })
    void load()
  }

  return {
    loading,
    definitions,
    categories,
    total,
    editorVisible,
    editorMode,
    copyVisible,
    query,
    form,
    copyForm,
    load,
    loadCategories,
    resetQuery,
    openCreate,
    openEdit,
    submit,
    publish,
    unpublish,
    setActive,
    openCopy,
    submitCopy,
    importFile,
    exportFile,
    remove,
  }
}
