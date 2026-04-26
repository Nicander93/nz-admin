import { ref, computed } from 'vue'
import { useForm } from '@/utils/CRUD'
import { listMenus, addMenu, updateMenu, deleteMenu } from '@/api/system/menu'
import type { SysMenu } from '@/api/system/menu'
import { buildTree } from '@/utils/tree'
import { ElMessage } from 'element-plus'

/**
 * 菜单页面的 CRUD 逻辑。
 */
export function useMenuCrud() {
  const loading = ref(false)
  const allMenus = ref<SysMenu[]>([])
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

  // 拉菜单列表，并构造成树结构给表格和下拉共用。
  async function loadData() {
    loading.value = true
    try {
      const res = await listMenus()
      allMenus.value = res.data
      treeData.value = buildTree(res.data)
    } finally {
      loading.value = false
    }
  }

  // 切换展开状态后，重新拉一遍数据。
  function toggleExpand() {
    isExpand.value = !isExpand.value
    loadData()
  }

  // 打开新增弹窗；传了父 id 就挂到对应节点下。
  function openAdd(parentId?: number) {
    formState.openAdd()
    if (parentId !== undefined) {
      formState.form.parentId = parentId
    }
  }

  // 提交表单，成功后提示并刷新列表。
  async function handleSubmit() {
    const result = await formState.submit()
    if (result.ok) {
      ElMessage.success(result.mode === 'add' ? '新增成功' : '更新成功')
      formState.close()
      await loadData()
    }
    return result
  }

  // 删除菜单后刷新列表。
  async function handleDelete(id: number) {
    await deleteMenu(id)
    ElMessage.success('删除成功')
    await loadData()
  }

  return {
    loading,
    allMenus,
    treeData,
    isExpand,
    menuSelectTree,
    ...formState,
    loadData,
    toggleExpand,
    openAdd,
    handleSubmit,
    handleDelete,
  }
}
