import { ref, reactive, onMounted } from 'vue'
import { getMonitorSummary, type MonitorSummary } from '@/api/system/monitor'

function formatBytes(n: number) {
  if (n <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let v = n
  let i = 0
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return `${v.toFixed(i === 0 ? 0 : 1)} ${units[i]}`
}

function formatDuration(ms: number) {
  const s = Math.floor(ms / 1000)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  if (h > 0) return `${h} 小时 ${m} 分`
  if (m > 0) return `${m} 分 ${sec} 秒`
  return `${sec} 秒`
}

/** 运行状态：JVM 与数据库连通性。 */
export function useMonitorSummary() {
  const loading = ref(false)
  const summary = ref<MonitorSummary | null>(null)

  async function load() {
    loading.value = true
    try {
      const res = await getMonitorSummary()
      summary.value = res.data
    } finally {
      loading.value = false
    }
  }

  onMounted(load)

  const table = reactive({
    loading,
    summary,
    load,
    formatBytes,
    formatDuration,
  })

  const actions = reactive({
    refresh: load,
  })

  return { table, actions }
}
