import { describe, expect, it, vi } from 'vitest'

vi.mock('@/api/demo/item', () => ({
  pageDemoItems: vi.fn().mockResolvedValue({
    code: 200,
    data: {
      records: [{ id: 1, name: '模块化示例', category: 'architecture', status: 0, sort: 10 }],
      total: 1,
    },
  }),
  addDemoItem: vi.fn().mockResolvedValue({ code: 200 }),
  updateDemoItem: vi.fn().mockResolvedValue({ code: 200 }),
  deleteDemoItem: vi.fn().mockResolvedValue({ code: 200 }),
}))

import { getFrontendModuleManifests, getModuleCodeForComponent } from '@/core/modules/registry'
import { useDemoItemCrud } from '@/views/demo/item/hooks'

describe('demo module', () => {
  it('is discovered by the frontend module registry', () => {
    expect(getFrontendModuleManifests().map((item) => item.code)).toContain('demo')
    expect(getModuleCodeForComponent('@/views/demo/item/index.vue')).toBe('demo')
  })

  it('loads demo item page data', async () => {
    const { table } = useDemoItemCrud()
    await table.refresh()

    expect(table.data).toHaveLength(1)
    expect(table.data[0].name).toBe('模块化示例')
    expect(table.pagination.total).toBe(1)
  })

  it('opens a complete default form', () => {
    const { form } = useDemoItemCrud()
    form.openAdd()

    expect(form.visible).toBe(true)
    expect(form.model.category).toBe('general')
    expect(form.model.status).toBe(0)
    expect(form.model.sort).toBe(10)
  })
})
