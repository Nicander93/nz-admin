import { describe, it, expect, vi } from 'vitest'
import { useMenuCrud } from '@/hooks/useMenuCrud'

vi.mock('@/api/system/menu', () => ({
  listMenus: vi.fn().mockResolvedValue({
    code: 200,
    data: [
      { id: 1, parentId: 0, name: '系统管理', type: 'M', sort: 0, visible: 0, status: 0 },
      { id: 2, parentId: 1, name: '用户管理', type: 'C', sort: 1, visible: 0, status: 0, perm: 'system:user:list' },
      { id: 3, parentId: 2, name: '查询', type: 'F', sort: 0, visible: 0, status: 0, perm: 'system:user:query' },
    ],
  }),
  addMenu: vi.fn().mockResolvedValue({ code: 200 }),
  updateMenu: vi.fn().mockResolvedValue({ code: 200 }),
  deleteMenu: vi.fn().mockResolvedValue({ code: 200 }),
}))

describe('useMenuCrud', () => {
  it('loadData 构建菜单树', async () => {
    const { loadData, treeData, allMenus } = useMenuCrud()
    await loadData()

    expect(allMenus.value).toHaveLength(3)
    expect(treeData.value).toHaveLength(1)
    expect(treeData.value[0].name).toBe('系统管理')
    expect(treeData.value[0].children).toHaveLength(1)
    expect(treeData.value[0].children![0].children).toHaveLength(1)
  })

  it('menuSelectTree 包含根目录节点', async () => {
    const { loadData, menuSelectTree } = useMenuCrud()
    await loadData()

    expect(menuSelectTree.value).toHaveLength(1)
    expect(menuSelectTree.value[0].name).toBe('根目录')
    expect(menuSelectTree.value[0].id).toBe(0)
  })

  it('openAdd 指定父菜单', () => {
    const { openAdd, form, visible, mode } = useMenuCrud()

    openAdd(5)
    expect(visible.value).toBe(true)
    expect(mode.value).toBe('add')
    expect(form.parentId).toBe(5)
    expect(form.type).toBe('M')
  })

  it('openEdit 加载菜单数据', () => {
    const { openEdit, form, visible, mode } = useMenuCrud()

    openEdit({
      id: 2, parentId: 1, name: '用户管理', path: 'user',
      component: 'system/user/index', type: 'C', sort: 1, visible: 0, status: 0,
    } as any)

    expect(mode.value).toBe('edit')
    expect(form.name).toBe('用户管理')
    expect(form.type).toBe('C')
    expect(form.component).toBe('system/user/index')
  })
})
