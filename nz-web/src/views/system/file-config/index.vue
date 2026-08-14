<template>
  <div>
    <el-form :inline="true" :model="query" class="mb-4">
      <el-form-item label="配置名称">
        <el-input
          v-model="query.configName"
          clearable
          placeholder="请输入名称"
        />
      </el-form-item>
      <el-form-item label="存储类型">
        <el-select v-model="query.storageType" clearable placeholder="全部">
          <el-option label="本地" value="local" />
          <el-option label="OSS" value="oss" />
          <el-option label="S3 / MinIO" value="s3" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部">
          <el-option label="生效中" :value="0" />
          <el-option label="未启用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4">
      <el-button
        v-permission="'system:fileconfig:add'"
        type="primary"
        @click="openAdd"
        >新增配置</el-button
      >
    </div>

    <el-alert type="info" :closable="false" class="mb-4">
      同一时间仅一个配置生效。密钥只可覆盖写入，接口不会返回原文。
    </el-alert>

    <el-table :data="data" v-loading="loading" border>
      <el-table-column prop="configName" label="配置名称" min-width="150" />
      <el-table-column prop="storageType" label="类型" width="90">
        <template #default="{ row }">
          <el-tag
            :type="
              row.storageType === 'oss'
                ? 'warning'
                : row.storageType === 's3'
                  ? 'success'
                  : 'primary'
            "
          >
            {{
              row.storageType === 'oss'
                ? 'OSS'
                : row.storageType === 's3'
                  ? 'S3 / MinIO'
                  : '本地'
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="endpoint"
        label="Endpoint / 本地路径"
        min-width="220"
      >
        <template #default="{ row }">{{
          row.storageType === 'local' ? row.basePath : row.endpoint
        }}</template>
      </el-table-column>
      <el-table-column prop="bucketName" label="Bucket" min-width="130" />
      <el-table-column prop="accessKeyIdMasked" label="AccessKey" width="140" />
      <el-table-column label="密钥" width="90">
        <template #default="{ row }">{{
          row.accessKeySecretConfigured ? '已配置' : '未配置'
        }}</template>
      </el-table-column>
      <el-table-column prop="maxFileSizeBytes" label="单文件上限" width="120">
        <template #default="{ row }">{{
          formatSize(row.maxFileSizeBytes)
        }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'info'">{{
            row.status === 0 ? '生效中' : '未启用'
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'system:fileconfig:edit'"
            link
            type="primary"
            @click="openEdit(row)"
            >编辑</el-button
          >
          <el-button
            v-if="row.status === 0"
            v-permission="'system:fileconfig:test'"
            link
            type="success"
            @click="testConnection(row.id)"
            >测试连接</el-button
          >

          <el-button
            v-if="row.status !== 0"
            v-permission="'system:fileconfig:edit'"
            link
            type="success"
            @click="onActivate(row.id)"
            >启用</el-button
          >
          <el-button
            v-if="row.status !== 0"
            v-permission="'system:fileconfig:remove'"
            link
            type="danger"
            @click="onRemove(row.id)"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      class="mt-4 justify-end"
      layout="total, sizes, prev, pager, next"
      @current-change="load"
      @size-change="load"
    />

    <el-dialog
      v-model="visible"
      :title="mode === 'add' ? '新增文件配置' : '编辑文件配置'"
      width="640px"
    >
      <el-form :model="form" label-width="130px">
        <el-form-item label="配置名称"
          ><el-input v-model="form.configName"
        /></el-form-item>
        <el-form-item label="存储类型">
          <el-radio-group v-model="form.storageType">
            <el-radio value="local">本地</el-radio>
            <el-radio value="oss">OSS</el-radio>
          </el-radio-group>
          <el-radio value="s3">S3 / MinIO</el-radio>
        </el-form-item>
        <template v-if="form.storageType === 'local'">
          <el-form-item label="本地存储路径"
            ><el-input v-model="form.basePath"
          /></el-form-item>
          <el-form-item label="访问前缀"
            ><el-input v-model="form.localAccessUrlPrefix"
          /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="Endpoint"
            ><el-input v-model="form.endpoint"
          /></el-form-item>
          <el-form-item label="AccessKey ID">
            <el-input
              v-model="form.accessKeyId"
              :placeholder="mode === 'edit' ? '留空则保留原值' : ''"
            />
          </el-form-item>
          <el-form-item label="AccessKey Secret">
            <el-input
              v-model="form.accessKeySecret"
              type="password"
              show-password
              :placeholder="mode === 'edit' ? '留空则保留原值' : ''"
            />
          </el-form-item>
          <el-form-item label="Bucket"
            ><el-input v-model="form.bucketName"
          /></el-form-item>
          <el-form-item label="自定义域名"
            ><el-input v-model="form.domain"
          /></el-form-item>
          <el-form-item v-if="form.storageType === 's3'" label="Region">
            <el-input v-model="form.region" placeholder="us-east-1" />
          </el-form-item>
          <el-form-item label="路径前缀"
            ><el-input v-model="form.pathPrefix"
          /></el-form-item>
        </template>
        <el-form-item label="单文件上限">
          <el-input-number
            v-model="form.maxFileSizeBytes"
            :min="1"
            :step="1048576"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注"
          ><el-input v-model="form.remark" type="textarea" :rows="3"
        /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useFileConfig } from './hooks'

const {
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
} = useFileConfig()

async function onActivate(id: number) {
  await ElMessageBox.confirm(
    '启用后将立即切换文件存储配置，确认继续？',
    '启用配置',
    { type: 'warning' },
  )
  await activate(id)
}

async function onRemove(id: number) {
  await ElMessageBox.confirm('确认删除该文件配置？', '提示', {
    type: 'warning',
  })
  await remove(id)
}

function formatSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(0)} MB`
}

onMounted(load)
</script>
