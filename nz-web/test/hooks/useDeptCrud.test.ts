import { describe, it, expect, vi } from 'vitest'
import { useDeptCrud } from '@/hooks/useDeptCrud'

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

    const { loadData, treeData, flatData, loading } = useDeptCrud({
      listApi: listApi as any,
    })

    expect(loading.value).toBe(false)
    await loadData()
    expect(loading.value).toBe(false)

    expect(flatData.value).toHaveLength(3)
    expect(treeData.value).toHaveLength(1)
    expect(treeData.value[0].name).toBe('总公司')
    expect(treeData.value[0].children).toHaveLength(2)
  })

  it('openAdd 设置 parentId', () => {
    const { openAdd, form, visible } = useDeptCrud({
      listApi: mockListApi() as any,
    })

    openAdd(5)
    expect(visible.value).toBe(true)
    expect(form.parentId).toBe(5)
  })

  it('openAdd 不传 parentId 默认为 0', () => {
    const { openAdd, form } = useDeptCrud({
      listApi: mockListApi() as any,
    })

    openAdd()
    expect(form.parentId).toBe(0)
  })

  it('openEdit 填充表单数据', () => {
    const { openEdit, form, visible, mode } = useDeptCrud({
      listApi: mockListApi() as any,
    })

    openEdit({ id: 2, parentId: 1, name: '技术部', sort: 1, status: 0 } as any)
    expect(visible.value).toBe(true)
    expect(mode.value).toBe('edit')
    expect(form.name).toBe('技术部')
    expect(form.parentId).toBe(1)
  })

  it('handleSubmit 新增后刷新列表', async () => {
    const addApi = mockVoidApi()
    const listApi = mockListApi([])

    const { openAdd, handleSubmit, form } = useDeptCrud({
      listApi: listApi as any,
      addApi: addApi as any,
    })

    openAdd()
    form.name = '新部门'
    await handleSubmit()

    expect(addApi).toHaveBeenCalled()
    expect(listApi).toHaveBeenCalledTimes(1) // 刷新列表
  })

  it('handleDelete 删除后刷新列表', async () => {
    const deleteApi = mockVoidApi()
    const listApi = mockListApi([])

    const { handleDelete } = useDeptCrud({
      listApi: listApi as any,
      deleteApi: deleteApi as any,
    })

    await handleDelete(1)
    expect(deleteApi).toHaveBeenCalledWith(1)
    expect(listApi).toHaveBeenCalledTimes(1)
  })

  it('toggleExpand 切换展开状态', () => {
    const listApi = mockListApi([])
    const { isExpand, toggleExpand } = useDeptCrud({
      listApi: listApi as any,
    })

    expect(isExpand.value).toBe(true)
    toggleExpand()
    expect(isExpand.value).toBe(false)
    toggleExpand()
    expect(isExpand.value).toBe(true)
  })
})
