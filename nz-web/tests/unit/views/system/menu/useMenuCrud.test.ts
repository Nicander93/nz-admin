import { describe, it, expect, vi } from 'vitest'
import { useMenuCrud } from '@/views/system/menu/hooks'

vi.mock('@/api/system/menu', () => ({
  listMenus: vi.fn().mockResolvedValue({
    code: 200,
    data: [
      { id: 1, parentId: 0, name: '系统管理', type: 'M', sort: 0, visible: 0, status: 0 },
      { id: 3, parentId: 1, name: '子节点后', type: 'C', sort: 2, visible: 0, status: 0, perm: 'a' },
      { id: 2, parentId: 1, name: '子节点先', type: 'C', sort: 1, visible: 0, status: 0, perm: 'b' },
      { id: 4, parentId: 2, name: '查询', type: 'F', sort: 0, visible: 0, status: 0, perm: 'system:user:query' },
    ],
  }),
  addMenu: vi.fn().mockResolvedValue({ code: 200 }),
  updateMenu: vi.fn().mockResolvedValue({ code: 200 }),
  deleteMenu: vi.fn().mockResolvedValue({ code: 200 }),
}))

describe('useMenuCrud', () => {
  it('loadData 构建菜单树并按 sort 排序', async () => {
    const { table } = useMenuCrud()
    await table.loadData()

    expect(table.treeData).toHaveLength(1)
    expect(table.treeData[0].name).toBe('系统管理')
    const children = table.treeData[0].children!
    expect(children).toHaveLength(2)
    expect(children[0].name).toBe('子节点先')
    expect(children[1].name).toBe('子节点后')
    expect(children[0].children).toHaveLength(1)
    expect(children[0].children![0].type).toBe('F')
  })

  it('menuSelectTree 包含根目录节点', async () => {
    const { table } = useMenuCrud()
    await table.loadData()

    expect(table.menuSelectTree).toHaveLength(1)
    expect(table.menuSelectTree[0].name).toBe('根目录')
    expect(table.menuSelectTree[0].id).toBe(0)
  })

  it('openAdd 指定父菜单', () => {
    const { form } = useMenuCrud()

    form.openAdd(5)
    expect(form.visible).toBe(true)
    expect(form.model.parentId).toBe(5)
  })

  it('openEdit 回填表单数据', () => {
    const { form } = useMenuCrud()

    form.openEdit({
      id: 2,
      parentId: 1,
      name: '用户管理',
      path: 'user',
      component: 'system/user/index',
      type: 'C',
      sort: 1,
      visible: 0,
      status: 0,
    })

    expect(form.model.name).toBe('用户管理')
    expect(form.model.type).toBe('C')
    expect(form.model.component).toBe('system/user/index')
  })
})
