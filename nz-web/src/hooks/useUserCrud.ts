import { ref, onMounted } from 'vue'
import { useCrud } from '@/utils/CRUD'
import { pageUsers, addUser, updateUser, deleteUser, getUserRoleIds, assignUserRoles } from '@/api/system/user'
import type { SysUser, UserQuery } from '@/api/system/user'
import { listDepts } from '@/api/system/dept'
import type { SysDept } from '@/api/system/dept'
import { listAllRoles } from '@/api/system/role'
import type { SysRole } from '@/api/system/role'
import { buildTree } from '@/utils/tree'

export interface UseUserCrudOptions {
  loadDeptApi?: typeof listDepts
  loadRoleApi?: typeof listAllRoles
}

export function useUserCrud(options: UseUserCrudOptions = {}) {
  const _listDepts = options.loadDeptApi ?? listDepts
  const _listAllRoles = options.loadRoleApi ?? listAllRoles

  const crud = useCrud<SysUser, SysUser, UserQuery>({
    name: '用户',
    pageApi: (params) => pageUsers(params),
    addApi: (data) => addUser(data),
    updateApi: (data) => updateUser(data),
    deleteApi: (ids) => deleteUser(ids[0] as number),
    defaultForm: () => ({
      id: undefined as unknown as number,
      username: '', password: '', nickname: '',
      deptId: undefined as unknown as number,
      email: '', phone: '', status: 0,
      createTime: undefined as unknown as string,
      updateTime: undefined as unknown as string,
    }),
    immediate: false,
  })

  // 部门树
  const deptTree = ref<SysDept[]>([])
  const deptMap = ref<Map<number, string>>(new Map())

  async function loadDepts() {
    const res = await _listDepts()
    deptTree.value = buildTree(res.data)
    const map = new Map<number, string>()
    res.data.forEach(d => map.set(d.id, d.name))
    deptMap.value = map
  }

  function getDeptName(deptId?: number) {
    return deptId ? deptMap.value.get(deptId) || '-' : '-'
  }

  // 角色分配
  const allRoles = ref<SysRole[]>([])
  const roleDialogVisible = ref(false)
  const selectedRoleIds = ref<number[]>([])
  const currentUserId = ref<number>()

  async function loadRoles() {
    const res = await _listAllRoles()
    allRoles.value = res.data
  }

  async function openRoleDialog(row: SysUser) {
    currentUserId.value = row.id
    const res = await getUserRoleIds(row.id)
    selectedRoleIds.value = res.data
    roleDialogVisible.value = true
  }

  async function handleAssignRoles() {
    await assignUserRoles(currentUserId.value!, selectedRoleIds.value)
    roleDialogVisible.value = false
  }

  function handleResetQuery() {
    crud.resetQuery()
    crud.refresh()
  }

  function init() {
    crud.refresh()
    loadDepts()
    loadRoles()
  }

  return {
    ...crud,
    deptTree,
    deptMap,
    getDeptName,
    allRoles,
    roleDialogVisible,
    selectedRoleIds,
    currentUserId,
    loadDepts,
    loadRoles,
    openRoleDialog,
    handleAssignRoles,
    handleResetQuery,
    init,
  }
}
