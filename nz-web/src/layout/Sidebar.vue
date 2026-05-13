<template>
  <el-aside width="var(--nz-sidebar-width)" class="app-sidebar">
    <div class="app-sidebar__brand">
      <span class="app-sidebar__brand-mark">NZ</span>
      <div class="app-sidebar__brand-copy">
        <span class="app-sidebar__title">NZ Admin</span>
        <span class="app-sidebar__subtitle">System Console</span>
      </div>
    </div>
    <div class="app-sidebar__section">
      <span class="app-sidebar__section-title">{{ activeTopTitle }}</span>
    </div>
    <el-scrollbar class="flex-1">
      <el-menu
        class="app-menu"
        :default-active="route.path"
        @select="handleSelect"
      >
        <SidebarMenuItem
          v-for="menu in secondaryMenus"
          :key="menu.id"
          :menu="menu"
          :parent-path="activeTopPath"
        />
      </el-menu>
    </el-scrollbar>
  </el-aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import type { MenuRouteItem } from '@/utils/routeHelper'
import { buildPath } from '@/utils/routeHelper'
import SidebarMenuItem from '@/layout/SidebarMenuItem.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menuTree = computed(() => userStore.menus as MenuRouteItem[])
const activeTopMenu = computed(() => menuTree.value.find((menu) => isActiveTopMenu(menu)))
const activeTopPath = computed(() => activeTopMenu.value ? buildPath(activeTopMenu.value.path, '') : '')
const activeTopTitle = computed(() => activeTopMenu.value?.meta?.title || activeTopMenu.value?.name || '菜单')
const secondaryMenus = computed(() => activeTopMenu.value?.children || [])

function isActiveTopMenu(menu: MenuRouteItem): boolean {
  const menuPath = buildPath(menu.path, '')
  if (route.path === menuPath) return true
  return route.path.startsWith(`${menuPath}/`)
}

function handleSelect(index: string) {
  if (!index || index === route.path) return
  router.push(index)
}
</script>
