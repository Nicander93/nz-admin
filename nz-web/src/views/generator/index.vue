<template>
  <div class="generator-page">
    <el-alert type="info" :closable="false" class="mb-4">
      读取当前 PostgreSQL 数据源。首版仅支持单主键表；预览确认后再下载，不会直接覆盖工作区文件。
    </el-alert>

    <el-form :inline="true" :model="query" class="mb-4">
      <el-form-item label="Schema">
        <el-input v-model="query.schemaName" placeholder="public" />
      </el-form-item>
      <el-form-item label="表名 / 注释">
        <el-input v-model="query.keyword" clearable placeholder="输入关键字" @keyup.enter="loadTables" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadTables">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tables" border>
      <el-table-column prop="schemaName" label="Schema" width="130" />
      <el-table-column prop="tableName" label="表名" min-width="200" />
      <el-table-column prop="tableComment" label="表注释" min-width="220" show-overflow-tooltip />
      <el-table-column prop="columnCount" label="字段数" width="90" />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'generator:table:query'" link type="primary" @click="openConfigure(row)">
            配置生成
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="configureVisible" title="配置代码生成" width="920px">
      <el-form :model="form" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="数据表">
              <el-input :model-value="`${form.schemaName}.${form.tableName}`" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="功能名称"><el-input v-model="form.featureName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模块名"><el-input v-model="form.moduleName" placeholder="inventory" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务名"><el-input v-model="form.businessName" placeholder="product" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类名"><el-input v-model="form.className" placeholder="InventoryProduct" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Java 包"><el-input v-model="form.packageName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作者"><el-input v-model="form.author" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="父菜单 ID">
              <el-input-number v-model="form.parentMenuId" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-table :data="columns" border max-height="300">
        <el-table-column prop="columnName" label="字段" min-width="150" />
        <el-table-column prop="dataType" label="数据库类型" min-width="150" />
        <el-table-column prop="columnComment" label="注释" min-width="180" show-overflow-tooltip />
        <el-table-column label="主键" width="70">
          <template #default="{ row }">{{ row.primaryKey ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="可空" width="70">
          <template #default="{ row }">{{ row.nullable ? '是' : '否' }}</template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="configureVisible = false">取消</el-button>
        <el-button
          v-permission="'generator:table:preview'"
          :loading="generating"
          type="primary"
          plain
          @click="generatePreview"
        >预览</el-button>
        <el-button
          v-permission="'generator:table:download'"
          :loading="generating"
          type="primary"
          @click="downloadZip"
        >下载 ZIP</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="previewVisible" title="代码预览" size="72%">
      <div class="preview-layout">
        <el-scrollbar class="preview-files">
          <button
            v-for="file in fileNames"
            :key="file"
            type="button"
            :class="{ active: activeFile === file }"
            @click="activeFile = file"
          >
            {{ file }}
          </button>
        </el-scrollbar>
        <el-scrollbar class="preview-code">
          <pre><code>{{ activeContent }}</code></pre>
        </el-scrollbar>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useGenerator } from './hooks'

const {
  loading,
  generating,
  tables,
  columns,
  configureVisible,
  previewVisible,
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
} = useGenerator()

onMounted(loadTables)
</script>

<style scoped>
.preview-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  height: calc(100vh - 130px);
  border: 1px solid var(--el-border-color);
}

.preview-files {
  border-right: 1px solid var(--el-border-color);
}

.preview-files button {
  width: 100%;
  padding: 10px 12px;
  border: 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: transparent;
  color: var(--el-text-color-regular);
  text-align: left;
  word-break: break-all;
  cursor: pointer;
}

.preview-files button.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.preview-code {
  background: #111827;
}

.preview-code pre {
  min-width: max-content;
  margin: 0;
  padding: 18px;
  color: #d1d5db;
  font: 13px/1.6 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}
</style>
