import { ref, reactive } from 'vue'
import { useCrud } from '@/utils/CRUD'
import {
  userApi,
  deptApi,
  roleApi,
  postApi,
  type SysUser,
  type UserQuery,
  type SysDept,
  type SysRole,
  type SysPost,
} from '@/api/system'
import { buildTree } from '@/utils/tree'

export interface UseUserCrudOptions {
  loadDeptApi?: typeof deptApi.listDepts
  loadRoleApi?: typeof roleApi.listAllRoles
}

function createUserCrudState() {
  const crud = useCrud<SysUser, SysUser, UserQuery & Record<string, unknown>>({
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
      postIds: [] as number[],
      status: 0,
    }),
    immediate: false,
  })

  const rawToEdit = crud.form.toEdit.bind(crud.form)
  crud.form.toEdit = async (row: Partial<SysUser>) => {
    const res = await userApi.getUser(row.id!)
    rawToEdit(res.data as SysUser)
  }

  return crud
}

function createUserDeptState(loadDeptApi: typeof deptApi.listDepts) {
  const deptTree = ref<SysDept[]>([])
  const deptMap = ref<Map<number, string>>(new Map())

  async function load() {
    const res = await loadDeptApi()
    deptTree.value = buildTree(res.data)
    const map = new Map<number, string>()
    res.data.forEach(d => map.set(d.id, d.name))
    deptMap.value = map
  }

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

  async function load() {
    const res = await loadRoleApi()
    allRoles.value = res.data
  }

  async function openDialog(row: SysUser) {
    currentUserId.value = row.id
    const res = await userApi.getUserRoleIds(row.id)
    selectedIds.value = res.data
    dialogVisible.value = true
  }

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

/** 用户页面的 CRUD、部门树和角色分配逻辑。 */
export function useUserCrud(options: UseUserCrudOptions = {}) {
  const _listDepts = options.loadDeptApi ?? deptApi.listDepts
  const _listAllRoles = options.loadRoleApi ?? roleApi.listAllRoles

  const posts = ref<SysPost[]>([])
  async function loadPosts() {
    const res = await postApi.listPosts()
    posts.value = res.data
  }

  const { table, form, actions } = createUserCrudState()
  const dept = createUserDeptState(_listDepts)
  const role = createUserRoleState(_listAllRoles)

  function handleResetQuery() {
    table.resetQuery()
    table.refresh()
  }

  function loadData() {
    table.refresh()
    dept.load()
    role.load()
    loadPosts()
  }

  const tableView = reactive({
    data: table.data,
    loading: table.loading,
    pagination: table.pagination,
    query: table.query,
    refresh: table.refresh,
    handleResetQuery,
    loadData,
  })

  const formView = reactive({
    model: form.form,
    visible: form.visible,
    mode: form.mode,
    title: form.title,
    close: form.close,
    openAdd: form.toAdd,
    openEdit: form.toEdit,
  })

  const actionsView = reactive({
    remove: actions.remove,
    submit: form.submit,
  })

  return {
    table: tableView,
    form: formView,
    actions: actionsView,
    dept,
    role,
    posts,
  }
}
