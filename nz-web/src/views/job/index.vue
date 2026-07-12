<template>
  <div>
    <el-form :inline="true" :model="query" class="mb-4">
      <el-form-item label="Job name"><el-input v-model="query.jobName" clearable /></el-form-item>
      <el-form-item label="Group"><el-input v-model="query.jobGroup" clearable /></el-form-item>
      <el-form-item><el-button type="primary" @click="load">Search</el-button></el-form-item>
    </el-form>
    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="jobName" label="Job" min-width="160" />
      <el-table-column prop="jobGroup" label="Group" width="120" />
      <el-table-column prop="invokeTarget" label="Invoke target" min-width="180" />
      <el-table-column prop="cronExpression" label="Cron" min-width="160" />
      <el-table-column label="Status" width="100"><template #default="{ row }"><el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? 'Enabled' : 'Paused' }}</el-tag></template></el-table-column>
      <el-table-column label="Actions" width="210" fixed="right"><template #default="{ row }">
        <el-button v-permission="'system:job:edit'" link type="primary" @click="run(row.id)">Run</el-button>
        <el-button v-if="row.status === 0" v-permission="'system:job:edit'" link type="warning" @click="pause(row.id)">Pause</el-button>
        <el-button v-else v-permission="'system:job:edit'" link type="success" @click="resume(row.id)">Resume</el-button>
      </template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" class="mt-4 justify-end" layout="total, prev, pager, next" @current-change="load" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageJobs, pauseJob, resumeJob, runJob, type JobRow } from '@/api/job'

const rows = ref<JobRow[]>([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const query = reactive({ jobName: '', jobGroup: '' })

async function load() {
  loading.value = true
  try {
    const result = await pageJobs({ ...query, pageNum: pageNum.value, pageSize: pageSize.value })
    rows.value = result.data.records
    total.value = result.data.total
  } finally { loading.value = false }
}
async function run(id: number) { await runJob(id); ElMessage.success('Job triggered'); await load() }
async function pause(id: number) { await pauseJob(id); ElMessage.success('Job paused'); await load() }
async function resume(id: number) { await resumeJob(id); ElMessage.success('Job resumed'); await load() }
onMounted(load)
</script>