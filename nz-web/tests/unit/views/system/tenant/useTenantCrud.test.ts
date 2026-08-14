import { describe, expect, it, vi } from 'vitest'

vi.mock('@/api/system/tenant', () => ({
  pageTenants: vi.fn().mockResolvedValue({
    code: 200,
    data: {
      records: [
        {
          id: 2,
          tenantCode: 'acme',
          tenantName: 'Acme',
          packageId: 1,
          accountCount: 20,
          status: 0,
        },
      ],
      total: 1,
    },
  }),
  listTenantPackages: vi.fn().mockResolvedValue({
    code: 200,
    data: [{ id: 1, packageName: '默认套餐', status: 0 }],
  }),
  addTenant: vi.fn().mockResolvedValue({ code: 200 }),
  updateTenant: vi.fn().mockResolvedValue({ code: 200 }),
  deactivateTenant: vi.fn().mockResolvedValue({ code: 200 }),
}))

import { useTenantCrud } from '@/views/system/tenant/hooks'

describe('useTenantCrud', () => {
  it('loads tenants and package options', async () => {
    const { table, packages, loadPackages } = useTenantCrud()

    await Promise.all([table.refresh(), loadPackages()])

    expect(table.data[0].tenantCode).toBe('acme')
    expect(packages.value[0].packageName).toBe('默认套餐')
  })

  it('opens tenant form with safe defaults', () => {
    const { form } = useTenantCrud()

    form.openAdd()

    expect(form.model.accountCount).toBe(100)
    expect(form.model.adminUsername).toBe('admin')
    expect(form.model.status).toBe(0)
  })
})
