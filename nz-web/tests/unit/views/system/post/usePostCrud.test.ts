import { describe, it, expect, vi } from 'vitest'

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
}))

import { usePostCrud } from '@/views/system/post/hooks'

function mockListApi(data: any[] = []) {
  return vi.fn().mockResolvedValue({ code: 200, data })
}

function mockVoidApi() {
  return vi.fn().mockResolvedValue({ code: 200, data: null })
}

describe('usePostCrud', () => {
  it('loadData 填充表格', async () => {
    const listApi = mockListApi([
      { id: 1, postCode: 'dev', postName: '开发', sort: 0, status: 0 },
    ])

    const { table } = usePostCrud({
      listApi: listApi as any,
    })

    expect(table.loading).toBe(false)
    await table.loadData()
    expect(table.loading).toBe(false)
    expect(table.data).toHaveLength(1)
    expect(table.data[0].postName).toBe('开发')
  })

  it('submit 新增成功后刷新列表', async () => {
    const addApi = mockVoidApi()
    const listApi = mockListApi([])

    const { form, table, actions } = usePostCrud({
      listApi: listApi as any,
      addApi: addApi as any,
    })

    form.openAdd()
    form.model.postCode = 'pm'
    form.model.postName = '产品'
    await actions.submit()

    expect(addApi).toHaveBeenCalled()
    expect(listApi).toHaveBeenCalled()
  })

  it('remove 删除后刷新列表', async () => {
    const deleteApi = mockVoidApi()
    const listApi = mockListApi([])

    const { actions } = usePostCrud({
      listApi: listApi as any,
      deleteApi: deleteApi as any,
    })

    await actions.remove(1)
    expect(deleteApi).toHaveBeenCalledWith(1)
    expect(listApi).toHaveBeenCalled()
  })
})
