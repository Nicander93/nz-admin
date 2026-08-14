import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  forceLogout as forceLogoutApi,
  listOnlineUsers,
  type OnlineUser,
  type OnlineUserQuery,
} from '@/api/system/online'

/** 在线用户列表、筛选和强制退出。 */
export function useOnlineUsers() {
  const loading = ref(false)
  const rows = ref<OnlineUser[]>([])
  const query = reactive<OnlineUserQuery>({ username: '', loginIp: '' })

  async function load() {
    loading.value = true
    try {
      const response = await listOnlineUsers({ ...query })
      rows.value = response.data ?? []
    } finally {
      loading.value = false
    }
  }

  async function reset() {
    query.username = ''
    query.loginIp = ''
    await load()
  }

  async function forceLogout(tokenValue: string) {
    await forceLogoutApi(tokenValue)
    ElMessage.success('用户已强制退出')
    await load()
  }

  function formatTimeout(seconds: number) {
    if (seconds === -1) return '永不过期'
    if (seconds <= 0) return '已过期'
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    if (hours > 0) return `${hours} 小时 ${minutes} 分`
    return `${Math.max(minutes, 1)} 分钟`
  }

  return {
    table: reactive({ rows, loading, query, load, reset, formatTimeout }),
    actions: reactive({ forceLogout }),
  }
}
