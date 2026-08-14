import { describe, expect, it } from 'vitest'
import { buildParentOptions } from '@/views/workflow/category/hooks'
import type { WorkflowCategory } from '@/api/workflow/category'

function category(
  categoryId: number,
  parentId: number,
  categoryName: string,
  children: WorkflowCategory[] = [],
): WorkflowCategory {
  return {
    categoryId,
    parentId,
    ancestors: parentId === 0 ? '0' : `0,${parentId}`,
    categoryName,
    orderNum: 0,
    builtIn: 0,
    children,
  }
}

describe('buildParentOptions', () => {
  it('adds the virtual root option', () => {
    const options = buildParentOptions([category(1, 0, 'OA审批')])

    expect(options[0].categoryId).toBe(0)
    expect(options[0].categoryName).toBe('顶级分类')
    expect(options[0].children?.[0].categoryName).toBe('OA审批')
  })

  it('disables the current category and every descendant', () => {
    const tree = [category(1, 0, 'OA审批', [category(2, 1, '人事', [category(3, 2, '请假')])])]

    const options = buildParentOptions(tree, 2)
    const root = options[0].children?.[0]

    expect(root?.disabled).toBe(false)
    expect(root?.children?.[0].disabled).toBe(true)
    expect(root?.children?.[0].children?.[0].disabled).toBe(true)
  })
})
