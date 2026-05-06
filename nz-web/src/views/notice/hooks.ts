import { reactive } from 'vue'
import { useCrud } from '@/utils/CRUD'
import { noticeApi, type NoticeQuery, type SysNotice } from '@/api/system'

/**
 * 通知公告页面的 CRUD 逻辑。
 */
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

  // 重置查询条件后重新查一遍列表。
  function handleResetQuery() {
    table.resetQuery()
    table.refresh()
  }

  // 页面初始化时拉第一页。
  function init() {
    table.refresh()
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
    lifecycle,
  }
}
