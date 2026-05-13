<template>
  <div class="file-management">
    <div class="mb-4 flex items-center justify-between">
      <div class="flex items-center">
        <el-button type="primary" @click="uploadDialogsRef?.openSingle()">上传文件</el-button>
        <el-button type="warning" class="ml-2" @click="uploadDialogsRef?.openBatch()">批量上传</el-button>
      </div>
      <div class="flex items-center">
        <el-input
          v-model="table.query.originalName"
          placeholder="搜索文件名..."
          class="w-64"
          clearable
          @keyup.enter="table.handleSearch"
          @clear="table.handleSearch"
        />
        <el-button type="primary" class="ml-2" @click="table.handleSearch">搜索</el-button>
        <el-button class="ml-2" @click="table.handleResetQuery">重置</el-button>
      </div>
    </div>

    <el-table :data="table.data" v-loading="table.loading" border row-key="id">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="originalName" label="文件名" min-width="220" show-overflow-tooltip />
      <el-table-column prop="fileSize" label="大小" width="120">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column prop="fileExt" label="类型" width="100" />
      <el-table-column prop="storageType" label="存储方式" width="140">
        <template #default="{ row }">
          <el-tag :type="row.storageType === 'oss' ? 'warning' : 'primary'">
            {{ row.storageType === 'oss' ? 'OSS' : '本地' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="uploaderName" label="上传者" width="120" />
      <el-table-column prop="createdAt" label="上传时间" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="actions.download(row)">下载</el-button>
          <el-button link type="danger" @click="onDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="mt-4 justify-end"
      v-model:current-page="table.pagination.current"
      v-model:page-size="table.pagination.size"
      :page-sizes="[10, 20, 50]"
      :total="table.pagination.total"
      layout="total, sizes, prev, pager, next"
    />

    <FileUploadDialogs ref="uploadDialogsRef" @uploaded="table.refresh" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useFileCrud } from './hooks'
import FileUploadDialogs from './FileUploadDialogs.vue'
import { ElMessageBox } from 'element-plus'

const { table, actions } = useFileCrud()
const uploadDialogsRef = ref<InstanceType<typeof FileUploadDialogs>>()

onMounted(() => table.refresh())

async function onDelete(id: number) {
  await ElMessageBox.confirm('确认删除该文件？', '提示', { type: 'warning' })
  await actions.remove(id)
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}
</script>

<style scoped>
.file-management {
  padding: 16px;
}
</style>
