import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'

import {
  actionEngineTask,
  getEngineDefinition,
  engineCapabilities,
  getEngineInstance,
  importEngineDefinition,
  publishEngineDefinition,
  startEngineInstance,
  type EngineSnapshot,
} from '@/api/workflow/engine'
import { useUserStore } from '@/stores/user'

/** 重试保留同一业务键；明确成功后才允许下一次请求使用新键。 */
export function useEngineWorkbench() {
  const store = useUserStore()
  const enabled = ref(false)
  const busy = ref(false)
  const definitionId = ref('')
  const instanceId = ref('')
  const businessId = ref('')
  const flowCode = ref('new_approval')
  const variablesJson = ref('{}')
  const comment = ref('')
  const snapshot = ref<EngineSnapshot>()
  const modelJson = ref(
    JSON.stringify(
      {
        flowCode: 'new_approval',
        flowName: '新审批流程',
        nodeList: [
          {
            nodeCode: 'start',
            nodeName: '开始',
            nodeType: 0,
            skipList: [{ nextNodeCode: 'review', skipType: 'PASS' }],
          },
          {
            nodeCode: 'review',
            nodeName: '审核',
            nodeType: 1,
            permissionFlag: `user:${store.userInfo?.id ?? 1}`,
            skipList: [{ nextNodeCode: 'end', skipType: 'PASS' }],
          },
          { nodeCode: 'end', nodeName: '结束', nodeType: 2 },
        ],
      },
      null,
      2,
    ),
  )
  // 请求失败可能已经提交；相同内容的下一次重试复用原键。
  const keys = new Map<string, string>()
  function keyFor(path: string, body: unknown) {
    const fingerprint = JSON.stringify([path, body])
    let key = keys.get(fingerprint)
    if (!key) {
      key = crypto.randomUUID()
      keys.set(fingerprint, key)
    }
    return key
  }
  function variables(): Record<string, unknown> {
    const value: unknown = JSON.parse(variablesJson.value)
    if (!value || typeof value !== 'object' || Array.isArray(value))
      throw new Error('流程变量须为 JSON 对象')
    return value as Record<string, unknown>
  }
  async function run(action: () => Promise<void>) {
    busy.value = true
    try {
      await action()
    } catch (error) {
      if (
        error instanceof SyntaxError ||
        (error instanceof Error && error.message === '流程变量须为 JSON 对象')
      )
        ElMessage.error(error.message)
    } finally {
      busy.value = false
    }
  }
  async function refresh() {
    if (instanceId.value)
      snapshot.value = (await getEngineInstance(instanceId.value)).data
  }
  const importDefinition = () =>
    run(async () => {
      const model = JSON.parse(modelJson.value) as { flowCode: string }
      definitionId.value = (
        await importEngineDefinition(model, keyFor('import', modelJson.value))
      ).data
      flowCode.value = model.flowCode
      ElMessage.success('已导入新版本，发布后可发起')
    })
  const loadDefinition = () =>
    run(async () => {
      modelJson.value = JSON.stringify(
        (await getEngineDefinition(definitionId.value)).data,
        null,
        2,
      )
    })
  const publish = () =>
    run(async () => {
      await publishEngineDefinition(definitionId.value)
      ElMessage.success('发布成功')
    })
  const start = () =>
    run(async () => {
      const body = {
        flowCode: flowCode.value,
        businessId: businessId.value,
        variables: variables(),
      }
      instanceId.value = (
        await startEngineInstance(body, keyFor('start', body))
      ).data
      await refresh()
    })
  const action = (id: string, type: 'PASS' | 'REJECT') =>
    run(async () => {
      const body = { type, comment: comment.value, variables: variables() }
      await actionEngineTask(id, body, keyFor(id, body))
      await refresh()
    })
  const load = () => run(refresh)
  onMounted(() =>
    run(async () => {
      enabled.value = (await engineCapabilities()).data.enabled
    }),
  )
  return {
    enabled,
    busy,
    definitionId,
    instanceId,
    businessId,
    flowCode,
    variablesJson,
    comment,
    snapshot,
    modelJson,
    importDefinition,
    loadDefinition,
    publish,
    start,
    action,
    load,
  }
}
