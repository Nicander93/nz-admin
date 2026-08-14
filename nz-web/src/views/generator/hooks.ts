import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  downloadGenerator,
  listGeneratorColumns,
  listGeneratorTables,
  previewGenerator,
  type GeneratorColumn,
  type GeneratorPreview,
  type GeneratorRequest,
  type GeneratorTable,
} from '@/api/generator'

export interface GeneratorApi {
  listTables: typeof listGeneratorTables
  listColumns: typeof listGeneratorColumns
  preview: typeof previewGenerator
  download: typeof downloadGenerator
}

function toPascalCase(value: string) {
  return value
    .split(/[_\-\s]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join('')
}

function toCamelCase(value: string) {
  const pascal = toPascalCase(value)
  return pascal.charAt(0).toLowerCase() + pascal.slice(1)
}

export function createGeneratorRequest(table: GeneratorTable): GeneratorRequest {
  const parts = table.tableName.split('_').filter(Boolean)
  const moduleName = parts.length > 1 ? parts[0].toLowerCase() : 'business'
  const businessSource = parts.length > 1 ? parts.slice(1).join('_') : table.tableName
  return {
    schemaName: table.schemaName,
    tableName: table.tableName,
    moduleName,
    businessName: toCamelCase(businessSource),
    className: toPascalCase(table.tableName),
    packageName: `com.nz.admin.modules.${moduleName}`,
    featureName: table.tableComment || table.tableName,
    author: 'nz-admin',
    parentMenuId: 0,
  }
}

export function useGenerator(api: Partial<GeneratorApi> = {}) {
  const client: GeneratorApi = {
    listTables: api.listTables ?? listGeneratorTables,
    listColumns: api.listColumns ?? listGeneratorColumns,
    preview: api.preview ?? previewGenerator,
    download: api.download ?? downloadGenerator,
  }
  const loading = ref(false)
  const generating = ref(false)
  const tables = ref<GeneratorTable[]>([])
  const columns = ref<GeneratorColumn[]>([])
  const configureVisible = ref(false)
  const previewVisible = ref(false)
  const preview = ref<GeneratorPreview | null>(null)
  const activeFile = ref('')
  const query = reactive({ schemaName: 'public', keyword: '' })
  const form = reactive<GeneratorRequest>({
    schemaName: 'public',
    tableName: '',
    moduleName: '',
    businessName: '',
    className: '',
    packageName: '',
    featureName: '',
    author: 'nz-admin',
    parentMenuId: 0,
  })
  const fileNames = computed(() => Object.keys(preview.value?.files ?? {}))
  const activeContent = computed(() => preview.value?.files[activeFile.value] ?? '')

  async function loadTables() {
    loading.value = true
    try {
      const response = await client.listTables({ ...query })
      tables.value = response.data
    } finally {
      loading.value = false
    }
  }

  async function openConfigure(table: GeneratorTable) {
    Object.assign(form, createGeneratorRequest(table))
    const response = await client.listColumns({
      schemaName: table.schemaName,
      tableName: table.tableName,
    })
    columns.value = response.data
    configureVisible.value = true
  }

  async function generatePreview() {
    generating.value = true
    try {
      const response = await client.preview({ ...form })
      preview.value = response.data
      activeFile.value = Object.keys(response.data.files)[0] ?? ''
      previewVisible.value = true
    } finally {
      generating.value = false
    }
  }

  async function downloadZip() {
    generating.value = true
    try {
      const response = await client.download({ ...form })
      const url = URL.createObjectURL(response.data)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `${form.moduleName}-${form.businessName}.zip`
      anchor.click()
      URL.revokeObjectURL(url)
      ElMessage.success('代码包已下载')
    } finally {
      generating.value = false
    }
  }

  function resetQuery() {
    query.keyword = ''
    void loadTables()
  }

  return {
    loading,
    generating,
    tables,
    columns,
    configureVisible,
    previewVisible,
    preview,
    activeFile,
    query,
    form,
    fileNames,
    activeContent,
    loadTables,
    openConfigure,
    generatePreview,
    downloadZip,
    resetQuery,
  }
}
