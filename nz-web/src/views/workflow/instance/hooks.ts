import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  actionWorkflowInstance,
  cancelWorkflowInstance,
  deleteWorkflowInstance,
  getWorkflowInstance,
  pageWorkflowInstances,
  setWorkflowInstanceActive,
  startWorkflowInstance,
  terminateWorkflowInstance,
  type WorkflowInstance,
  type WorkflowInstanceActionForm,
  type WorkflowInstanceQuery,
  type WorkflowInstanceStartForm,
  type WorkflowInstanceStatus,
} from '@/api/workflow/instance'
import { pageWorkflowDefinitions, type WorkflowDefinition } from '@/api/workflow/definition'

const STATUS_TEXT: Record<WorkflowInstanceStatus, string> = {
  RUNNING: '运行中',
  COMPLETED: '已完成',
  REJECTED: '已驳回',
  CANCELED: '已撤回',
  TERMINATED: '已终止',
}

const EVENT_TEXT: Record<string, string> = {
  START: '发起',
  APPROVE: '同意',
  REJECT: '驳回',
  CANCEL: '撤回',
  TERMINATE: '终止',
}

export function instanceStatusText(status: WorkflowInstanceStatus) {
  return STATUS_TEXT[status] ?? status
}

export function instanceEventText(eventType: string) {
  return EVENT_TEXT[eventType] ?? eventType
}

export function formatVariables(variablesJson?: string) {
  try {
    return JSON.stringify(JSON.parse(variablesJson || '{}'), null, 2)
  } catch {
    return variablesJson || '{}'
  }
}

export function buildStartPayload(form: {
  flowCode: string
  businessKey: string
  title: string
  variablesJson: string
}): WorkflowInstanceStartForm {
  return {
    flowCode: form.flowCode.trim(),
    businessKey: form.businessKey.trim(),
    title: form.title.trim(),
    variables: JSON.parse(form.variablesJson || '{}') as Record<string, unknown>,
  }
}

export function useWorkflowInstance() {
  const loading = ref(false)
  const instances = ref<WorkflowInstance[]>([])
  const definitions = ref<WorkflowDefinition[]>([])
  const total = ref(0)
  const startVisible = ref(false)
  const detailVisible = ref(false)
  const detail = ref<WorkflowInstance>()
  const query = reactive<WorkflowInstanceQuery>({ pageNum: 1, pageSize: 10, mine: false })
  const startForm = reactive({
    flowCode: '',
    businessKey: '',
    title: '',
    variablesJson: '{}',
  })

  async function load() {
    loading.value = true
    try {
      const response = await pageWorkflowInstances({ ...query })
      instances.value = response.data.records
      total.value = response.data.total
    } finally {
      loading.value = false
    }
  }

  async function loadDefinitions() {
    const response = await pageWorkflowDefinitions({
      pageNum: 1,
      pageSize: 100,
      publishStatus: 1,
    })
    definitions.value = response.data.records.filter((item) => item.activityStatus === 1)
  }

  function openStart() {
    Object.assign(startForm, {
      flowCode: definitions.value[0]?.flowCode ?? '',
      businessKey: '',
      title: '',
      variablesJson: '{}',
    })
    startVisible.value = true
  }

  async function submitStart() {
    if (!startForm.flowCode || !startForm.businessKey.trim() || !startForm.title.trim()) {
      ElMessage.warning('请选择流程并填写业务标识和标题')
      return false
    }
    let payload: WorkflowInstanceStartForm
    try {
      payload = buildStartPayload(startForm)
    } catch {
      ElMessage.warning('流程变量不是有效的 JSON')
      return false
    }
    await startWorkflowInstance(payload)
    ElMessage.success('流程已发起')
    startVisible.value = false
    await load()
    return true
  }

  async function openDetail(instanceId: number) {
    const response = await getWorkflowInstance(instanceId)
    detail.value = response.data
    detailVisible.value = true
  }

  async function action(instanceId: number, actionForm: WorkflowInstanceActionForm) {
    await actionWorkflowInstance(instanceId, actionForm)
    ElMessage.success(actionForm.action === 'APPROVE' ? '审批已通过' : '流程已驳回')
    await load()
  }

  async function cancel(instanceId: number, comment?: string) {
    await cancelWorkflowInstance(instanceId, comment)
    ElMessage.success('流程已撤回')
    await load()
  }

  async function terminate(instanceId: number, comment?: string) {
    await terminateWorkflowInstance(instanceId, comment)
    ElMessage.success('流程已终止')
    await load()
  }

  async function setActive(instanceId: number, active: boolean) {
    await setWorkflowInstanceActive(instanceId, active)
    ElMessage.success(active ? '流程实例已激活' : '流程实例已挂起')
    await load()
  }

  async function remove(instanceId: number) {
    await deleteWorkflowInstance(instanceId)
    ElMessage.success('流程实例已删除')
    await load()
  }

  function resetQuery() {
    Object.assign(query, {
      pageNum: 1,
      pageSize: 10,
      flowCode: undefined,
      title: undefined,
      businessKey: undefined,
      status: undefined,
      mine: false,
    })
    void load()
  }

  return {
    loading,
    instances,
    definitions,
    total,
    startVisible,
    detailVisible,
    detail,
    query,
    startForm,
    load,
    loadDefinitions,
    openStart,
    submitStart,
    openDetail,
    action,
    cancel,
    terminate,
    setActive,
    remove,
    resetQuery,
  }
}
