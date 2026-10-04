import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import {
  listLeaveFlows,
  listLeaveApplications,
  saveLeaveApplication,
  submitLeaveApplication,
  type LeaveApplication,
  type LeaveDraft,
} from '@/api/demo/leave'

export function useLeaveApplications() {
  const router = useRouter()
  const busy = ref(false)
  const rows = ref<LeaveApplication[]>([])
  const definitions = ref<{ flowCode: string; flowName: string }[]>([])
  const editing = ref(false)
  const id = ref<string>()
  const draft = ref<LeaveDraft>({
    reason: '',
    startDate: '',
    endDate: '',
    flowCode: '',
  })
  const keys = new Map<string, string>()
  function keyFor(value: unknown) {
    const fingerprint = JSON.stringify(value)
    if (!keys.has(fingerprint)) keys.set(fingerprint, crypto.randomUUID())
    return keys.get(fingerprint)!
  }
  async function run(action: () => Promise<void>) {
    busy.value = true
    try {
      await action()
    } finally {
      busy.value = false
    }
  }
  async function refresh() {
    rows.value = (await listLeaveApplications()).data
  }
  function edit(row?: LeaveApplication) {
    id.value = row?.id
    draft.value = row
      ? {
          reason: row.reason,
          startDate: row.startDate,
          endDate: row.endDate,
          flowCode: row.flowCode,
        }
      : {
          reason: '',
          startDate: '',
          endDate: '',
          flowCode: definitions.value[0]?.flowCode ?? '',
        }
    editing.value = true
  }
  const save = () =>
    run(async () => {
      await saveLeaveApplication(
        draft.value,
        keyFor(['draft', id.value, draft.value]),
        id.value,
      )
      keys.delete(JSON.stringify(['draft', id.value, draft.value]))
      editing.value = false
      await refresh()
      ElMessage.success('草稿已保存')
    })
  const submit = (row: LeaveApplication) =>
    run(async () => {
      const fingerprint = [
        'submit',
        row.id,
        row.status,
        row.reason,
        row.startDate,
        row.endDate,
      ]
      await submitLeaveApplication(row.id, keyFor(fingerprint))
      keys.delete(JSON.stringify(fingerprint))
      await refresh()
      ElMessage.success('申请已提交，可在流程中心查看进度')
    })
  const detail = (row: LeaveApplication) =>
    router.push({
      path: '/workflow/engine',
      query: { instance: row.instanceId },
    })
  onMounted(() =>
    run(async () => {
      await refresh()
      definitions.value = (await listLeaveFlows()).data
    }),
  )
  return {
    busy,
    rows,
    definitions,
    editing,
    draft,
    edit,
    save,
    submit,
    detail,
    refresh,
  }
}
