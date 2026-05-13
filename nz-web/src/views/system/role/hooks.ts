import { reactive } from 'vue'
import { useCrud } from '@/utils/CRUD'
import { pageRoles, addRole, updateRole, deleteRole } from '@/api/system/role'
import type { SysRole, RoleQuery } from '@/api/system/role'

/** 角色页面的 CRUD 逻辑。 */
export function useRoleCrud() {
  const { table, form, actions } = useCrud<SysRole, SysRole, RoleQuery & Record<string, unknown>>({
    name: '角色',
    api: {
      page: (params) => pageRoles(params),
      add: (data) => addRole(data),
      update: (data) => updateRole(data),
      delete: (ids: number[]) => deleteRole(ids[0]),
    },
    defaultForm: () => ({
      id: undefined as unknown as number,
      name: '', roleKey: '', sort: 0, status: 0, remark: '',
      createTime: undefined as unknown as string,
    }),
    immediate: false,
  })

  function handleResetQuery() {
    table.resetQuery()
    table.refresh()
  }

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
  }
}
