<template>
  <div class="file-management">
    <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
      <div class="flex items-center">
        <el-button
          v-permission="'system:file:upload'"
          type="primary"
          @click="uploadDialogsRef?.openSingle()"
        >
          上传文件
        </el-button>
        <el-button
          v-permission="'system:file:upload'"
          type="warning"
          class="ml-2"
          @click="uploadDialogsRef?.openBatch()"
        >
          批量上传
        </el-button>
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
        <el-input
          v-model="table.query.fileExt"
          placeholder="文件类型后缀"
          class="ml-2 w-36"
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
        <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="fileExt" label="类型" width="90" />
      <el-table-column prop="storageType" label="存储方式" width="130">
        <template #default="{ row }">
          <el-tag :type="storageTagType(row.storageType)">
            {{ storageTypeLabel(row.storageType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tooltip
            v-if="row.cleanupError"
            :content="row.cleanupError"
            placement="top"
          >
            <el-tag :type="row.lifecycleStatus === 'DELETE_PENDING' ? 'warning' : 'danger'">
              {{ lifecycleLabel(row.lifecycleStatus) }}
            </el-tag>
          </el-tooltip>
          <el-tag v-else-if="row.storageBindingRequired" type="info">待绑定存储</el-tag>
          <el-tag v-else :type="row.lifecycleStatus === 'READY' ? 'success' : 'warning'">
            {{ lifecycleLabel(row.lifecycleStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="uploaderName" label="上传者" width="120" />
      <el-table-column prop="createTime" label="上传时间" width="180" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'system:file:query'"
            link
            type="primary"
            :disabled="row.lifecycleStatus !== 'READY' || row.storageBindingRequired"
            @click="actions.download(row)"
          >
            下载
          </el-button>
          <el-button
            v-if="row.storageBindingRequired && canBindStorage"
            link
            type="warning"
            @click="actions.openBinding(row)"
          >
            绑定存储
          </el-button>
          <el-button
            v-permission="'system:file:remove'"
            link
            type="danger"
            @click="onDelete(row)"
          >
            {{ row.lifecycleStatus === 'DELETE_PENDING' ? '重试清理' : '删除' }}
          </el-button>
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
      @current-change="table.refresh"
      @size-change="table.refresh"
    />

    <FileUploadDialogs ref="uploadDialogsRef" @uploaded="table.refresh" />

    <el-dialog
      :model-value="binding.visible"
      title="绑定历史文件存储"
      width="560px"
      :close-on-click-modal="!binding.submitting"
      :before-close="beforeCloseBinding"
    >
      <el-alert type="warning" :closable="false" class="mb-4">
        绑定只记录该历史文件原有的存储配置，不会迁移文件。请选择原租户中与文件存储类型一致的配置。
      </el-alert>
      <el-form label-width="110px">
        <el-form-item label="文件">
          <span>{{ binding.file?.originalName }}</span>
        </el-form-item>
        <el-form-item label="存储类型">
          <span>{{ storageTypeLabel(binding.file?.storageType) }}</span>
        </el-form-item>
        <el-form-item label="原存储配置">
          <el-select
            v-model="binding.configId"
            class="w-full"
            placeholder="请选择同类型配置"
            :loading="binding.loading"
            :disabled="binding.loading || binding.submitting"
          >
            <el-option
              v-for="config in binding.configs"
              :key="config.id"
              :label="config.configName + (config.status === 0 ? '（当前生效）' : '（未启用）')"
              :value="config.id"
            />
          </el-select>
          <el-empty
            v-if="!binding.loading && binding.configs.length === 0"
            description="当前租户没有可绑定的同类型配置"
            :image-size="64"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :disabled="binding.submitting" @click="binding.close()">取消</el-button>
        <el-button
          v-if="canBindStorage"
          type="primary"
          :loading="binding.submitting"
          :disabled="!binding.configId || binding.loading"
          @click="binding.submit()"
        >
          确认绑定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import { computed, onMounted, ref } from 'vue'

import type { SysFile } from '@/api/system/file'
import { useUserStore } from '@/stores/user'

import FileUploadDialogs from './FileUploadDialogs.vue'
import { useFileCrud, storageTypeLabel } from './hooks'

const { table, actions, binding } = useFileCrud()
const uploadDialogsRef = ref<InstanceType<typeof FileUploadDialogs>>()
const userStore = useUserStore()
const canBindStorage = computed(() =>
  userStore.hasPermission('system:fileconfig:edit')
  && userStore.hasPermission('system:file:upload')
  && userStore.hasPermission('system:fileconfig:list'),
)

onMounted(() => table.refresh())

async function onDelete(row: SysFile) {
  const isRetry = row.lifecycleStatus === 'DELETE_PENDING'
  await ElMessageBox.confirm(
    isRetry ? '再次提交该文件清理请求？系统会按原存储配置重试。' : '确认删除该文件？',
    isRetry ? '重试清理' : '提示',
    {
      type: 'warning',
      confirmButtonText: isRetry ? '重试清理' : '确认删除',
      cancelButtonText: '取消',
    },
  )
  await actions.remove(row.id)
}

function beforeCloseBinding(done: () => void) {
  if (binding.submitting) return
  binding.close()
  done()
}

function storageTagType(storageType?: string) {
  if (storageType === 'oss') return 'warning'
  if (storageType === 's3') return 'success'
  return 'primary'
}

function lifecycleLabel(status?: string) {
  if (status === 'DELETE_PENDING') return '待清理'
  if (status === 'READY') return '可用'
  return '处理中'
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
