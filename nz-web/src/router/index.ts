import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('@/views/login/index.vue'),
    },
    {
      path: '/',
      component: () => import('@/layout/index.vue'),
      redirect: '/system/user',
      children: [
        {
          path: 'system/user',
          component: () => import('@/views/system/user/index.vue'),
          meta: { title: '用户管理' },
        },
        {
          path: 'system/role',
          component: () => import('@/views/system/role/index.vue'),
          meta: { title: '角色管理' },
        },
        {
          path: 'system/dept',
          component: () => import('@/views/system/dept/index.vue'),
          meta: { title: '部门管理' },
        },
        {
          path: 'system/menu',
          component: () => import('@/views/system/menu/index.vue'),
          meta: { title: '菜单管理' },
        },
        {
          path: 'system/dict',
          component: () => import('@/views/system/dict/index.vue'),
          meta: { title: '字典管理' },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
