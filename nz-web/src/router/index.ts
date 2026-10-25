import { createRouter, createWebHistory } from 'vue-router'

import { useUserStore } from '@/stores/user'
import {
  buildDynamicChildrenRoutes,
  resolveFirstRoutePath,
} from '@/utils/routeHelper'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('@/views/login/index.vue'),
    },
    {
      path: '/auth-unavailable',
      name: 'AuthUnavailable',
      component: () => import('@/views/auth-unavailable/index.vue'),
    },
    {
      path: '/oauth/callback/:provider',
      component: () => import('@/views/oauth/callback/index.vue'),
    },
    {
      path: '/',
      name: 'Root',
      component: () => import('@/layout/index.vue'),
      children: [
        {
          path: '/profile',
          name: 'Profile',
          component: () => import('@/views/profile/index.vue'),
        },
        {
          path: '/notice',
          name: 'Notice',
          component: () => import('@/views/notice/index.vue'),
        },
        {
          path: '/file',
          name: 'File',
          redirect: '/system/file',
        },
        {
          path: '/system/post',
          name: 'SystemPost',
          component: () => import('@/views/system/post/index.vue'),
        },
        {
          path: '/system/config',
          name: 'SystemConfig',
          component: () => import('@/views/system/config/index.vue'),
        },
        {
          path: '/system/notice',
          name: 'SystemNotice',
          component: () => import('@/views/system/notice/index.vue'),
        },
      ],
    },
  ],
})

const removeDynamicRoutes: (() => void)[] = []

function clearDynamicRoutes() {
  removeDynamicRoutes.splice(0).forEach((remove) => remove())
}

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  const token = localStorage.getItem('token')
  const authUnavailableRoute = to.path === '/auth-unavailable'
  const publicRoute =
    to.path === '/login' ||
    authUnavailableRoute ||
    to.path.startsWith('/oauth/callback/')
  if (!publicRoute && !token) {
    next('/login')
    return
  }

  if (to.path === '/login' && token) {
    if (userStore.token !== token) userStore.setToken(token)
    next('/')
    return
  }

  if (!token) {
    clearDynamicRoutes()
    next()
    return
  }

  if (authUnavailableRoute) {
    if (userStore.token !== token) userStore.setToken(token)
    next()
    return
  }

  if (userStore.token !== token) userStore.setToken(token)

  if (!userStore.routesLoaded) {
    try {
      if (!userStore.userInfo) {
        await userStore.initAuthData()
      } else if (!userStore.menus.length) {
        await userStore.fetchUserMenus()
      }
      if (!isCurrentIdentity(userStore, token)) {
        next(localStorage.getItem('token') ? '/' : '/login')
        return
      }

      const childrenRoutes = buildDynamicChildrenRoutes(
        userStore.menus,
        userStore.enabledModuleCodes,
      )
      childrenRoutes.forEach((route) => {
        const name = route.name
        if (typeof name === 'string' && !router.hasRoute(name)) {
          removeDynamicRoutes.push(router.addRoute('Root', route))
        }
      })
      userStore.setRoutesLoaded(true)
      next({ ...to, replace: true })
      return
    } catch {
      if (!isCurrentIdentity(userStore, token)) {
        next(localStorage.getItem('token') ? '/' : '/login')
        return
      }

      clearDynamicRoutes()
      userStore.clearUserState()
      next('/auth-unavailable')
      return
    }
  }

  if (to.path === '/') {
    next(resolveFirstRoutePath(userStore.menus, userStore.enabledModuleCodes))
    return
  }

  next()
})

function isCurrentIdentity(userStore: ReturnType<typeof useUserStore>, requestToken: string) {
  return userStore.token === requestToken && localStorage.getItem('token') === requestToken
}

router.onError((error) => {
  console.error('[router]', error)
})

export default router
