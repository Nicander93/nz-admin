import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import { ref, onMounted, onUnmounted, nextTick } from 'vue'

import { apiResponse, pageResponse, createHookHost } from '@nz/test/unit'

describe('测试支持包', () => {
  it('保留分页总数并构造统一响应', () => {
    expect(pageResponse([{ id: 1 }], 100).data.total).toBe(100)
    expect(apiResponse(null).code).toBe(200)
  })

  it('hook 在组件生命周期内执行并清理', async () => {
    let disposed = false
    const host = createHookHost(() => {
      const ready = ref(false)
      onMounted(() => { ready.value = true })
      onUnmounted(() => { disposed = true })
      return ready
    })
    expect(() => host.result()).toThrow('请先挂载')
    const wrapper = mount(host.component)
    await nextTick()
    expect(host.result().value).toBe(true)
    wrapper.unmount()
    expect(disposed).toBe(true)
  })
})
