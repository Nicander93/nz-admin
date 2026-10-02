import { defineComponent, h, type Component } from 'vue'

export interface ApiTestResponse<T> {
  code: number
  data: T
  msg: string
}

export function apiResponse<T>(data: T): ApiTestResponse<T> {
  return { code: 200, data, msg: '成功' }
}

export function pageResponse<T>(records: T[], total = records.length) {
  return apiResponse({ records, total })
}

// 由测试调用者 mount/unmount，保证 hook 的生命周期属于真实 Vue 组件。
export function createHookHost<T>(hook: () => T): { component: Component; result: () => T } {
  let value: T
  let mounted = false
  const component = defineComponent({
    setup() {
      value = hook()
      mounted = true
      return () => h('div')
    },
  })
  return {
    component,
    result() {
      if (!mounted) throw new Error('请先挂载 hook 测试组件')
      return value
    },
  }
}
