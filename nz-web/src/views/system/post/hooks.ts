import { ref, reactive } from 'vue'
import { useForm } from '@/utils/CRUD'
import { listPosts, addPost, updatePost, deletePost } from '@/api/system/post'
import type { SysPost } from '@/api/system/post'
import { ElMessage } from 'element-plus'

export interface UsePostCrudOptions {
  listApi?: typeof listPosts
  addApi?: typeof addPost
  updateApi?: typeof updatePost
  deleteApi?: typeof deletePost
}

/**
 * 岗位页面的 CRUD 逻辑。
 */
export function usePostCrud(options: UsePostCrudOptions = {}) {
  const _listPosts = options.listApi ?? listPosts
  const _addPost = options.addApi ?? addPost
  const _updatePost = options.updateApi ?? updatePost
  const _deletePost = options.deleteApi ?? deletePost

  const loading = ref(false)
  const tableData = ref<SysPost[]>([])

  const formState = useForm<SysPost>({
    defaultForm: () => ({
      id: undefined as unknown as number,
      postCode: '',
      postName: '',
      sort: 0,
      status: 0,
      remark: '',
      createTime: undefined as unknown as string,
    }),
    addApi: (data) => _addPost(data),
    updateApi: (data) => _updatePost(data),
  })

  async function loadData() {
    loading.value = true
    try {
      const res = await _listPosts()
      tableData.value = res.data
    } finally {
      loading.value = false
    }
  }

  async function handleSubmit() {
    const result = await formState.submit()
    if (result.ok) {
      ElMessage.success(result.mode === 'add' ? '新增成功' : '更新成功')
      formState.close()
      await loadData()
    }
    return result
  }

  async function handleDelete(id: number) {
    await _deletePost(id)
    ElMessage.success('删除成功')
    await loadData()
  }

  return {
    loading,
    tableData,
    form: formState,
    loadData,
    handleSubmit,
    handleDelete,
  }
}
