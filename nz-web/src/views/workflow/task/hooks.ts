import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  actionWorkflowTask,
  copyWorkflowTask,
  delegateWorkflowTask,
  markWorkflowTaskCopyRead,
  pageWorkflowCopyTasks,
  pageWorkflowDoneTasks,
  pageWorkflowTodoTasks,
  resolveWorkflowTask,
  transferWorkflowTask,
  type WorkflowTask,
  type WorkflowTaskAction,
  type WorkflowTaskQuery,
  type WorkflowTaskTab,
} from '@/api/workflow/task'
import { getWorkflowInstance, type WorkflowInstance } from '@/api/workflow/instance'
import { pageUsers, type SysUser } from '@/api/system/user'

const ACTION_TEXT: Record<WorkflowTaskAction, string> = {
  APPROVE: '同意',
  REJECT: '驳回',
  TRANSFER: '转办',
  DELEGATE: '委派',
  RESOLVE: '完成委派',
  CANCEL: '撤回',
  TERMINATE: '终止',
}

export function taskActionText(action?: WorkflowTaskAction) {
  return action ? ACTION_TEXT[action] ?? action : '-'
}

export function uniqueReceiverIds(receiverIds: number[], currentUserId?: number) {
  return [...new Set(receiverIds)].filter((id) => id > 0 && id !== currentUserId)
}

export function useWorkflowTask() {
  const loading = ref(false)
  const activeTab = ref<WorkflowTaskTab>('todo')
  const tasks = ref<WorkflowTask[]>([])
  const users = ref<SysUser[]>([])
  const total = ref(0)
  const detailVisible = ref(false)
  const detail = ref<WorkflowInstance>()
  const transferVisible = ref(false)
  const copyVisible = ref(false)
  const delegateVisible = ref(false)
  const currentTask = ref<WorkflowTask>()
  const query = reactive<WorkflowTaskQuery>({ pageNum: 1, pageSize: 10 })
  const transferForm = reactive({ targetUserId: undefined as number | undefined, comment: '' })
  const copyForm = reactive({ receiverIds: [] as number[], comment: '' })
  const delegateForm = reactive({ targetUserId: undefined as number | undefined, comment: '' })

  async function load() {
    loading.value = true
    try {
      const params = { ...query }
      const response = activeTab.value === 'todo'
        ? await pageWorkflowTodoTasks(params)
        : activeTab.value === 'done'
          ? await pageWorkflowDoneTasks(params)
          : await pageWorkflowCopyTasks(params)
      tasks.value = response.data.records
      total.value = response.data.total
    } finally {
      loading.value = false
    }
  }

  async function loadUsers() {
    const response = await pageUsers({ pageNum: 1, pageSize: 200, status: 0 })
    users.value = response.data.records
  }

  function changeTab(tab: WorkflowTaskTab) {
    activeTab.value = tab
    Object.assign(query, { pageNum: 1, nodeName: undefined, action: undefined, readStatus: undefined })
    void load()
  }

  async function openDetail(instanceId: number) {
    const response = await getWorkflowInstance(instanceId)
    detail.value = response.data
    detailVisible.value = true
  }

  async function action(taskId: number, actionType: 'APPROVE' | 'REJECT', comment?: string) {
    await actionWorkflowTask(taskId, { action: actionType, comment })
    ElMessage.success(actionType === 'APPROVE' ? '任务已通过' : '任务已驳回')
    await load()
  }

  function openTransfer(task: WorkflowTask) {
    currentTask.value = task
    Object.assign(transferForm, { targetUserId: undefined, comment: '' })
    transferVisible.value = true
  }

  async function submitTransfer() {
    if (!currentTask.value?.taskId || !transferForm.targetUserId) {
      ElMessage.warning('请选择转办用户')
      return false
    }
    await transferWorkflowTask(currentTask.value.taskId, {
      targetUserId: transferForm.targetUserId,
      comment: transferForm.comment.trim() || undefined,
    })
    ElMessage.success('任务已转办')
    transferVisible.value = false
    await load()
    return true
  }

  function openDelegate(task: WorkflowTask) {
    currentTask.value = task
    Object.assign(delegateForm, { targetUserId: undefined, comment: '' })
    delegateVisible.value = true
  }

  async function submitDelegate() {
    if (!currentTask.value?.taskId || !delegateForm.targetUserId) {
      ElMessage.warning('请选择受托用户')
      return false
    }
    await delegateWorkflowTask(currentTask.value.taskId, {
      targetUserId: delegateForm.targetUserId,
      comment: delegateForm.comment.trim() || undefined,
    })
    ElMessage.success('任务已委派')
    delegateVisible.value = false
    await load()
    return true
  }

  async function resolveDelegation(taskId: number, comment?: string) {
    await resolveWorkflowTask(taskId, comment?.trim() || undefined)
    ElMessage.success('委派已完成，任务已归还原办理人')
    await load()
  }

  function openCopy(task: WorkflowTask) {
    currentTask.value = task
    Object.assign(copyForm, { receiverIds: [], comment: '' })
    copyVisible.value = true
  }

  async function submitCopy() {
    if (!currentTask.value?.taskId || copyForm.receiverIds.length === 0) {
      ElMessage.warning('请选择抄送用户')
      return false
    }
    await copyWorkflowTask(currentTask.value.taskId, {
      receiverIds: uniqueReceiverIds(copyForm.receiverIds),
      comment: copyForm.comment.trim() || undefined,
    })
    ElMessage.success('任务已抄送')
    copyVisible.value = false
    return true
  }

  async function markRead(copyId: number) {
    await markWorkflowTaskCopyRead(copyId)
    ElMessage.success('已标记为已读')
    await load()
  }

  function resetQuery() {
    Object.assign(query, {
      pageNum: 1,
      pageSize: 10,
      nodeName: undefined,
      action: undefined,
      readStatus: undefined,
    })
    void load()
  }

  return {
    loading,
    activeTab,
    tasks,
    users,
    total,
    detailVisible,
    detail,
    transferVisible,
    delegateVisible,
    copyVisible,
    currentTask,
    query,
    transferForm,
    delegateForm,
    copyForm,
    load,
    loadUsers,
    changeTab,
    openDetail,
    action,
    openTransfer,
    submitTransfer,
    openDelegate,
    submitDelegate,
    resolveDelegation,
    openCopy,
    submitCopy,
    markRead,
    resetQuery,
  }
}
