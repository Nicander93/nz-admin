import { describe, expect, it, vi } from 'vitest'
import { createCrudFactory } from '@/utils/crudFactory'

type RecordItem = { id: number; name: string }

describe('createCrudFactory', () => {
  it('loads a page and passes the current pagination query', async () => {
    const page = vi.fn().mockResolvedValue({
      data: { records: [{ id: 1, name: 'first' }], total: 1, current: 2, size: 20 },
    })
    const { useTable } = createCrudFactory()
    const table = useTable<RecordItem, Record<string, unknown>>(page)

    table.pagination.current = 2
    table.pagination.size = 20
    await table.refresh()

    expect(page).toHaveBeenCalledWith({ pageNum: 2, pageSize: 20 })
    expect(table.data.value).toEqual([{ id: 1, name: 'first' }])
    expect(table.pagination.total).toBe(1)
  })

  it('resets forms and submits through the matching API', async () => {
    const addApi = vi.fn().mockResolvedValue(undefined)
    const updateApi = vi.fn().mockResolvedValue(undefined)
    const { useForm } = createCrudFactory()
    const form = useForm<RecordItem>({
      defaultForm: () => ({ id: 0, name: '' }),
      addApi,
      updateApi,
    })

    form.openAdd()
    form.form.name = 'new'
    await form.submit()
    form.openEdit({ id: 2, name: 'updated' })
    await form.submit()

    expect(addApi).toHaveBeenCalledWith({ id: 0, name: 'new' })
    expect(updateApi).toHaveBeenCalledWith({ id: 2, name: 'updated' })
  })
})

