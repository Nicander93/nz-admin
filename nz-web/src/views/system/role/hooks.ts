import { ref, nextTick } from 'vue'
import { useCrud } from '@/utils/CRUD'
import { pageRoles, addRole, updateRole, deleteRole, getRoleMenuIds, assignRoleMenus } from '@/api/system/role'
import type { SysRole, RoleQuery } from '@/api/system/role'
import { listMenus } from '@/api/system/menu'
import type { SysMenu } from '@/api/system/menu'
import { buildTree } from '@/utils/tree'

/**
 * 角色页面的 CRUD 和菜单分配逻辑。
 */
export function useRoleCrud() {
  const crud = useCrud<SysRole, SysRole, RoleQuery>({
    name: '角色',
    pageApi: (params) => pageRoles(params),
    addApi: (data) => addRole(data),
    updateApi: (data) => updateRole(data),
    deleteApi: (ids) => deleteRole(ids[0] as number),
    defaultForm: () => ({
      id: undefined as unknown as number,
      name: '', roleKey: '', sort: 0, status: 0, remark: '',
      createTime: undefined as unknown as string,
    }),
    immediate: false,
  })

  const menuDialogVisible = ref(false)
  const menuTree = ref<SysMenu[]>([])
  const currentRoleId = ref<number>()
  const checkedMenuKeys = ref<number[]>([])

  // 打开分配菜单弹窗，并把当前角色已选菜单回填上去。
  async function openMenuDialog(row: SysRole, setTreeCheckedKeys?: (keys: number[]) => void) {
    currentRoleId.value = row.id

    const [menusRes, idsRes] = await Promise.all([
      listMenus(),
      getRoleMenuIds(row.id),
    ])
    menuTree.value = buildTree(menusRes.data)

    menuDialogVisible.value = true
    await nextTick()

    // 只回填叶子节点，避免父节点全选状态和半选状态冲突。
    const allMenus = menusRes.data
    const parentIds = new Set(allMenus.map(m => m.parentId))
    const leafIds = idsRes.data.filter(id => !parentIds.has(id))
    checkedMenuKeys.value = leafIds
    setTreeCheckedKeys?.(leafIds)
  }

  // 提交角色菜单分配并关闭弹窗。
  async function handleAssignMenus(getCheckedAndHalfChecked: () => number[]) {
    const allKeys = getCheckedAndHalfChecked()
    await assignRoleMenus(currentRoleId.value!, allKeys)
    menuDialogVisible.value = false
  }

  // 重置查询条件并重新拉列表。
  function handleResetQuery() {
    crud.resetQuery()
    crud.refresh()
  }

  return {
    ...crud,
    menuDialogVisible,
    menuTree,
    currentRoleId,
    checkedMenuKeys,
    openMenuDialog,
    handleAssignMenus,
    handleResetQuery,
  }
}
