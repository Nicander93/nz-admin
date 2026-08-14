<template>
  <el-header class="app-header">
    <el-scrollbar class="app-top-menu-scroll">
      <el-menu
        class="app-top-menu"
        mode="horizontal"
        aria-label="一级菜单"
        :ellipsis="false"
        :default-active="activeTopIndex"
        @select="handleTopSelect"
      >
        <el-menu-item
          v-for="menu in topMenus"
          :key="menu.id"
          :index="buildPath(menu.path, '')"
        >
          <el-icon>
            <component :is="resolveIcon(menu.meta?.icon)" />
          </el-icon>
          <span>{{ menu.meta?.title || menu.name }}</span>
        </el-menu-item>
      </el-menu>
    </el-scrollbar>
    <div class="app-header__actions">
      <el-badge
        v-if="canViewMessages"
        :value="unreadCount"
        :hidden="unreadCount === 0"
        :max="99"
      >
        <button type="button" class="app-message-trigger" aria-label="打开消息中心" @click="openMessages">
          <el-icon><Bell /></el-icon>
        </button>
      </el-badge>
      <el-dropdown @command="handleCommand">
        <button type="button" class="app-user-trigger" aria-label="打开用户菜单">
          <span>{{ userStore.userInfo?.nickname || '用户' }}</span>
          <el-icon><ArrowDown /></el-icon>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ArrowDown, Bell, Menu as DefaultMenuIcon } from '@element-plus/icons-vue'
import * as Icons from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { logout as apiLogout } from '@/api/system/user'
import { getUnreadMessageCount } from '@/api/system/message'
import { useRoute, useRouter } from 'vue-router'
import type { MenuRouteItem } from '@/utils/routeHelper'
import { buildPath, resolveFirstRoutePath } from '@/utils/routeHelper'

const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const topMenus = computed(() => userStore.menus as MenuRouteItem[])
const unreadCount = ref(0)
const canViewMessages = computed(() => userStore.hasPermission('system:message:list'))
let unreadTimer: ReturnType<typeof setInterval> | undefined
const activeTopIndex = computed(() => {
  const active = topMenus.value.find((menu) => isActiveTopMenu(menu))
  return active ? buildPath(active.path, '') : ''
})

function isActiveTopMenu(menu: MenuRouteItem): boolean {
  const menuPath = buildPath(menu.path, '')
  if (route.path === menuPath) return true
  return route.path.startsWith(`${menuPath}/`)
}

function resolveIcon(iconName?: unknown) {
  if (typeof iconName === 'string' && iconName && iconName in Icons) {
    return Icons[iconName as keyof typeof Icons]
  }
  return DefaultMenuIcon
}

function handleTopSelect(index: string) {
  const menu = topMenus.value.find((item) => buildPath(item.path, '') === index)
  if (!menu) return
  const targetPath = resolveFirstRoutePath([menu])
  if (targetPath && targetPath !== route.path) {
    router.push(targetPath)
  }
}

async function handleCommand(command: string) {
  if (command === 'profile') {
    await router.push('/profile')
    return
  }
  if (command === 'logout') {
    await apiLogout()
    userStore.logout()
    router.push('/login')
  }
}

async function refreshUnreadCount() {
  if (!canViewMessages.value) return
  try {
    const response = await getUnreadMessageCount()
    unreadCount.value = response.data ?? 0
  } catch {
    // 全局请求层负责错误提示，角标刷新失败不影响页面操作。
  }
}

function openMessages() {
  void router.push('/system/message')
}

onMounted(() => {
  void refreshUnreadCount()
  unreadTimer = setInterval(refreshUnreadCount, 60_000)
  window.addEventListener('message-unread-changed', refreshUnreadCount)
})

onBeforeUnmount(() => {
  if (unreadTimer) clearInterval(unreadTimer)
  window.removeEventListener('message-unread-changed', refreshUnreadCount)
})
</script>

<style scoped>
.app-message-trigger { display: inline-flex; align-items: center; justify-content: center; width: 34px; height: 34px; padding: 0; border: 0; border-radius: 50%; color: inherit; background: transparent; cursor: pointer; }
.app-message-trigger:hover { background: var(--el-fill-color-light); }
</style>
