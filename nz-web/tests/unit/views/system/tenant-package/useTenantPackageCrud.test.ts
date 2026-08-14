import { describe, expect, it, vi } from 'vitest'

vi.mock('@/api/system/tenant', () => ({
  pageTenantPackages: vi.fn().mockResolvedValue({
    code: 200,
    data: {
      records: [{ id: 1, packageName: '默认套餐', status: 0 }],
      total: 1,
    },
  }),
  addTenantPackage: vi.fn().mockResolvedValue({ code: 200 }),
  updateTenantPackage: vi.fn().mockResolvedValue({ code: 200 }),
  deleteTenantPackage: vi.fn().mockResolvedValue({ code: 200 }),
}))

import { useTenantPackageCrud } from '@/views/system/tenant-package/hooks'

describe('useTenantPackageCrud', () => {
  it('loads package page data', async () => {
    const { table } = useTenantPackageCrud()

    await table.refresh()

    expect(table.data[0].packageName).toBe('默认套餐')
    expect(table.pagination.total).toBe(1)
  })

  it('starts with an empty menu selection', () => {
    const { form } = useTenantPackageCrud()

    form.openAdd()

    expect(form.model.menuIds).toEqual([])
  })
})
