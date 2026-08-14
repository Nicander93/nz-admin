import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  activateFileConfig,
  addFileConfig,
  deleteFileConfig,
  pageFileConfigs,
  testFileConfig,
  updateFileConfig,
  type FileConfigForm,
  type FileConfigQuery,
  type SysFileConfig,
} from '@/api/system/fileConfig'

export interface FileConfigApi {
  page: typeof pageFileConfigs
  add: typeof addFileConfig
  update: typeof updateFileConfig
  remove: typeof deleteFileConfig
  activate: typeof activateFileConfig
  test: typeof testFileConfig
}

const defaultForm = (): FileConfigForm => ({
  configName: '',
  storageType: 'local',
  basePath: '',
  endpoint: '',
  accessKeyId: '',
  accessKeySecret: '',
  bucketName: '',
  region: 'us-east-1',
  domain: '',
  pathPrefix: '',
  localAccessUrlPrefix: '/api/system/file/download/',
  maxFileSizeBytes: 104857600,
  remark: '',
})

export function useFileConfig(api: Partial<FileConfigApi> = {}) {
  const client: FileConfigApi = {
    page: api.page ?? pageFileConfigs,
    add: api.add ?? addFileConfig,
    update: api.update ?? updateFileConfig,
    remove: api.remove ?? deleteFileConfig,
    activate: api.activate ?? activateFileConfig,
    test: api.test ?? testFileConfig,
  }
  const loading = ref(false)
  const data = ref<SysFileConfig[]>([])
  const total = ref(0)
  const visible = ref(false)
  const mode = ref<'add' | 'edit'>('add')
  const query = reactive<FileConfigQuery>({ pageNum: 1, pageSize: 10 })
  const form = reactive<FileConfigForm>(defaultForm())

  async function load() {
    loading.value = true
    try {
      const response = await client.page({ ...query })
      data.value = response.data.records
      total.value = response.data.total
    } finally {
      loading.value = false
    }
  }

  function openAdd() {
    mode.value = 'add'
    Object.assign(form, defaultForm())
    visible.value = true
  }

  function openEdit(row: SysFileConfig) {
    mode.value = 'edit'
    Object.assign(form, defaultForm(), row, {
      accessKeyId: '',
      accessKeySecret: '',
    })
    visible.value = true
  }

  async function submit() {
    if (mode.value === 'add') {
      await client.add({ ...form })
    } else {
      await client.update({ ...form })
    }
    ElMessage.success(mode.value === 'add' ? '新增成功' : '更新成功')
    visible.value = false
    await load()
  }

  async function activate(id: number) {
    await client.activate(id)
    ElMessage.success('配置已生效')
    await load()
  }

  async function testConnection(id: number) {
    await client.test(id)
    ElMessage.success('存储连接正常')
  }

  async function remove(id: number) {
    await client.remove(id)
    ElMessage.success('删除成功')
    await load()
  }

  return {
    loading,
    data,
    total,
    visible,
    mode,
    query,
    form,
    load,
    openAdd,
    openEdit,
    submit,
    activate,
    testConnection,
    remove,
  }
}
