import { computed, reactive, ref, type Ref } from 'vue'

type PageResponse<T> = {
  data: { records: T[]; total: number; current?: number; size?: number }
}

type PageQuery = Record<string, unknown> & { pageNum?: number; pageSize?: number }
type PageApi<T, Q extends PageQuery> = (query: Q) => Promise<PageResponse<T>>
type FormApi<T> = (data: Partial<T>) => Promise<unknown>

function replaceObject<T extends object>(target: T, source: T) {
  Object.keys(target).forEach(key => delete (target as Record<string, unknown>)[key])
  Object.assign(target, source)
}

export function createCrudFactory(options: {
  notify?: (type: 'success' | 'error' | 'warning', message: string) => void
} = {}) {
  function useForm<T extends object>(formOptions: {
    defaultForm: () => T
    addApi: FormApi<T>
    updateApi: FormApi<T>
  }) {
    const form = reactive(formOptions.defaultForm()) as T
    const visible = ref(false)
    const mode = ref<'add' | 'edit'>('add')
    const title = computed(() => (mode.value === 'add' ? '新增' : '编辑'))

    function openAdd() {
      mode.value = 'add'
      replaceObject(form, formOptions.defaultForm())
      visible.value = true
    }

    function openEdit(row: Partial<T>) {
      mode.value = 'edit'
      replaceObject(form, { ...formOptions.defaultForm(), ...row } as T)
      visible.value = true
    }

    async function submit() {
      const currentMode = mode.value
      const api = currentMode === 'add' ? formOptions.addApi : formOptions.updateApi
      try {
        await api({ ...form })
        return { ok: true, mode: currentMode }
      } catch (error) {
        options.notify?.('error', error instanceof Error ? error.message : '提交失败')
        return { ok: false, mode: currentMode }
      }
    }

    return {
      form, visible, mode, title, openAdd, openEdit,
      toAdd: openAdd, toEdit: openEdit,
      close: () => { visible.value = false },
      submit,
    }
  }

  function useTable<T, Q extends PageQuery>(pageApi: PageApi<T, Q>) {
    const data = ref<T[]>([]) as Ref<T[]>
    const loading = ref(false)
    const pagination = reactive({ current: 1, size: 10, total: 0 })
    const query = reactive({ pageNum: 1, pageSize: 10 } as unknown as Q) as Q

    async function refresh() {
      loading.value = true
      try {
        query.pageNum = pagination.current
        query.pageSize = pagination.size
        const page = (await pageApi({ ...query } as Q)).data
        data.value = page.records ?? []
        pagination.total = page.total ?? 0
        pagination.current = page.current ?? pagination.current
        pagination.size = page.size ?? pagination.size
      } finally {
        loading.value = false
      }
    }

    function resetQuery() {
      replaceObject(query, { pageNum: 1, pageSize: pagination.size } as unknown as Q)
      pagination.current = 1
    }

    return { data, loading, pagination, query, refresh, resetQuery }
  }

  function useCrud<T, F extends object, Q extends PageQuery, Id = number>(crudOptions: {
    name: string
    api: {
      page: PageApi<T, Q>
      add?: FormApi<F>
      update?: FormApi<F>
      delete: (ids: Id[]) => Promise<unknown>
    }
    defaultForm: () => Partial<F>
    immediate?: boolean
  }) {
    const table = useTable(crudOptions.api.page)
    const unsupported: FormApi<F> = async () => { throw new Error('当前页面不支持表单操作') }
    const form = useForm({
      defaultForm: crudOptions.defaultForm as () => F,
      addApi: crudOptions.api.add ?? unsupported,
      updateApi: crudOptions.api.update ?? unsupported,
    })

    async function remove(id: Id) {
      try {
        await crudOptions.api.delete([id])
        options.notify?.('success', '删除成功')
        await table.refresh()
        return true
      } catch (error) {
        options.notify?.('error', error instanceof Error ? error.message : '删除失败')
        return false
      }
    }

    if (crudOptions.immediate) void table.refresh()
    return { table, form, actions: { remove } }
  }

  return { useTable, useCrud, useForm }
}

