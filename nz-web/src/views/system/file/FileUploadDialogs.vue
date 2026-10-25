<template>
  <el-dialog
    v-model="upload.visible"
    :title="upload.mode === 'single' ? '上传文件' : '批量上传'"
    width="560px"
    :close-on-click-modal="!upload.uploading"
    :close-on-press-escape="!upload.uploading"
    :before-close="beforeClose"
  >
    <div class="space-y-4">
      <el-alert type="info" :closable="false">
        文件将保存到当前租户已启用的存储配置。
      </el-alert>

      <input
        ref="inputRef"
        class="hidden"
        type="file"
        :multiple="upload.mode === 'batch'"
        :disabled="upload.uploading"
        @change="onSelect"
      />
      <el-button :disabled="upload.uploading" @click="openPicker">
        {{ upload.files.length ? '继续选择' : '选择文件' }}
      </el-button>

      <ul v-if="upload.files.length" class="space-y-2">
        <li
          v-for="file in upload.files"
          :key="fileKey(file)"
          class="flex items-center justify-between rounded border px-3 py-2"
        >
          <span class="min-w-0 truncate">{{ file.name }} · {{ formatSize(file.size) }}</span>
          <el-button
            link
            type="danger"
            :disabled="upload.uploading"
            @click="upload.removeSelectedFile(file)"
          >
            移除
          </el-button>
        </li>
      </ul>
      <el-empty v-else description="尚未选择文件" :image-size="72" />

      <el-alert
        v-if="upload.failedFiles.length"
        type="warning"
        :closable="false"
        title="部分文件未上传成功，请查看原因后重试。"
      >
        <ul class="mt-2 space-y-1">
          <li v-for="failure in upload.failedFiles" :key="failure.index">
            {{ failure.fileName }}：{{ failure.reason }}
          </li>
        </ul>
      </el-alert>
    </div>

    <template #footer>
      <el-button :disabled="upload.uploading" @click="upload.reset()">关闭</el-button>
      <el-button
        type="primary"
        :loading="upload.uploading"
        :disabled="!upload.canSubmit"
        @click="upload.submit()"
      >
        {{ upload.mode === 'single' ? '开始上传' : '上传所选文件' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'

import { useFileUploadDialog } from './hooks'

const emit = defineEmits<{ uploaded: [] }>()
const upload = reactive(useFileUploadDialog(() => emit('uploaded')))
const inputRef = ref<HTMLInputElement>()

function openSingle() {
  upload.open('single')
}

function openBatch() {
  upload.open('batch')
}

function openPicker() {
  if (!upload.uploading) inputRef.value?.click()
}

function onSelect(event: Event) {
  const input = event.currentTarget as HTMLInputElement
  upload.selectFiles(Array.from(input.files ?? []))
  input.value = ''
}

function beforeClose(done: () => void) {
  if (upload.uploading) return
  upload.reset()
  done()
}

function fileKey(file: File) {
  return file.name + '-' + file.size + '-' + file.lastModified
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

defineExpose({ openSingle, openBatch })
</script>
