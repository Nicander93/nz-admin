import { ref, reactive, onMounted } from 'vue'
import { getWorkbenchSnapshot, type WorkbenchSnapshot } from '@/api/system/workbench'

const SHORTCUTS = [
  { title: '用户管理', path: '/system/user' },
  { title: '角色管理', path: '/system/role' },
  { title: '菜单管理', path: '/system/menu' },
  { title: '部门管理', path: '/system/dept' },
  { title: '字典管理', path: '/system/dict' },
  { title: '定时任务', path: '/monitor/job' },
] as const

/** 工作台：聚合快照与快捷入口。 */
export function useWorkbenchHome() {
  const loading = ref(false)
  const snapshot = ref<WorkbenchSnapshot | null>(null)

  async function load() {
    loading.value = true
    try {
      const res = await getWorkbenchSnapshot()
      snapshot.value = res.data
    } finally {
      loading.value = false
    }
  }

  onMounted(load)

  const table = reactive({
    loading,
    snapshot,
    load,
  })

  const actions = reactive({
    refresh: load,
  })

  return {
    table,
    actions,
    shortcuts: SHORTCUTS,
  }
}
