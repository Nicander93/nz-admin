import { reactive, ref } from 'vue'
import { useCrud } from '@/utils/CRUD'
import {
  addTenant,
  deactivateTenant,
  listTenantPackages,
  pageTenants,
  updateTenant,
  type SysTenant,
  type SysTenantPackage,
  type TenantForm,
  type TenantQuery,
} from '@/api/system/tenant'

/**
 * 租户管理页面的 CRUD 与套餐选项。
 */
export function useTenantCrud() {
  const packages = ref<SysTenantPackage[]>([])
  const { table, form, actions } = useCrud<SysTenant, TenantForm, TenantQuery>({
    name: '租户',
    api: {
      page: pageTenants,
      add: addTenant,
      update: updateTenant,
      delete: (ids: number[]) => deactivateTenant(ids[0]),
    },
    defaultForm: () => ({
      tenantCode: '',
      tenantName: '',
      contactUser: '',
      contactPhone: '',
      packageId: undefined,
      expireTime: undefined,
      accountCount: 100,
      status: 0,
      remark: '',
      adminUsername: 'admin',
      adminPassword: '',
    }),
    immediate: false,
  })

  async function loadPackages() {
    const result = await listTenantPackages()
    packages.value = result.data || []
  }

  function handleResetQuery() {
    table.resetQuery()
    void table.refresh()
  }

  return {
    packages,
    loadPackages,
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
