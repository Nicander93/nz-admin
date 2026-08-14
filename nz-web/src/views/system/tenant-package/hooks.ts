import { reactive } from 'vue'
import { useCrud } from '@/utils/CRUD'
import {
  addTenantPackage,
  deleteTenantPackage,
  pageTenantPackages,
  updateTenantPackage,
  type SysTenantPackage,
  type TenantPackageQuery,
} from '@/api/system/tenant'

/**
 * 租户套餐页面的 CRUD 逻辑。
 */
export function useTenantPackageCrud() {
  const { table, form, actions } = useCrud<
    SysTenantPackage,
    SysTenantPackage,
    TenantPackageQuery
  >({
    name: '租户套餐',
    api: {
      page: pageTenantPackages,
      add: addTenantPackage,
      update: updateTenantPackage,
      delete: (ids: number[]) => deleteTenantPackage(ids[0]),
    },
    defaultForm: () => ({
      id: 0,
      packageName: '',
      status: 0,
      remark: '',
      menuIds: [],
    }),
    immediate: false,
  })

  function handleResetQuery() {
    table.resetQuery()
    void table.refresh()
  }

  return {
    table: reactive({
      data: table.data,
      loading: table.loading,
      pagination: table.pagination,
      query: table.query,
      refresh: table.refresh,
      handleResetQuery,
    }),
    form: reactive({
      model: form.form,
      visible: form.visible,
      mode: form.mode,
      title: form.title,
      openAdd: form.toAdd,
      openEdit: form.toEdit,
      close: form.close,
    }),
    actions: reactive({
      submit: form.submit,
      remove: actions.remove,
    }),
  }
}
