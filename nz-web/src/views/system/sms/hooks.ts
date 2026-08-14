import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addSmsChannel, addSmsTemplate, deleteSmsChannel, deleteSmsTemplate,
  pageSmsChannels, pageSmsLogs, pageSmsTemplates, sendTestSms,
  updateSmsChannel, updateSmsTemplate,
  type SmsChannel, type SmsSendLog, type SmsTemplate,
} from '@/api/system/sms'

export function parseParameters(source: string): Record<string, unknown> {
  if (!source.trim()) return {}
  const parsed: unknown = JSON.parse(source)
  if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
    throw new Error('模板参数必须是 JSON 对象')
  }
  return parsed as Record<string, unknown>
}

function createPageState<T>() {
  return reactive({ records: [] as T[], loading: false, pageNum: 1, pageSize: 10, total: 0 })
}

export function useSmsManagement() {
  const channels = createPageState<SmsChannel>()
  const templates = createPageState<SmsTemplate>()
  const logs = createPageState<SmsSendLog>()
  const channelQuery = reactive<{ keyword?: string; status?: number }>({})
  const templateQuery = reactive<{ channelId?: number; keyword?: string; status?: number }>({})
  const logQuery = reactive<{ sendStatus?: string }>({})
  const channelOptions = ref<SmsChannel[]>([])
  const channelDialog = reactive({ visible: false, editing: false })
  const channelForm = reactive<Partial<SmsChannel>>({})
  const templateDialog = reactive({ visible: false, editing: false })
  const templateForm = reactive<Partial<SmsTemplate>>({})
  const sendDialog = reactive({ visible: false, loading: false })
  const sendForm = reactive({
    templateId: undefined as number | undefined,
    phoneNumber: '',
    parametersText: '{\n  "code": "123456"\n}',
  })

  async function loadChannels() {
    channels.loading = true
    try {
      const response = await pageSmsChannels({ ...channelQuery, pageNum: channels.pageNum, pageSize: channels.pageSize })
      channels.records = response.data.records
      channels.total = response.data.total
    } finally {
      channels.loading = false
    }
  }

  async function loadChannelOptions() {
    const response = await pageSmsChannels({ pageNum: 1, pageSize: 100, status: 0 })
    channelOptions.value = response.data.records
  }

  async function loadTemplates() {
    templates.loading = true
    try {
      const response = await pageSmsTemplates({ ...templateQuery, pageNum: templates.pageNum, pageSize: templates.pageSize })
      templates.records = response.data.records
      templates.total = response.data.total
    } finally {
      templates.loading = false
    }
  }

  async function loadLogs() {
    logs.loading = true
    try {
      const response = await pageSmsLogs({ ...logQuery, pageNum: logs.pageNum, pageSize: logs.pageSize })
      logs.records = response.data.records
      logs.total = response.data.total
    } finally {
      logs.loading = false
    }
  }

  function reset(target: Record<string, unknown>) {
    Object.keys(target).forEach(key => delete target[key])
  }

  function openChannel(row?: SmsChannel) {
    reset(channelForm as Record<string, unknown>)
    Object.assign(channelForm, row
      ? { ...row, accessKeySecret: '' }
      : { channelCode: '', channelName: '', providerCode: 'log', endpoint: '', accessKeyId: '', accessKeySecret: '', signature: '', status: 0, remark: '' })
    channelDialog.editing = Boolean(row)
    channelDialog.visible = true
  }

  async function saveChannel() {
    if (channelDialog.editing) await updateSmsChannel(channelForm)
    else await addSmsChannel(channelForm)
    channelDialog.visible = false
    ElMessage.success('短信渠道已保存')
    await Promise.all([loadChannels(), loadChannelOptions()])
  }

  async function removeChannel(id: number) {
    await ElMessageBox.confirm('确认删除该短信渠道？', '提示', { type: 'warning' })
    await deleteSmsChannel(id)
    await Promise.all([loadChannels(), loadChannelOptions()])
  }

  function openTemplate(row?: SmsTemplate) {
    reset(templateForm as Record<string, unknown>)
    Object.assign(templateForm, row
      ? { ...row }
      : { channelId: channelOptions.value[0]?.id, templateCode: '', templateName: '', providerTemplateId: '', content: '', status: 0, remark: '' })
    templateDialog.editing = Boolean(row)
    templateDialog.visible = true
  }

  async function saveTemplate() {
    if (templateDialog.editing) await updateSmsTemplate(templateForm)
    else await addSmsTemplate(templateForm)
    templateDialog.visible = false
    ElMessage.success('短信模板已保存')
    await loadTemplates()
  }

  async function removeTemplate(id: number) {
    await ElMessageBox.confirm('确认删除该短信模板？', '提示', { type: 'warning' })
    await deleteSmsTemplate(id)
    await loadTemplates()
  }

  function openSend(template?: SmsTemplate) {
    sendForm.templateId = template?.id
    sendForm.phoneNumber = ''
    sendForm.parametersText = '{\n  "code": "123456"\n}'
    sendDialog.visible = true
  }

  async function send() {
    if (!sendForm.templateId) throw new Error('请选择短信模板')
    sendDialog.loading = true
    try {
      await sendTestSms({
        templateId: sendForm.templateId,
        phoneNumber: sendForm.phoneNumber,
        parameters: parseParameters(sendForm.parametersText),
      })
      sendDialog.visible = false
      ElMessage.success('短信已提交发送')
      await loadLogs()
    } finally {
      sendDialog.loading = false
    }
  }

  return {
    channels, templates, logs, channelQuery, templateQuery, logQuery, channelOptions,
    channelDialog, channelForm, templateDialog, templateForm, sendDialog, sendForm,
    loadChannels, loadChannelOptions, loadTemplates, loadLogs,
    openChannel, saveChannel, removeChannel, openTemplate, saveTemplate, removeTemplate, openSend, send,
  }
}
