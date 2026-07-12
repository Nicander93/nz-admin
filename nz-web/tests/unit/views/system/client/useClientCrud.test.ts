import { describe, expect, it, vi } from 'vitest'

vi.mock('@/api/system/client', () => ({
  pageClients: vi.fn().mockResolvedValue({
    code: 200,
    data: { records: [{ id: 1, clientId: 'web', clientName: 'Web', loginType: 'account', tokenTimeout: 7200, status: 0 }], total: 1 },
  }),
  addClient: vi.fn().mockResolvedValue({ code: 200 }),
  updateClient: vi.fn().mockResolvedValue({ code: 200 }),
  deleteClient: vi.fn().mockResolvedValue({ code: 200 }),
}))

import { useClientCrud } from '@/views/system/client/hooks'

describe('useClientCrud', () => {
  it('loads client page data', async () => {
    const { table } = useClientCrud()
    await table.refresh()

    expect(table.data).toHaveLength(1)
    expect(table.data[0].clientId).toBe('web')
    expect(table.pagination.total).toBe(1)
  })

  it('opens a default client form', () => {
    const { form } = useClientCrud()
    form.openAdd()

    expect(form.visible).toBe(true)
    expect(form.model.loginType).toBe('account')
    expect(form.model.tokenTimeout).toBe(7200)
  })
})
