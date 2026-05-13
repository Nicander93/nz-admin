import { describe, it, expect, vi } from 'vitest'
import { useDeptCrud } from '@/views/system/dept/hooks'

function mockListApi(data: any[] = []) {
  return vi.fn().mockResolvedValue({ code: 200, data })
}

function mockVoidApi() {
  return vi.fn().mockResolvedValue({ code: 200, data: null })
}

describe('useDeptCrud', () => {
  it('loadData 加载并构建部门树', async () => {
    const listApi = mockListApi([
      { id: 1, parentId: 0, name: '总公司', sort: 0, status: 0 },
      { id: 2, parentId: 1, name: '技术部', sort: 1, status: 0 },
      { id: 3, parentId: 1, name: '市场部', sort: 2, status: 0 },
    ])

    const { table } = useDeptCrud({
      listApi: listApi as any,
    })

    expect(table.loading).toBe(false)
    await table.loadData()
    expect(table.loading).toBe(false)

    expect(table.flatData).toHaveLength(3)
    expect(table.treeData).toHaveLength(1)
    expect(table.treeData[0].name).toBe('总公司')
    expect(table.treeData[0].children).toHaveLength(2)
  })

  it('openAdd 设置 parentId', () => {
    const { form } = useDeptCrud({
      listApi: mockListApi() as any,
    })

    form.openAdd(5)
    expect(form.visible).toBe(true)
    expect(form.model.parentId).toBe(5)
  })

  it('openAdd 不传 parentId 默认为 0', () => {
    const { form } = useDeptCrud({
      listApi: mockListApi() as any,
    })

    form.openAdd()
    expect(form.model.parentId).toBe(0)
  })

  it('openEdit 填充表单数据', () => {
    const { form } = useDeptCrud({
      listApi: mockListApi() as any,
    })

    form.openEdit({ id: 2, parentId: 1, name: '技术部', sort: 1, status: 0 } as any)
    expect(form.visible).toBe(true)
    expect(form.mode).toBe('edit')
    expect(form.model.name).toBe('技术部')
    expect(form.model.parentId).toBe(1)
  })

  it('submit 新增后刷新列表', async () => {
    const addApi = mockVoidApi()
    const listApi = mockListApi([])

    const { form, actions } = useDeptCrud({
      listApi: listApi as any,
      addApi: addApi as any,
    })

    form.openAdd()
    form.model.name = '新部门'
    await actions.submit()

    expect(addApi).toHaveBeenCalled()
    expect(listApi).toHaveBeenCalledTimes(1)
  })

  it('remove 删除后刷新列表', async () => {
    const deleteApi = mockVoidApi()
    const listApi = mockListApi([])

    const { actions } = useDeptCrud({
      listApi: listApi as any,
      deleteApi: deleteApi as any,
    })

    await actions.remove(1)
    expect(deleteApi).toHaveBeenCalledWith(1)
    expect(listApi).toHaveBeenCalledTimes(1)
  })

  it('toggleExpand 切换展开状态', () => {
    const listApi = mockListApi([])
    const { table } = useDeptCrud({
      listApi: listApi as any,
    })

    expect(table.isExpand).toBe(true)
    table.toggleExpand()
    expect(table.isExpand).toBe(false)
    table.toggleExpand()
    expect(table.isExpand).toBe(true)
  })
})
