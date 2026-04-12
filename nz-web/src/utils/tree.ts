export interface TreeNode {
  id: number
  parentId: number
  children?: TreeNode[]
}

export function buildTree<T extends TreeNode>(list: T[]): T[] {
  const map = new Map<number, T>()
  const roots: T[] = []
  const items = list.map(item => ({ ...item, children: [] as T[] }))

  items.forEach(item => map.set(item.id, item))

  items.forEach(item => {
    const parent = map.get(item.parentId)
    if (parent) {
      parent.children!.push(item)
    } else {
      roots.push(item)
    }
  })

  return roots
}
