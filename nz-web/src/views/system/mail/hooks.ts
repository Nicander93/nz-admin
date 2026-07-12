import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { sendTestMail } from '@/api/system/mail'

export function useMailTest() {
  const loading = ref(false)
  const form = reactive({
    to: '',
    subject: 'nz-admin 邮件测试',
    content: '邮件能力配置成功。',
    html: false,
  })

  async function send() {
    loading.value = true
    try {
      await sendTestMail({ ...form })
      ElMessage.success('邮件发送成功')
    } finally {
      loading.value = false
    }
  }

  return { form, loading, send }
}