import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { computed, getCurrentInstance, onMounted, onScopeDispose, reactive, ref, watch } from 'vue'

import {
  createJob,
  deleteJob,
  getJob,
  pageExecutions,
  pageJobLogs,
  pageJobs,
  pageScheduleOutbox,
  pauseJob,
  resumeJob,
  retryExecution,
  retryScheduleOutbox,
  runJob,
  updateJob,
  type JobExecution,
  type JobLog,
  type JobRow,
  type JobWriteInput,
  type ScheduleOutbox,
} from '@/api/job'
import { useUserStore } from '@/stores/user'

function emptyForm(): JobWriteInput {
  return {
    jobName: '',
    jobGroup: 'DEFAULT',
    invokeTarget: 'demoTask.run',
    cronExpression: '0 0 0 1 1 ? 2099',
    status: 0,
    concurrent: 1,
    misfirePolicy: 3,
    remark: '',
  }
}

function newIdempotencyKey() {
  return crypto.randomUUID()
}

export function canRetryExecution(execution: JobExecution) {
  return execution.state === 'FAILED' && execution.retryable === true
}

export function useJobPage() {
  const userStore = useUserStore()
  const rows = ref<JobRow[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const total = ref(0)
  const query = reactive({ jobName: '', jobGroup: '' })
  const formVisible = ref(false)
  const formRef = ref<FormInstance>()
  const editingId = ref<number>()
  const form = reactive<JobWriteInput>(emptyForm())
  const recordsVisible = ref(false)
  const recordsTab = ref<'executions' | 'logs'>('executions')
  const recordsLoading = ref(false)
  const recordsJobId = ref<number>()
  const executions = ref<JobExecution[]>([])
  const logs = ref<JobLog[]>([])
  const outboxVisible = ref(false)
  const outboxLoading = ref(false)
  const outboxRows = ref<ScheduleOutbox[]>([])
  const outboxTotal = ref(0)
  const outboxPageNum = ref(1)
  const canEditJob = computed(() =>
    userStore.hasPermission('system:job:edit') && userStore.hasPermission('system:job:query'),
  )
  const schedulePending = computed(() => rows.value.some((row) => row.scheduleState === 'PENDING'))
  const runKeys = new Map<number, string>()
  let loadSequence = 0
  let editSequence = 0
  let recordSequence = 0
  let outboxSequence = 0
  let identitySequence = 0
  let recordsRefreshTimer: ReturnType<typeof setInterval> | undefined
  let scheduleRefreshTimer: ReturnType<typeof setInterval> | undefined
  let outboxRefreshTimer: ReturnType<typeof setInterval> | undefined
  let disposed = false

  function isCurrentIdentity(sequence: number) {
    return !disposed && sequence === identitySequence
  }

  watch(() => userStore.token, () => {
    identitySequence += 1
    loadSequence += 1
    editSequence += 1
    recordSequence += 1
    outboxSequence += 1
    runKeys.clear()
    if (recordsRefreshTimer) clearInterval(recordsRefreshTimer)
    if (scheduleRefreshTimer) clearInterval(scheduleRefreshTimer)
    if (outboxRefreshTimer) clearInterval(outboxRefreshTimer)
    recordsRefreshTimer = undefined
    scheduleRefreshTimer = undefined
    outboxRefreshTimer = undefined
    rows.value = []
    executions.value = []
    logs.value = []
    outboxRows.value = []
    formVisible.value = false
    editingId.value = undefined
    Object.assign(form, emptyForm())
    recordsVisible.value = false
    recordsJobId.value = undefined
    outboxPageNum.value = 1
    outboxVisible.value = false
    loading.value = false
    saving.value = false
    recordsLoading.value = false
    outboxLoading.value = false
    total.value = 0
    outboxTotal.value = 0
  }, { flush: 'sync' })

  async function load() {
    if (disposed) return
    const identity = identitySequence
    const sequence = ++loadSequence
    loading.value = true
    try {
      const result = await pageJobs({ ...query, pageNum: pageNum.value, pageSize: pageSize.value })
      if (!isCurrentIdentity(identity) || sequence !== loadSequence) return
      rows.value = result.data.records
      total.value = result.data.total
      if (schedulePending.value && !scheduleRefreshTimer) {
        scheduleRefreshTimer = setInterval(() => void load(), 1500)
      } else if (!schedulePending.value && scheduleRefreshTimer) {
        clearInterval(scheduleRefreshTimer)
        scheduleRefreshTimer = undefined
      }
    } finally {
      if (sequence === loadSequence) loading.value = false
    }
  }

  function openCreate() {
    if (disposed) return
    editSequence += 1
    editingId.value = undefined
    Object.assign(form, emptyForm())
    formVisible.value = true
  }

  async function openEdit(id: number) {
    if (disposed) return
    const identity = identitySequence
    const sequence = ++editSequence
    const result = await getJob(id)
    if (!isCurrentIdentity(identity) || sequence !== editSequence) return
    editingId.value = id
    Object.assign(form, emptyForm(), {
      jobName: result.data.jobName,
      jobGroup: result.data.jobGroup,
      invokeTarget: result.data.invokeTarget,
      cronExpression: result.data.cronExpression,
      status: result.data.status,
      concurrent: result.data.concurrent ?? 1,
      misfirePolicy: result.data.misfirePolicy ?? 3,
      remark: result.data.remark ?? '',
    })
    formVisible.value = true
  }

  async function save() {
    if (disposed || !formRef.value || saving.value) return
    const identity = identitySequence
    const formSequence = editSequence
    const targetId = editingId.value
    await formRef.value.validate()
    if (!isCurrentIdentity(identity) || formSequence !== editSequence || targetId !== editingId.value) return
    saving.value = true
    try {
      const payload = { ...form, ...(targetId ? { id: targetId } : {}) }
      const result = targetId
        ? await updateJob(payload as JobWriteInput & { id: number })
        : await createJob(payload)
      if (!isCurrentIdentity(identity) || formSequence !== editSequence || targetId !== editingId.value) return
      formVisible.value = false
      ElMessage.success(result.data.scheduleState === 'SYNCED'
        ? 'Job saved and scheduled'
        : 'Job saved; schedule sync pending')
      await load()
    } finally {
      if (isCurrentIdentity(identity)) saving.value = false
    }
  }

  async function setPaused(row: JobRow, paused: boolean) {
    if (disposed) return
    const identity = identitySequence
    const result = paused ? await pauseJob(row.id) : await resumeJob(row.id)
    if (!isCurrentIdentity(identity)) return
    ElMessage.success(result.data.scheduleState === 'SYNCED'
      ? (paused ? 'Job paused' : 'Job resumed')
      : 'Job updated; schedule sync pending')
    await load()
  }

  async function execute(row: JobRow) {
    if (disposed) return
    const identity = identitySequence
    const key = runKeys.get(row.id) ?? newIdempotencyKey()
    runKeys.set(row.id, key)
    const result = await runJob(row.id, key)
    if (!isCurrentIdentity(identity)) return
    if (runKeys.get(row.id) === key) runKeys.delete(row.id)
    ElMessage.success(`Execution submitted: ${result.data.executionKey}`)
    await openRecords(row.id)
  }

  async function remove(row: JobRow) {
    if (disposed) return
    const identity = identitySequence
    await ElMessageBox.confirm(`Delete job “${row.jobName}”?`, 'Delete job', { type: 'warning' })
    if (!isCurrentIdentity(identity)) return
    const result = await deleteJob(row.id)
    if (!isCurrentIdentity(identity)) return
    ElMessage.success(result.data.scheduleState === 'SYNCED'
      ? 'Job deleted' : 'Job deleted; schedule removal is pending')
    await load()
  }

  async function refreshRecords() {
    if (disposed) return
    const identity = identitySequence
    const jobId = recordsJobId.value
    if (!jobId) return
    const sequence = ++recordSequence
    recordsLoading.value = true
    try {
      const [executionResult, logResult] = await Promise.all([
        pageExecutions({ jobId, pageNum: 1, pageSize: 100 }),
        pageJobLogs({ jobId, pageNum: 1, pageSize: 100 }),
      ])
      if (!isCurrentIdentity(identity) || sequence !== recordSequence || recordsJobId.value !== jobId) return
      executions.value = executionResult.data.records
      logs.value = logResult.data.records
    } finally {
      if (sequence === recordSequence) recordsLoading.value = false
    }
  }

  async function openRecords(jobId: number) {
    if (disposed) return
    recordsJobId.value = jobId
    recordsTab.value = 'executions'
    recordsVisible.value = true
    stopRecordsRefresh()
    await refreshRecords()
    if (!disposed && recordsVisible.value && recordsJobId.value === jobId) {
      recordsRefreshTimer = setInterval(() => void refreshRecords(), 1500)
    }
  }

  function stopRecordsRefresh() {
    recordSequence += 1
    if (recordsRefreshTimer) clearInterval(recordsRefreshTimer)
    recordsRefreshTimer = undefined
    recordsLoading.value = false
  }

  async function retryFailedExecution(execution: JobExecution) {
    if (disposed || !canRetryExecution(execution)) return
    const identity = identitySequence
    const receipt = await retryExecution(execution.id)
    if (!isCurrentIdentity(identity)) return
    ElMessage.success(`Retry submitted: ${receipt.data.executionKey} (${receipt.data.state})`)
    await refreshRecords()
  }

  async function loadOutbox() {
    if (disposed) return
    const identity = identitySequence
    const sequence = ++outboxSequence
    outboxLoading.value = true
    try {
      const result = await pageScheduleOutbox({ pageNum: outboxPageNum.value, pageSize: 20 })
      if (!isCurrentIdentity(identity) || sequence !== outboxSequence) return
      outboxRows.value = result.data.records
      outboxTotal.value = result.data.total
      const hasPending = outboxRows.value.some((row) => row.state === 'PENDING')
      if (hasPending && outboxVisible.value && !outboxRefreshTimer) {
        outboxRefreshTimer = setInterval(() => void loadOutbox(), 1500)
      } else if ((!hasPending || !outboxVisible.value) && outboxRefreshTimer) {
        clearInterval(outboxRefreshTimer)
        outboxRefreshTimer = undefined
      }
    } finally {
      if (sequence === outboxSequence) outboxLoading.value = false
    }
  }

  async function openOutbox() {
    if (disposed) return
    outboxVisible.value = true
    await loadOutbox()
  }

  function stopOutboxRefresh() {
    outboxSequence += 1
    if (outboxRefreshTimer) clearInterval(outboxRefreshTimer)
    outboxRefreshTimer = undefined
    outboxLoading.value = false
  }

  async function retryOutbox(row: ScheduleOutbox) {
    if (disposed || row.state !== 'ERROR') return
    const identity = identitySequence
    const result = await retryScheduleOutbox(row.id)
    if (!isCurrentIdentity(identity)) return
    ElMessage.success(`Schedule retry submitted: ${result.data.scheduleState}`)
    await Promise.all([loadOutbox(), load()])
  }

  if (getCurrentInstance()) onMounted(() => void load())
  onScopeDispose(() => {
    disposed = true
    identitySequence += 1
    loadSequence += 1
    editSequence += 1
    recordSequence += 1
    outboxSequence += 1
    if (recordsRefreshTimer) clearInterval(recordsRefreshTimer)
    if (scheduleRefreshTimer) clearInterval(scheduleRefreshTimer)
    if (outboxRefreshTimer) clearInterval(outboxRefreshTimer)
  })

  return {
    rows, loading, saving, pageNum, pageSize, total, query,
    formVisible, formRef, editingId, form,
    recordsVisible, recordsTab, recordsLoading, recordsJobId, executions, logs,
    outboxVisible, outboxLoading, outboxRows, outboxTotal, outboxPageNum,
    schedulePending,
    canEditJob,
    load, openCreate, openEdit, save, setPaused, execute, remove,
    refreshRecords, openRecords, stopRecordsRefresh, retryFailedExecution,
    loadOutbox, openOutbox, stopOutboxRefresh, retryOutbox,
  }
}
