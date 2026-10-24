import { describe, expect, it, vi } from 'vitest'

import { createCrudFactory } from '@/utils/crudFactory'

function createState(
  add = vi.fn().mockResolvedValue({ code: 200 }),
  deleteApi = vi.fn().mockResolvedValue({ code: 200 }),
) {
  const notify = vi.fn()
  const page = vi.fn().mockResolvedValue({
    data: { records: [{ id: 1, name: 'saved' }], total: 1 },
  })
  const state = createCrudFactory({ notify }).useCrud({
    name: '测试',
    api: { page, add, update: add, delete: deleteApi },
    defaultForm: () => ({ name: '' }),
  })
  state.form.openAdd()
  return { ...state, page, notify, deleteApi }
}

describe('CRUD save lifecycle', () => {
  it('closes and reloads the list after a successful save', async () => {
    const state = createState()
    state.form.form.name = 'saved'
    expect((await state.actions.submit()).ok).toBe(true)
    expect(state.form.visible.value).toBe(false)
    expect(state.table.data.value).toEqual([{ id: 1, name: 'saved' }])
    expect(state.page).toHaveBeenCalledOnce()
  })

  it('keeps the form and its content after a rejected save', async () => {
    const state = createState(vi.fn().mockRejectedValue(new Error('rejected')))
    state.form.form.name = 'retain this'
    expect((await state.actions.submit()).ok).toBe(false)
    expect(state.form.visible.value).toBe(true)
    expect(state.form.form.name).toBe('retain this')
    expect(state.page).not.toHaveBeenCalled()
  })

  it('does not report a committed save as failed if reloading fails', async () => {
    const state = createState()
    state.page.mockRejectedValueOnce(new Error('offline'))
    expect((await state.actions.submit()).ok).toBe(true)
    expect(state.form.visible.value).toBe(false)
    expect(state.notify).toHaveBeenCalledWith(
      'warning',
      '保存已成功，列表刷新失败，请重新查询',
    )
    expect(state.notify).not.toHaveBeenCalledWith('error', expect.anything())
  })
})


describe('CRUD delete lifecycle', () => {
  it('keeps a committed deletion successful if reloading fails', async () => {
    const state = createState()
    state.page.mockRejectedValueOnce(new Error('offline'))

    expect(await state.actions.remove(1)).toBe(true)
    expect(state.deleteApi).toHaveBeenCalledWith([1])
    expect(state.notify).toHaveBeenCalledWith('success', '删除成功')
    expect(state.notify).toHaveBeenCalledWith(
      'warning',
      '删除已成功，列表刷新失败，请重新查询',
    )
    expect(state.notify).not.toHaveBeenCalledWith('error', expect.anything())
    expect(state.page).toHaveBeenCalledOnce()
  })

  it('does not reload the list when deletion fails', async () => {
    const state = createState(
      undefined,
      vi.fn().mockRejectedValue(new Error('delete failed')),
    )

    expect(await state.actions.remove(1)).toBe(false)
    expect(state.notify).toHaveBeenCalledWith('error', 'delete failed')
    expect(state.notify).not.toHaveBeenCalledWith('success', '删除成功')
    expect(state.page).not.toHaveBeenCalled()
  })

  it('does not report an error or reload after deletion is cancelled', async () => {
    const state = createState(undefined, vi.fn().mockRejectedValue('cancel'))

    expect(await state.actions.remove(1)).toBe(false)
    expect(state.notify).not.toHaveBeenCalled()
    expect(state.page).not.toHaveBeenCalled()
  })
})
