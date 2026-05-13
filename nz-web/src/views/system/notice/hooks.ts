import { reactive } from 'vue'
import { useCrud } from '@/utils/CRUD'
import { noticeApi, type NoticeQuery, type SysNotice } from '@/api/system'

/** 通知公告管理页面的 CRUD 逻辑。 */
export function useNoticeCrud() {
  const { table, form, actions } = useCrud<SysNotice, SysNotice, NoticeQuery & Record<string, unknown>>({
    name: '通知公告',
    api: {
      page: noticeApi.pageNotices,
      add: noticeApi.addNotice,
      update: noticeApi.updateNotice,
      delete: (ids: number[]) => noticeApi.deleteNotice(ids[0]),
    },
    defaultForm: () => ({
      id: undefined as unknown as number,
      title: '',
      content: '',
      type: 1,
      status: 0,
      remark: '',
      createTime: undefined as unknown as string,
      updateTime: undefined as unknown as string,
    }),
    immediate: false,
  })

  function handleResetQuery() {
    table.resetQuery()
    table.refresh()
  }

  function loadData() {
    table.refresh()
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
  }
}
