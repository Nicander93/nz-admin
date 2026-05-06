import { describe, it, expect } from 'vitest'
import type { MenuRouteItem } from '@/utils/routeHelper'
import {
  buildTree,
  flattenRouteMenus,
  buildDynamicChildrenRoutes,
  resolveFirstRoutePath,
} from '@/utils/routeHelper'

describe('routeHelper', () => {
  it('buildTree 将扁平菜单列表转为树形结构', () => {
    const list: MenuRouteItem[] = [
      { id: 1, name: '系统管理', path: '/system', parentId: 0 },
      { id: 2, name: '用户管理', path: 'user', parentId: 1, component: '@/views/system/user/index.vue' },
      { id: 3, name: '角色管理', path: 'role', parentId: 1, component: '@/views/system/role/index.vue' },
    ]

    const tree = buildTree(list)

    expect(tree).toHaveLength(1)
    expect(tree[0].id).toBe(1)
    expect(tree[0].children).toHaveLength(2)
    expect(tree[0].children?.map((item) => item.id)).toEqual([2, 3])
  })

  it('flattenRouteMenus 将树形菜单扁平化', () => {
    const tree: MenuRouteItem[] = [
      {
        id: 1,
        name: '系统管理',
        path: '/system',
        children: [
          { id: 2, name: '用户管理', path: 'user', component: '@/views/system/user/index.vue' },
          { id: 3, name: '角色管理', path: 'role', component: '@/views/system/role/index.vue' },
        ],
      },
    ]

    const flattened = flattenRouteMenus(tree)

    expect(flattened).toHaveLength(2)
    expect(flattened[0].fullPath).toBe('/system/user')
    expect(flattened[1].fullPath).toBe('/system/role')
    expect(flattened.map((item) => item.menu.id)).toEqual([2, 3])
  })

  it('buildDynamicChildrenRoutes 生成路由配置', () => {
    const list: MenuRouteItem[] = [
      { id: 1, name: '系统管理', path: '/system', parentId: 0 },
      {
        id: 2,
        name: '用户管理',
        path: 'user',
        parentId: 1,
        component: '@/views/system/user/index.vue',
        meta: { icon: 'User' },
      },
      {
        id: 3,
        name: '角色管理',
        path: 'role',
        parentId: 1,
        component: '@/views/system/role/index.vue',
        meta: { title: '角色维护' },
      },
    ]

    const routes = buildDynamicChildrenRoutes(list)

    expect(routes).toHaveLength(2)
    expect(routes[0].path).toBe('system/user')
    expect(routes[0].name).toBe('menu-2')
    expect(typeof routes[0].component).toBe('function')
    expect(routes[0].meta).toMatchObject({ title: '用户管理', icon: 'User' })

    expect(routes[1].path).toBe('system/role')
    expect(routes[1].name).toBe('menu-3')
    expect(routes[1].meta).toMatchObject({ title: '角色维护' })
  })

  it('resolveFirstRoutePath 返回第一个路由路径', () => {
    const list: MenuRouteItem[] = [
      { id: 1, name: '系统管理', path: '/system', parentId: 0 },
      { id: 2, name: '用户管理', path: 'user', parentId: 1, component: '@/views/system/user/index.vue' },
      { id: 3, name: '角色管理', path: 'role', parentId: 1, component: '@/views/system/role/index.vue' },
    ]

    expect(resolveFirstRoutePath(list)).toBe('/system/user')
  })
})
