<template>
  <el-header class="app-header">
    <div class="app-header__meta">
      <span class="app-header__title">NZ Admin</span>
      <span class="app-header__subtitle">极简淡绿色后台界面基线</span>
    </div>
    <el-dropdown @command="handleCommand">
      <button type="button" class="app-user-trigger" aria-label="打开用户菜单">
        <span>{{ userStore.userInfo?.nickname || '用户' }}</span>
        <el-icon><ArrowDown /></el-icon>
      </button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="logout">退出登录</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </el-header>
</template>

<script setup lang="ts">
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { logout as apiLogout } from '@/api/system/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

async function handleCommand(command: string) {
  if (command === 'logout') {
    await apiLogout()
    userStore.logout()
    router.push('/login')
  }
}
</script>
