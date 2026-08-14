<template>
  <div class="callback-shell">
    <el-card class="callback-card" shadow="never">
      <el-result
        :icon="status === 'error' ? 'error' : 'info'"
        :title="title"
        :sub-title="description"
      >
        <template v-if="status === 'error'" #extra>
          <el-button type="primary" @click="returnToSafePage">返回</el-button>
        </template>
      </el-result>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { completeSocialCallback } from '@/api/system/social'
import { useUserStore } from '@/stores/user'
import { parseSocialCallbackQuery } from './hooks'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const status = ref<'loading' | 'error'>('loading')
const description = ref('正在校验第三方授权，请稍候。')
const title = computed(() =>
  status.value === 'error' ? '授权未完成' : '正在完成授权',
)

onMounted(async () => {
  const parsed = parseSocialCallbackQuery(route.params.provider, route.query)
  if (!parsed.ok) {
    fail(parsed.error)
    return
  }
  try {
    const response = await completeSocialCallback(parsed.data)
    if (response.data.purpose === 'LOGIN' && response.data.token) {
      userStore.setToken(response.data.token)
      await userStore.initAuthData()
      ElMessage.success('第三方账号登录成功')
      await router.replace('/')
      return
    }
    ElMessage.success('第三方账号绑定成功')
    await router.replace('/system/social')
  } catch {
    fail('授权状态可能已过期，请返回后重新操作。')
  }
})

function fail(message: string) {
  status.value = 'error'
  description.value = message
}

function returnToSafePage() {
  router.replace(localStorage.getItem('token') ? '/system/social' : '/login')
}
</script>

<style scoped>
.callback-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: var(--el-bg-color-page);
}

.callback-card {
  width: min(520px, 100%);
}
</style>
