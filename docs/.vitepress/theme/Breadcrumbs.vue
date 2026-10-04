<script setup lang="ts">
import { computed } from 'vue'
import { useData, withBase, type DefaultTheme } from 'vitepress'

const { page, theme } = useData()

function normalize(path: string) {
  return path
    .replace(/\.(?:md|html)$/, '')
    .replace(/\/index$/, '')
    .replace(/^\/|\/$/g, '')
}

function findTrail(
  items: DefaultTheme.SidebarItem[],
  path: string,
): DefaultTheme.SidebarItem[] {
  for (const item of items) {
    if (item.link && normalize(item.link) === path) return [item]
    const children = item.items ? findTrail(item.items, path) : []
    if (children.length) return [item, ...children]
  }
  return []
}

const trail = computed(() =>
  findTrail(theme.value.sidebar, normalize(page.value.relativePath)),
)
</script>

<template>
  <nav v-if="trail.length" class="breadcrumbs" aria-label="阅读路径">
    <ol class="breadcrumbs__list">
      <li v-for="(item, index) in trail" :key="index" class="breadcrumbs__item">
        <a
          v-if="item.link && index < trail.length - 1"
          :href="withBase(item.link)"
          >{{ item.text }}</a
        >
        <span
          v-else
          :aria-current="index === trail.length - 1 ? 'page' : undefined"
          >{{ item.text }}</span
        >
      </li>
    </ol>
  </nav>
</template>

<style scoped>
.breadcrumbs {
  margin-bottom: 20px;
  color: var(--vp-c-text-2);
  font-size: 13px;
}
.breadcrumbs__list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  list-style: none;
  padding: 0;
  margin: 0;
}
.breadcrumbs__item {
  display: flex;
  gap: 10px;
  align-items: baseline;
}
.breadcrumbs__item + .breadcrumbs__item::before {
  content: '/';
  color: var(--vp-c-text-3);
}
.breadcrumbs a {
  color: var(--vp-c-brand-1);
}
.breadcrumbs a:hover {
  text-decoration: underline;
}
</style>
