<template>
  <el-header class="flex items-center justify-between border-b border-gray-200">
    <span class="text-base">后台管理系统</span>
    <el-dropdown @command="handleCommand">
      <span class="flex items-center cursor-pointer">
        {{ userStore.userInfo?.nickname || '用户' }}
        <el-icon class="ml-1"><ArrowDown /></el-icon>
      </span>
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
