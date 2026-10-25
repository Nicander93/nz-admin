<template>
  <main class="auth-unavailable">
    <el-result
      icon="warning"
      title="暂时无法加载账户信息"
      sub-title="请检查网络后重试，或重新登录。"
    >
      <template #extra>
        <el-button type="primary" @click="retry">重试</el-button>
        <el-button @click="loginAgain">重新登录</el-button>
      </template>
    </el-result>
  </main>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

function retry() {
  void router.replace('/')
}

function loginAgain() {
  userStore.logout()
  void router.replace('/login')
}
</script>

<style scoped>
.auth-unavailable {
  display: grid;
  min-height: 100vh;
  place-items: center;
}
</style>
