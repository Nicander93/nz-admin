import { reactive, ref } from 'vue'
import { useCrud } from '@/utils/CRUD'
import { userApi, deptApi, roleApi, type SysUser, type UserQuery, type SysDept, type SysRole } from '@/api/system'
import { buildTree } from '@/utils/tree'

export interface UseUserCrudOptions {
  loadDeptApi?: typeof deptApi.listDepts
  loadRoleApi?: typeof roleApi.listAllRoles
}

function createUserCrudState() {
  return useCrud<SysUser, SysUser, UserQuery & Record<string, unknown>>({
    name: '用户',
    api: {
      page: userApi.pageUsers,
      add: userApi.addUser,
      update: userApi.updateUser,
      delete: (ids: number[]) => userApi.deleteUser(ids[0]),
    },
    defaultForm: () => ({
      username: '',
      nickname: '',
      email: '',
      phone: '',
      password: '',
      deptId: undefined as unknown as number,
      roleIds: [],
      status: 0,
    }),
    immediate: false,
  })
}

function createUserDeptState(loadDeptApi: typeof deptApi.listDepts) {
  const deptTree = ref<SysDept[]>([])
  const deptMap = ref<Map<number, string>>(new Map())

  // 拉部门列表，构建 id->name Map
  async function load() {
    const res = await loadDeptApi()
    deptTree.value = buildTree(res.data)
    const map = new Map<number, string>()
    res.data.forEach(d => map.set(d.id, d.name))
    deptMap.value = map
  }

  // 根据部门 id 拿部门名，拿不到时统一显示 `-`。
  function getName(deptId?: number) {
    return deptId ? deptMap.value.get(deptId) || '-' : '-'
  }

  return reactive({
    tree: deptTree,
    map: deptMap,
    getName,
    load,
  })
}

function createUserRoleState(loadRoleApi: typeof roleApi.listAllRoles) {
  const allRoles = ref<SysRole[]>([])
  const dialogVisible = ref(false)
  const selectedIds = ref<number[]>([])
  const currentUserId = ref<number>()

  // 拉角色列表，用在角色分配弹窗。
  async function load() {
    const res = await loadRoleApi()
    allRoles.value = res.data
  }

  // 打开角色分配弹窗，并回填当前用户已有角色。
  async function openDialog(row: SysUser) {
    currentUserId.value = row.id
    const res = await userApi.getUserRoleIds(row.id)
    selectedIds.value = res.data
    dialogVisible.value = true
  }

  // 提交角色分配并关闭弹窗。
  async function assign() {
    await userApi.assignUserRoles(currentUserId.value!, selectedIds.value)
    dialogVisible.value = false
  }

  return reactive({
    all: allRoles,
    dialogVisible,
    selectedIds,
    openDialog,
    assign,
    load,
  })
}

/**
 * 用户页面的 CRUD、部门树和角色分配逻辑。
 */
export function useUserCrud(options: UseUserCrudOptions = {}) {
  const _listDepts = options.loadDeptApi ?? deptApi.listDepts
  const _listAllRoles = options.loadRoleApi ?? roleApi.listAllRoles

  const { table, form, actions } = createUserCrudState()
  const dept = createUserDeptState(_listDepts)
  const role = createUserRoleState(_listAllRoles)

  // 重置查询条件并重新拉列表。
  function handleResetQuery() {
    table.resetQuery()
    table.refresh()
  }

  // 页面初始化：先查用户，再把部门和角色基础数据准备好。
  function init() {
    table.refresh()
    dept.load()
    role.load()
  }

  const lifecycle = reactive({
    init,
  })

  const tableView = reactive({
    data: table.data,
    loading: table.loading,
    pagination: table.pagination,
    query: table.query,
    refresh: table.refresh,
    handleResetQuery,
  })

  const formView = reactive({
    model: form.form,
    visible: form.visible,
    mode: form.mode,
    title: form.title,
    close: form.close,
    submit: form.submit,
    toAdd: form.toAdd,
    toEdit: form.toEdit,
  })

  const actionsView = reactive({
    remove: actions.remove,
  })

  return {
    table: tableView,
    form: formView,
    actions: actionsView,
    dept,
    role,
    lifecycle,
  }
}
