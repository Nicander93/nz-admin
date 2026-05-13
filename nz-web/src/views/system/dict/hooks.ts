import { reactive } from 'vue'
import { useCrud } from '@/utils/CRUD'
import { pageDictTypes, addDictType, updateDictType, deleteDictType } from '@/api/system/dict'
import type { SysDictType, DictTypeQuery } from '@/api/system/dict'

/** 字典类型页面的 CRUD 逻辑。 */
export function useDictCrud() {
  const { table, form, actions } = useCrud<SysDictType, SysDictType, DictTypeQuery & Record<string, unknown>>({
    name: '字典类型',
    api: {
      page: (params) => pageDictTypes(params),
      add: (data) => addDictType(data),
      update: (data) => updateDictType(data),
      delete: (ids: number[]) => deleteDictType(ids[0]),
    },
    defaultForm: () => ({
      id: undefined as unknown as number,
      name: '', type: '', status: 0, remark: '',
      createTime: undefined as unknown as string,
    }),
    immediate: false,
  })

  function handleTypeResetQuery() {
    table.resetQuery()
    table.refresh()
  }

  const tableView = reactive({
    data: table.data,
    loading: table.loading,
    pagination: table.pagination,
    query: table.query,
    refresh: table.refresh,
    handleResetQuery: handleTypeResetQuery,
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
