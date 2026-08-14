import { reactive } from 'vue'
import { useCrud } from '@/utils/CRUD'
import {
  addDemoItem,
  deleteDemoItem,
  pageDemoItems,
  updateDemoItem,
  type DemoItem,
  type DemoItemQuery,
} from '@/api/demo/item'

/**
 * 示例条目页面的 CRUD 逻辑。
 */
export function useDemoItemCrud() {
  const { table, form, actions } = useCrud<DemoItem, DemoItem, DemoItemQuery>({
    name: '示例条目',
    api: {
      page: pageDemoItems,
      add: addDemoItem,
      update: updateDemoItem,
      delete: (ids: number[]) => deleteDemoItem(ids[0]),
    },
    defaultForm: () => ({
      name: '',
      category: 'general',
      status: 0,
      sort: 10,
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
