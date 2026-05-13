import { ref, computed, reactive } from 'vue'
import { useForm } from '@/utils/CRUD'
import { listMenus, addMenu, updateMenu, deleteMenu } from '@/api/system/menu'
import type { SysMenu } from '@/api/system/menu'
import { buildTree } from '@/utils/tree'
import { ElMessage } from 'element-plus'

function sortMenuTree(nodes: SysMenu[]): SysMenu[] {
  const sorted = [...nodes].sort((a, b) => {
    const d = (a.sort ?? 0) - (b.sort ?? 0)
    return d !== 0 ? d : a.id - b.id
  })
  return sorted.map(n => ({
    ...n,
    children: n.children?.length ? sortMenuTree(n.children) : [],
  }))
}

/** 菜单页面的 CRUD 逻辑。 */
export function useMenuCrud() {
  const loading = ref(false)
  const treeData = ref<SysMenu[]>([])
  const isExpand = ref(true)

  const menuSelectTree = computed(() => {
    const root = { id: 0, parentId: -1, name: '根目录', children: treeData.value } as any
    return [root]
  })

  const formState = useForm<SysMenu>({
    defaultForm: () => ({
      id: undefined as unknown as number,
      parentId: 0, name: '', path: '', component: '',
      icon: '', sort: 0, type: 'M', perm: '', visible: 0, status: 0,
      createTime: undefined as unknown as string,
    }),
    addApi: (data) => addMenu(data),
    updateApi: (data) => updateMenu(data),
  })

  async function loadData() {
    loading.value = true
    try {
      const res = await listMenus()
      treeData.value = sortMenuTree(buildTree(res.data))
    } finally {
      loading.value = false
    }
  }

  function toggleExpand() {
    isExpand.value = !isExpand.value
  }

  function openAdd(parentId?: number) {
    formState.openAdd()
    if (parentId !== undefined) {
      formState.form.parentId = parentId
    }
  }

  async function submit() {
    const result = await formState.submit()
    if (result.ok) {
      ElMessage.success(result.mode === 'add' ? '新增成功' : '更新成功')
      formState.close()
      await loadData()
    }
    return result
  }

  async function remove(id: number) {
    await deleteMenu(id)
    ElMessage.success('删除成功')
    await loadData()
  }

  const table = reactive({
    loading,
    treeData,
    isExpand,
    menuSelectTree,
    loadData,
    toggleExpand,
  })

  const form = reactive({
    model: formState.form,
    visible: formState.visible,
    title: formState.title,
    mode: formState.mode,
    openAdd,
    openEdit: (row: SysMenu) => formState.openEdit(row),
    close: formState.close,
  })

  const actions = reactive({
    submit,
    remove,
  })

  return { table, form, actions }
}
