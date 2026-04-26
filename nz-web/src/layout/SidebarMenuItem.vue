<template>
  <el-sub-menu v-if="hasChildren" :index="menuIndex">
    <template #title>
      <el-icon>
        <component :is="iconComponent" />
      </el-icon>
      <span>{{ menuTitle }}</span>
    </template>
    <SidebarMenuItem
      v-for="child in menu.children"
      :key="child.id"
      :menu="child"
      :parent-path="menuIndex"
    />
  </el-sub-menu>

  <el-menu-item v-else :index="menuIndex">
    <el-icon>
      <component :is="iconComponent" />
    </el-icon>
    <span>{{ menuTitle }}</span>
  </el-menu-item>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import * as Icons from '@element-plus/icons-vue'
import { Menu as DefaultMenuIcon } from '@element-plus/icons-vue'
import type { MenuRouteItem } from '@/utils/routeHelper'

const props = withDefaults(defineProps<{
  menu: MenuRouteItem
  parentPath?: string
}>(), {
  parentPath: '',
})

const hasChildren = computed(() => Boolean(props.menu.children?.length))
const menuTitle = computed(() => props.menu.meta?.title || props.menu.name)
const menuIndex = computed(() => buildPath(props.menu.path, props.parentPath))

const iconComponent = computed(() => {
  const iconName = props.menu.meta?.icon
  if (typeof iconName === 'string' && iconName && iconName in Icons) {
    return Icons[iconName as keyof typeof Icons]
  }
  return DefaultMenuIcon
})

function normalizeSegment(path: string): string {
  return path.replace(/^\/+|\/+$/g, '')
}

function buildPath(currentPath: string, parentPath: string): string {
  if (!currentPath) return parentPath || '/'
  if (currentPath.startsWith('/')) return currentPath
  const current = normalizeSegment(currentPath)
  const parent = normalizeSegment(parentPath)
  return parent ? `/${parent}/${current}` : `/${current}`
}
</script>
