import { describe, it, expect } from 'vitest'
import { buildTree } from '@/utils/tree'

describe('buildTree', () => {
  it('将平铺数据构建为树形结构', () => {
    const list = [
      { id: 1, parentId: 0, name: '根' },
      { id: 2, parentId: 1, name: '子1' },
      { id: 3, parentId: 1, name: '子2' },
      { id: 4, parentId: 2, name: '孙1' },
    ]

    const tree = buildTree(list)

    expect(tree).toHaveLength(1)
    expect(tree[0].name).toBe('根')
    expect(tree[0].children).toHaveLength(2)
    expect(tree[0].children![0].children).toHaveLength(1)
    expect(tree[0].children![0].children![0].name).toBe('孙1')
  })

  it('处理空数组', () => {
    expect(buildTree([])).toEqual([])
  })

  it('多个根节点', () => {
    const list = [
      { id: 1, parentId: 0, name: '根1' },
      { id: 2, parentId: 0, name: '根2' },
    ]

    const tree = buildTree(list)
    expect(tree).toHaveLength(2)
  })

  it('parentId 不存在的节点作为根节点', () => {
    const list = [
      { id: 1, parentId: 999, name: '孤儿' },
    ]

    const tree = buildTree(list)
    expect(tree).toHaveLength(1)
    expect(tree[0].name).toBe('孤儿')
  })
})
