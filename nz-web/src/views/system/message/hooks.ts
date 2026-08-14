import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteMessage,
  getMessage,
  markAllMessagesRead,
  markMessageRead,
  pageMessages,
  sendMessage,
  type MessageQuery,
  type MessageSendRequest,
  type SystemMessage,
} from '@/api/system/message'
import { pageUsers, type SysUser } from '@/api/system/user'

export const messageCategoryLabels = {
  system: '系统',
  notice: '通知',
  workflow: '流程',
} as const

export function validateMessageSendForm(form: MessageSendRequest): string | null {
  if (!form.title.trim()) return '请输入消息标题'
  if (!form.content.trim()) return '请输入消息内容'
  if (form.targetType === 'USERS' && form.userIds.length === 0) {
    return '请选择接收用户'
  }
  if (form.path && (!form.path.startsWith('/') || form.path.startsWith('//'))) {
    return '跳转路径必须是站内绝对路径'
  }
  if (form.dataJson) {
    try {
      JSON.parse(form.dataJson)
    } catch {
      return '扩展数据必须是有效 JSON'
    }
  }
  return null
}

function newSendForm(): MessageSendRequest {
  return {
    category: 'notice',
    title: '',
    content: '',
    targetType: 'ALL',
    userIds: [],
  }
}

/** 消息收件箱、已读处理和管理员发送。 */
export function useMessageCenter() {
  const loading = ref(false)
  const rows = ref<SystemMessage[]>([])
  const total = ref(0)
  const query = reactive<MessageQuery>({
    pageNum: 1,
    pageSize: 10,
    category: '',
    title: '',
    readStatus: undefined,
  })
  const detail = reactive<{ visible: boolean; loading: boolean; data?: SystemMessage }>({
    visible: false,
    loading: false,
  })
  const sendDialog = reactive({ visible: false, loading: false })
  const sendForm = reactive<MessageSendRequest>(newSendForm())
  const userOptions = ref<SysUser[]>([])

  async function load() {
    loading.value = true
    try {
      const response = await pageMessages({ ...query })
      rows.value = response.data.records ?? []
      total.value = response.data.total ?? 0
    } finally {
      loading.value = false
    }
  }

  async function reset() {
    query.category = ''
    query.title = ''
    query.readStatus = undefined
    query.pageNum = 1
    await load()
  }

  async function openDetail(row: SystemMessage) {
    detail.visible = true
    detail.loading = true
    try {
      const response = await getMessage(row.id)
      detail.data = response.data
      if (response.data.readStatus === 0) {
        await markMessageRead(row.id)
        detail.data = { ...response.data, readStatus: 1 }
        notifyUnreadChanged()
        await load()
      }
    } finally {
      detail.loading = false
    }
  }

  async function read(row: SystemMessage) {
    if (row.readStatus === 1) return
    await markMessageRead(row.id)
    ElMessage.success('消息已标记为已读')
    notifyUnreadChanged()
    await load()
  }

  async function readAll() {
    const response = await markAllMessagesRead()
    ElMessage.success(`已标记 ${response.data ?? 0} 条消息`)
    notifyUnreadChanged()
    await load()
  }

  async function remove(row: SystemMessage) {
    await ElMessageBox.confirm(`确认删除“${row.title}”吗？`, '删除消息', {
      type: 'warning',
    })
    await deleteMessage(row.id)
    ElMessage.success('消息已删除')
    notifyUnreadChanged()
    await load()
  }

  async function openSend() {
    Object.assign(sendForm, newSendForm())
    sendDialog.visible = true
    const response = await pageUsers({ pageNum: 1, pageSize: 500, status: 0 })
    userOptions.value = response.data.records ?? []
  }

  async function submitSend() {
    const error = validateMessageSendForm(sendForm)
    if (error) {
      ElMessage.warning(error)
      return
    }
    sendDialog.loading = true
    try {
      const response = await sendMessage({
        ...sendForm,
        userIds: sendForm.targetType === 'USERS' ? sendForm.userIds : [],
      })
      ElMessage.success(`已发送给 ${response.data} 位用户`)
      sendDialog.visible = false
      notifyUnreadChanged()
      await load()
    } finally {
      sendDialog.loading = false
    }
  }

  function notifyUnreadChanged() {
    window.dispatchEvent(new CustomEvent('message-unread-changed'))
  }

  return {
    table: reactive({ loading, rows, total, query, load, reset }),
    detail,
    sendDialog,
    sendForm,
    userOptions,
    actions: reactive({ openDetail, read, readAll, remove, openSend, submitSend }),
  }
}
