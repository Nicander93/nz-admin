<template>
  <div>
    <div class="mb-4">
      <el-upload
        :auto-upload="false"
        :on-change="handleFileChange"
        :show-file-list="false"
      >
        <el-button type="primary">单文件上传</el-button>
      </el-upload>
      <el-upload
        class="ml-2"
        :auto-upload="false"
        :on-change="handleFilesChange"
        multiple
        :show-file-list="false"
      >
        <el-button type="primary">多文件上传</el-button>
      </el-upload>
    </div>

    <el-form :inline="true" :model="table.query" class="mb-4">
      <el-form-item label="文件名">
        <el-input v-model="table.query.originalName" placeholder="请输入文件名" clearable />
      </el-form-item>
      <el-form-item label="文件类型">
        <el-input v-model="table.query.fileExt" placeholder="请输入后缀" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="table.refresh">查询</el-button>
        <el-button @click="table.handleResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="table.data" v-loading="table.loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="originalName" label="文件名" min-width="200" />
      <el-table-column prop="fileExt" label="类型" width="80" />
      <el-table-column prop="fileSize" label="大小" width="120">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column prop="uploaderName" label="上传人" width="120" />
      <el-table-column prop="createTime" label="上传时间" width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'system:file:remove'"
            link
            type="danger"
            @click="actions.remove(row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="mt-4 justify-end"
      v-model:current-page="table.pagination.current"
      v-model:page-size="table.pagination.size"
      :total="table.pagination.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useFileCrud } from './hooks'

const { table, actions } = useFileCrud()

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

function handleFileChange(file: { raw: File }) {
  actions.uploadFile(file.raw)
}

function handleFilesChange(files: { raw: File }[]) {
  actions.uploadFiles(files.map((f) => f.raw))
}

onMounted(() => table.loadData())
</script>
