<template>
  <div class="auth-shell">
    <el-card class="auth-panel" shadow="never">
      <template #header>
        <div class="auth-header">
          <p class="auth-eyebrow">NZ Admin</p>
          <h1 class="auth-title">登录后台控制台</h1>
          <p class="auth-caption">使用账号密码或已绑定手机号安全登录。</p>
        </div>
      </template>
      <el-tabs v-model="mode" stretch class="auth-tabs">
        <el-tab-pane label="账号登录" name="account" />
        <el-tab-pane label="短信登录" name="sms" />
      </el-tabs>
      <el-form
        :model="form"
        label-position="top"
        class="auth-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="租户编码">
          <el-input
            v-model="form.tenantCode"
            name="tenant"
            autocomplete="organization"
            placeholder="请输入租户编码"
            spellcheck="false"
          />
        </el-form-item>
        <template v-if="mode === 'account'">
          <el-form-item label="用户名">
            <el-input
              v-model="form.username"
              name="username"
              autocomplete="username"
              placeholder="请输入用户名"
              spellcheck="false"
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="form.password"
              name="current-password"
              type="password"
              autocomplete="current-password"
              placeholder="请输入密码"
              show-password
            />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="手机号">
            <el-input
              v-model="form.phone"
              name="phone"
              autocomplete="tel"
              placeholder="请输入已绑定手机号"
            />
          </el-form-item>
          <el-form-item label="验证码">
            <el-input
              v-model="form.code"
              name="one-time-code"
              autocomplete="one-time-code"
              maxlength="8"
              placeholder="请输入短信验证码"
            >
              <template #append>
                <el-button
                  :disabled="countdown > 0"
                  :loading="sendingCode"
                  @click="handleSendCode"
                >
                  {{ countdown > 0 ? `${countdown} 秒后重试` : '获取验证码' }}
                </el-button>
              </template>
            </el-input>
          </el-form-item>
        </template>
        <el-form-item>
          <el-button
            type="primary"
            class="w-full auth-submit"
            :loading="loading"
            native-type="submit"
          >
            {{ mode === 'sms' ? '短信登录' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
      <div v-if="socialProviders.length" class="social-login">
        <el-divider>第三方账号登录</el-divider>
        <div class="social-login__buttons">
          <el-button
            v-for="provider in socialProviders"
            :key="provider.code"
            :loading="socialAuthorizing === provider.code"
            @click="handleSocialLogin(provider.code)"
          >
            {{ provider.displayName }}
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login, sendSmsLoginCode, smsLogin } from '@/api/system/user'
import {
  authorizeSocialLogin,
  getSocialProviders,
  type SocialProvider,
} from '@/api/system/social'
import { ElMessage } from 'element-plus'
import {
  SMS_CLIENT_ID,
  SOCIAL_CLIENT_ID,
  accountLoginPayload,
  smsLoginPayload,
  validateLoginForm,
  validateSmsCodeRequest,
  type LoginMode,
} from './hooks'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const socialProviders = ref<SocialProvider[]>([])
const socialAuthorizing = ref('')
const sendingCode = ref(false)
const countdown = ref(0)
const mode = ref<LoginMode>('account')
let countdownTimer: number | undefined
const form = reactive({
  tenantCode: localStorage.getItem('tenantCode') || 'default',
  username: '',
  password: '',
  phone: '',
  code: '',
})

async function handleLogin() {
  const validationMessage = validateLoginForm(mode.value, form)
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }
  loading.value = true
  try {
    const res = mode.value === 'account'
      ? await login(accountLoginPayload(form))
      : await smsLogin(smsLoginPayload(form))
    userStore.setToken(res.data)
    localStorage.setItem('tenantCode', form.tenantCode.trim())
    await userStore.initAuthData()
    await router.push('/')
    ElMessage.success('登录成功')
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleSendCode() {
  const validationMessage = validateSmsCodeRequest(form)
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }
  sendingCode.value = true
  try {
    await sendSmsLoginCode({
      tenantCode: form.tenantCode.trim(),
      clientId: SMS_CLIENT_ID,
      phone: form.phone.trim(),
    })
    startCountdown()
    ElMessage.success('验证码已发送')
  } catch {
    // handled by interceptor
  } finally {
    sendingCode.value = false
  }
}

async function loadSocialProviders() {
  try {
    const response = await getSocialProviders()
    socialProviders.value = response.data || []
  } catch {
    socialProviders.value = []
  }
}

async function handleSocialLogin(provider: string) {
  const tenantCode = form.tenantCode.trim()
  if (!tenantCode) {
    ElMessage.warning('请输入租户编码')
    return
  }
  socialAuthorizing.value = provider
  try {
    localStorage.setItem('tenantCode', tenantCode)
    const response = await authorizeSocialLogin({
      tenantCode,
      clientId: SOCIAL_CLIENT_ID,
      provider,
    })
    window.location.assign(response.data.authorizeUrl)
  } finally {
    socialAuthorizing.value = ''
  }
}


function startCountdown() {
  countdown.value = 60
  window.clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(countdownTimer)
      countdownTimer = undefined
    }
  }, 1000)
}

onMounted(loadSocialProviders)
onBeforeUnmount(() => window.clearInterval(countdownTimer))
</script>
