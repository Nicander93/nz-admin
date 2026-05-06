import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { buildDynamicChildrenRoutes, resolveFirstRoutePath } from '@/utils/routeHelper'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('@/views/login/index.vue'),
    },
    {
      path: '/',
      name: 'Root',
      component: () => import('@/layout/index.vue'),
      children: [
        {
          path: '/notice',
          name: 'Notice',
          component: () => import('@/views/notice/index.vue'),
        },
        {
          path: '/file',
          name: 'File',
          component: () => import('@/views/file/index.vue'),
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

router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }

  if (to.path === '/login' && token) {
    next('/')
    return
  }

  if (!token) {
    next()
    return
  }

  if (!userStore.routesLoaded) {
    try {
      if (!userStore.userInfo) {
        await userStore.initAuthData()
      } else if (!userStore.menus.length) {
        await userStore.fetchUserMenus()
      }
      const childrenRoutes = buildDynamicChildrenRoutes(userStore.menus)
      childrenRoutes.forEach((route) => {
        const name = route.name
        if (typeof name === 'string' && !router.hasRoute(name)) {
          router.addRoute('Root', route)
        }
      })
      userStore.setRoutesLoaded(true)
      next({ ...to, replace: true })
      return
    } catch {
      userStore.logout()
      next('/login')
      return
    }
  }

  if (to.path === '/') {
    next(resolveFirstRoutePath(userStore.menus))
    return
  }

  next()
})

router.onError((error) => {
  console.error('[router]', error)
})

export default router
