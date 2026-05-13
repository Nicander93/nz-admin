import type { RouteRecordRaw } from 'vue-router'

export interface MenuMeta {
  title?: string
  icon?: string
  [key: string]: unknown
}

export interface MenuRouteItem {
  id: number
  name: string
  path: string
  component?: string
  parentId?: number | null
  meta?: MenuMeta
  children?: MenuRouteItem[]
}

const viewModules = import.meta.glob('/src/views/**/*.vue')

function normalizeComponentPath(componentPath?: string): string {
  if (!componentPath) return ''
  let p = componentPath
  if (p.startsWith('@/')) {
    p = p.replace('@/', '/src/')
  } else if (!p.startsWith('/src/')) {
    p = `/src/views/${p.replace(/^\/+/, '')}`
  }
  if (!p.endsWith('.vue')) {
    p = `${p}.vue`
  }
  return p
}

function normalizeSegment(path: string): string {
  return path.replace(/^\/+|\/+$/g, '')
}

export function buildPath(currentPath: string, parentPath: string): string {
  if (!currentPath) return parentPath
  if (currentPath.startsWith('/')) return currentPath
  const current = normalizeSegment(currentPath)
  const parent = normalizeSegment(parentPath)
  return parent ? `/${parent}/${current}` : `/${current}`
}

export function buildTree(items: MenuRouteItem[]): MenuRouteItem[] {
  const map = new Map<number, MenuRouteItem>()
  const roots: MenuRouteItem[] = []
  items.forEach((item) => map.set(item.id, { ...item, children: [] }))
  map.forEach((item) => {
    const parentId = item.parentId ?? 0
    if (parentId && map.has(parentId)) {
      map.get(parentId)!.children!.push(item)
      return
    }
    roots.push(item)
  })
  return roots
}

export function flattenRouteMenus(
  menus: MenuRouteItem[],
  parentPath = '',
): Array<{ menu: MenuRouteItem; fullPath: string }> {
  return menus.flatMap((menu) => {
    const fullPath = buildPath(menu.path, parentPath)
    const current = menu.component ? [{ menu, fullPath }] : []
    const children = menu.children?.length ? flattenRouteMenus(menu.children, fullPath) : []
    return current.concat(children)
  })
}

export function buildDynamicChildrenRoutes(menuList: MenuRouteItem[]): RouteRecordRaw[] {
  return flattenRouteMenus(menuList)
    .map(({ menu, fullPath }) => {
      const componentPath = normalizeComponentPath(menu.component)
      const component = viewModules[componentPath]
      if (!component) {
        console.warn(`[router] 未找到组件: ${componentPath}`)
        return null
      }
      return {
        path: fullPath.replace(/^\/+/, ''),
        name: `menu-${menu.id}`,
        component,
        meta: {
          ...(menu.meta || {}),
          title: menu.meta?.title || menu.name,
        },
      } as RouteRecordRaw
    })
    .filter((item): item is RouteRecordRaw => Boolean(item))
}

export function resolveFirstRoutePath(menuList: MenuRouteItem[]): string {
  const first = flattenRouteMenus(menuList).find((item) => Boolean(item.menu.component))
  if (!first) return '/'
  return first.fullPath.startsWith('/') ? first.fullPath : `/${first.fullPath}`
}
