import { reactive } from 'vue'
import { useCrud } from '@/utils/CRUD'
import { addClient, deleteClient, pageClients, updateClient, type ClientQuery, type SysClient } from '@/api/system/client'

/**
 * 客户端管理页面的 CRUD 逻辑。
 */
export function useClientCrud() {
  const { table, form, actions } = useCrud<SysClient, SysClient, ClientQuery>({
    name: '客户端',
    api: {
      page: pageClients,
      add: addClient,
      update: updateClient,
      delete: (ids: number[]) => deleteClient(ids[0]),
    },
    defaultForm: () => ({
      clientId: '',
      clientName: '',
      loginType: 'account',
      tokenTimeout: 7200,
      status: 0,
      remark: '',
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
