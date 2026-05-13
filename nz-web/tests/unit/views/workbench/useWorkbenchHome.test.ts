import { describe, it, expect, vi, beforeEach } from 'vitest'
import { defineComponent } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { getWorkbenchSnapshot } from '@/api/system/workbench'

vi.mock('@/api/system/workbench', () => ({
  getWorkbenchSnapshot: vi.fn(),
}))

import { useWorkbenchHome } from '@/views/workbench/hooks'

describe('useWorkbenchHome', () => {
  beforeEach(() => {
    vi.mocked(getWorkbenchSnapshot).mockResolvedValue({
      code: 200,
      data: {
        recentLoginLogs: [{ id: 1, username: 'admin', status: 0 }],
        recentOperLogs: [{ id: 2, title: '登录' }],
      },
    } as never)
  })

  it('onMounted 拉取工作台快照', async () => {
    const Comp = defineComponent({
      setup() {
        return useWorkbenchHome()
      },
      template: '<div />',
    })
    const w = mount(Comp)
    await flushPromises()
    expect(getWorkbenchSnapshot).toHaveBeenCalled()
    expect(w.vm.table.snapshot?.recentLoginLogs).toHaveLength(1)
    expect(w.vm.table.snapshot?.recentOperLogs).toHaveLength(1)
  })
})
