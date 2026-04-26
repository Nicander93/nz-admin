<template>
  <el-aside width="220px" class="app-sidebar">
    <div class="app-sidebar__brand">
      <span class="app-sidebar__brand-mark">NZ</span>
      <div class="app-sidebar__brand-copy">
        <span class="app-sidebar__title">NZ Admin</span>
        <span class="app-sidebar__subtitle">System Console</span>
      </div>
    </div>
    <el-scrollbar class="flex-1">
      <el-menu
        class="app-menu"
        :default-active="route.path"
        @select="handleSelect"
      >
        <SidebarMenuItem
          v-for="menu in menuTree"
          :key="menu.id"
          :menu="menu"
        />
      </el-menu>
    </el-scrollbar>
  </el-aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { buildTree, type MenuRouteItem } from '@/utils/routeHelper'
import SidebarMenuItem from '@/layout/SidebarMenuItem.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menuTree = computed(() => buildTree(userStore.menus as MenuRouteItem[]))

function handleSelect(index: string) {
  if (!index || index === route.path) return
  router.push(index)
}
</script>
