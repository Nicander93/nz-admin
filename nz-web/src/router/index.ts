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
      children: [],
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
