<template>
  <el-dialog v-model="singleVisible" title="上传文件" width="600px">
    <el-radio-group v-if="showStorageSwitch" v-model="storageType" class="mb-4">
      <el-radio value="local">本地存储</el-radio>
      <el-radio value="oss">OSS 对象存储</el-radio>
    </el-radio-group>
    <el-upload
      ref="uploadRef"
      :auto-upload="false"
      drag
      :limit="1"
      :on-change="handleFileChange"
      :on-exceed="() => ElMessage.warning('单文件上传仅支持选择 1 个文件')"
    />
    <template #footer>
      <el-button @click="singleVisible = false">取消</el-button>
      <el-button type="primary" @click="doUpload">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="batchVisible" title="批量上传" width="600px">
    <el-radio-group v-if="showStorageSwitch" v-model="storageType" class="mb-4">
      <el-radio value="local">本地存储</el-radio>
      <el-radio value="oss">OSS 对象存储</el-radio>
    </el-radio-group>
    <el-upload
      ref="batchUploadRef"
      :auto-upload="false"
      drag
      multiple
      :on-change="handleBatchFileChange"
      :on-remove="handleBatchFileRemove"
    />
    <template #footer>
      <el-button @click="batchVisible = false">取消</el-button>
      <el-button type="primary" @click="doBatchUpload">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadFiles, UploadInstance } from 'element-plus'
import { uploadFile, uploadFiles, getStorageType } from '@/api/system/file'

const emit = defineEmits<{ uploaded: [] }>()

const singleVisible = ref(false)
const batchVisible = ref(false)
const storageType = ref<'local' | 'oss'>('local')
const showStorageSwitch = ref(false)
const selectedFile = ref<File | null>(null)
const selectedFiles = ref<File[]>([])
const uploadRef = ref<UploadInstance>()
const batchUploadRef = ref<UploadInstance>()

async function checkStorageSwitch() {
  try {
    const res = await getStorageType()
    showStorageSwitch.value = res.data === 'configurable'
  } catch {
    showStorageSwitch.value = false
  }
}

function handleFileChange(file: UploadFile) {
  selectedFile.value = (file.raw as File) || null
}

function handleBatchFileChange(_: UploadFile, files: UploadFiles) {
  selectedFiles.value = files.map(f => f.raw as File).filter(Boolean)
}

function handleBatchFileRemove(_: UploadFile, files: UploadFiles) {
  selectedFiles.value = files.map(f => f.raw as File).filter(Boolean)
}

async function doUpload() {
  if (!selectedFile.value) { ElMessage.warning('请选择文件'); return }
  try {
    await uploadFile(selectedFile.value, storageType.value)
    ElMessage.success('上传成功')
    singleVisible.value = false
    selectedFile.value = null
    uploadRef.value?.clearFiles()
    emit('uploaded')
  } catch (e: any) {
    ElMessage.error(e.message || '上传失败')
  }
}

async function doBatchUpload() {
  if (!selectedFiles.value.length) { ElMessage.warning('请选择文件'); return }
  try {
    await uploadFiles(selectedFiles.value, storageType.value)
    ElMessage.success('批量上传成功')
    batchVisible.value = false
    selectedFiles.value = []
    batchUploadRef.value?.clearFiles()
    emit('uploaded')
  } catch (e: any) {
    ElMessage.error(e.message || '批量上传失败')
  }
}

function openSingle() {
  checkStorageSwitch()
  singleVisible.value = true
}

function openBatch() {
  checkStorageSwitch()
  batchVisible.value = true
}

defineExpose({ openSingle, openBatch })
</script>
