import { reactive } from 'vue'
import { useCrud } from '@/utils/CRUD'
import {
  add@@CLASS@@,
  delete@@CLASS@@,
  page@@CLASS@@s,
  update@@CLASS@@,
  type @@CLASS@@,
  type @@CLASS@@Form,
  type @@CLASS@@Query,
} from '@/api/@@MODULE@@/@@BUSINESS@@'

export function use@@CLASS@@Crud() {
  const { table, form, actions } = useCrud<@@CLASS@@, @@CLASS@@Form, @@CLASS@@Query, @@CLASS@@['@@PK_FIELD@@']>({
    name: '@@FEATURE_TS@@',
    api: {
      page: page@@CLASS@@s,
      add: add@@CLASS@@,
      update: update@@CLASS@@,
      delete: (ids: Array<@@CLASS@@['@@PK_FIELD@@']>) => delete@@CLASS@@(ids[0]),
    },
    defaultForm: () => ({
@@TS_DEFAULT_FORM@@
    }),
    immediate: false,
  })

  function resetQuery() {
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
      resetQuery,
    }),
    form: reactive({
      model: form.form,
      visible: form.visible,
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
