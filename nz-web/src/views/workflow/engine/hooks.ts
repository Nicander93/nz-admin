import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  actionEngineTask,
  createEngineDraft,
  engineCapabilities,
  getEngineInstance,
  listEngineCenter,
  listEngineDefinitions,
  listEngineBusinessTypes,
  publishEngineDefinition,
  manageEngineTask,
  revokeEngineInstance,
  engineReturnNodes,
  engineParticipants,
  type EngineDefinition,
  type EngineInstance,
  type EngineSnapshot,
} from '@/api/workflow/engine'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

export function useEngineWorkbench() {
  const enabled = ref(false)
  const busy = ref(false)
  const tab = ref('pending')
  const page = ref(1)
  const rows = ref<EngineInstance[]>([])
  const definitions = ref<EngineDefinition[]>([])
  const businessTypes = ref<{ code: string; name: string }[]>([])
  const selectedDefinition = ref<EngineDefinition>()
  const snapshot = ref<EngineSnapshot>()
  const detailOpen = ref(false)
  const draftOpen = ref(false)
  const draft = ref({ flowCode: '', flowName: '', businessType: '' })
  const comment = ref('')
  const managing = ref(false)
  const manage = ref({
    taskId: '',
    type: '',
    targets: [] as string[],
    nodeCode: '',
  })
  const candidates = ref<
    { storageId: string; handlerName: string; handlerCode: string }[]
  >([])
  const returnNodes = ref<{ nodeCode: string; nodeName: string }[]>([])
  const store = useUserStore()
  const route = useRoute()
  const canDesign = computed(() =>
    store.hasPermission('workflow:engine:design'),
  )
  const keys = new Map<string, string>()
  function keyFor(path: string, body: unknown) {
    const fingerprint = JSON.stringify([path, body])
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
    if (!enabled.value) return
    rows.value = (
      await listEngineCenter(
        tab.value === 'definitions' ? 'pending' : tab.value,
        page.value,
      )
    ).data
    if (store.hasPermission('workflow:engine:design')) {
      const [list, types] = await Promise.all([
        listEngineDefinitions(),
        listEngineBusinessTypes(),
      ])
      definitions.value = list.data
      businessTypes.value = types.data
    }
  }
  const create = (source?: EngineDefinition) =>
    run(async () => {
      const body = source
        ? {
            flowCode: source.flowCode,
            flowName: source.flowName,
            sourceId: source.id,
          }
        : draft.value
      const id = (await createEngineDraft(body, keyFor('draft', body))).data
      keys.delete(JSON.stringify(['draft', body]))
      draftOpen.value = false
      await refresh()
      selectedDefinition.value = definitions.value.find((v) => v.id === id)
    })
  const saved = () =>
    run(async () => {
      selectedDefinition.value = undefined
      await refresh()
      ElMessage.success('流程草稿已保存')
    })
  const publish = (definition: EngineDefinition) =>
    run(async () => {
      await ElMessageBox.confirm(
        `发布「${definition.flowName}」版本 ${definition.version}？发布后请通过新版本修改。`,
        '发布流程',
        { confirmButtonText: '确定', cancelButtonText: '取消' },
      )
      await publishEngineDefinition(definition.id)
      await refresh()
      ElMessage.success('流程已发布')
    })
  const open = (row: EngineInstance) =>
    run(async () => {
      snapshot.value = (await getEngineInstance(row.id)).data
      comment.value = ''
      detailOpen.value = true
    })
  const action = (id: string, type: 'PASS' | 'REJECT') =>
    run(async () => {
      const body = { type, comment: comment.value, variables: {} }
      await actionEngineTask(id, body, keyFor(id, body))
      keys.delete(JSON.stringify([id, body]))
      snapshot.value = (
        await getEngineInstance(snapshot.value!.instance.id)
      ).data
      await refresh()
      ElMessage.success(type === 'PASS' ? '已通过' : '已退回')
    })
  async function searchParticipants(name = '') {
    candidates.value = (await engineParticipants(name)).data.list
  }
  const prepareManage = (taskId: string, type: string) =>
    run(async () => {
      manage.value = { taskId, type, targets: [], nodeCode: '' }
      if (type === 'RETURN')
        returnNodes.value = (await engineReturnNodes(taskId)).data
      else if (type !== 'TERMINATE') await searchParticipants()
      managing.value = true
    })
  const submitManage = () =>
    run(async () => {
      const { taskId, ...body } = manage.value
      const request = { ...body, comment: comment.value }
      await manageEngineTask(
        taskId,
        request,
        keyFor(`manage:${taskId}`, request),
      )
      keys.delete(JSON.stringify([`manage:${taskId}`, request]))
      managing.value = false
      snapshot.value = (
        await getEngineInstance(snapshot.value!.instance.id)
      ).data
      await refresh()
      ElMessage.success('操作已完成')
    })
  const revoke = () =>
    run(async () => {
      await ElMessageBox.confirm(
        '撤回当前申请？审批人将不再办理当前任务。',
        '撤回申请',
        { confirmButtonText: '确定', cancelButtonText: '取消' },
      )
      const instance = snapshot.value!.instance.id
      await revokeEngineInstance(
        instance,
        comment.value,
        keyFor(`revoke:${instance}`, comment.value),
      )
      keys.delete(JSON.stringify([`revoke:${instance}`, comment.value]))
      snapshot.value = (await getEngineInstance(instance)).data
      await refresh()
      ElMessage.success('申请已撤回')
    })
  const timer = window.setInterval(() => {
    if (detailOpen.value && snapshot.value?.sync.pending && !busy.value)
      void run(async () => {
        snapshot.value = (
          await getEngineInstance(snapshot.value!.instance.id)
        ).data
      })
  }, 3000)
  onBeforeUnmount(() => window.clearInterval(timer))
  watch(tab, () => {
    page.value = 1
    void run(refresh)
  })
  watch(page, () => void run(refresh))
  onMounted(() =>
    run(async () => {
      enabled.value = (await engineCapabilities()).data.enabled
      await refresh()
      if (typeof route.query.instance === 'string') {
        snapshot.value = (await getEngineInstance(route.query.instance)).data
        detailOpen.value = true
      }
    }),
  )
  return {
    canDesign,
    enabled,
    busy,
    tab,
    page,
    rows,
    definitions,
    businessTypes,
    selectedDefinition,
    snapshot,
    detailOpen,
    draftOpen,
    draft,
    comment,
    managing,
    manage,
    candidates,
    returnNodes,
    prepareManage,
    submitManage,
    searchParticipants,
    revoke,
    create,
    saved,
    publish,
    open,
    action,
    refresh,
  }
}
