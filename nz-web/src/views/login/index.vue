<template>
  <div class="auth-shell">
    <el-card class="auth-panel" shadow="never">
      <template #header>
        <div class="auth-header">
          <p class="auth-eyebrow">NZ Admin</p>
          <h1 class="auth-title">登录后台控制台</h1>
          <p class="auth-caption">统一管理用户、角色、部门、菜单与字典配置。</p>
        </div>
      </template>
      <el-form
        :model="form"
        label-position="top"
        class="auth-form"
        @submit.prevent="handleLogin"
      >
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
        <el-form-item>
          <el-button
            type="primary"
            class="w-full auth-submit"
            :loading="loading"
            native-type="submit"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/system/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function handleLogin() {
  loading.value = true
  try {
    const res = await login(form)
    userStore.setToken(res.data)
    await userStore.initAuthData()
    router.push('/')
    ElMessage.success('登录成功')
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>
