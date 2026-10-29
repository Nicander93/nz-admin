<template>
  <section class="job-page">
    <el-form :inline="true" :model="query" class="job-page__filters">
      <el-form-item label="Job name">
        <el-input v-model="query.jobName" clearable data-testid="job-filter-name" />
      </el-form-item>
      <el-form-item label="Group">
        <el-input v-model="query.jobGroup" clearable data-testid="job-filter-group" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">Search</el-button>
        <el-button v-permission="'system:job:add'" type="primary" @click="openCreate">
          Add job
        </el-button>
        <el-button v-permission="'system:job:list'" @click="openOutbox">Schedule sync</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading" border row-key="id">
      <el-table-column prop="jobName" label="Job" min-width="160" />
      <el-table-column prop="jobGroup" label="Group" width="140" />
      <el-table-column prop="invokeTarget" label="Invoke target" min-width="170" />
      <el-table-column prop="cronExpression" label="Cron" min-width="170" />
      <el-table-column label="Status" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'info'">
            {{ row.status === 0 ? 'Enabled' : 'Paused' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Schedule" width="145">
        <template #default="{ row }">
          <el-tooltip v-if="row.scheduleState === 'ERROR' && row.scheduleError" :content="row.scheduleError">
            <el-tag type="danger">Sync failed</el-tag>
          </el-tooltip>
          <el-tag v-else-if="row.scheduleState === 'PENDING'" type="warning">Syncing</el-tag>
          <el-tag v-else type="success">Synced</el-tag>
          <span class="job-page__revision">r{{ row.revision }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Actions" min-width="410" fixed="right">
        <template #default="{ row }">
          <div :data-testid="`job-row-${row.id}`" class="job-page__actions">
            <el-button v-permission="'system:job:edit'" link type="primary" @click="execute(row)">
              Run
            </el-button>
            <el-button
              v-if="row.status === 0"
              v-permission="'system:job:edit'"
              link
              type="warning"
              @click="setPaused(row, true)"
            >
              Pause
            </el-button>
            <el-button
              v-else
              v-permission="'system:job:edit'"
              link
              type="success"
              @click="setPaused(row, false)"
            >
              Resume
            </el-button>
            <el-button v-if="canEditJob" v-permission="'system:job:edit'" link @click="openEdit(row.id)">
              Edit
            </el-button>
            <el-button v-permission="'system:joblog:list'" link @click="openRecords(row.id)">
              Executions &amp; logs
            </el-button>
            <el-button v-permission="'system:job:remove'" link type="danger" @click="remove(row)">
              Delete
            </el-button>
          </div>
        </template>
      </el-table-column>
      <template #empty><el-empty description="No scheduled jobs" /></template>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      class="job-page__pagination"
      layout="total, prev, pager, next"
      @current-change="load"
    />

    <el-dialog v-model="formVisible" :title="editingId ? 'Edit job' : 'Add job'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
        <el-form-item label="Job name" prop="jobName">
          <el-input v-model="form.jobName" maxlength="64" data-testid="job-name-input" />
        </el-form-item>
        <el-form-item label="Group" prop="jobGroup">
          <el-input v-model="form.jobGroup" maxlength="64" data-testid="job-group-input" />
        </el-form-item>
        <el-form-item label="Invoke target" prop="invokeTarget">
          <el-input v-model="form.invokeTarget" maxlength="500" data-testid="job-target-input" />
        </el-form-item>
        <el-form-item label="Cron" prop="cronExpression">
          <el-input v-model="form.cronExpression" maxlength="64" data-testid="job-cron-input" />
        </el-form-item>
        <el-form-item label="Status" prop="status">
          <el-select v-model="form.status" data-testid="job-status-input">
            <el-option label="Enabled" :value="0" />
            <el-option label="Paused" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="Concurrent" prop="concurrent">
          <el-select v-model="form.concurrent" data-testid="job-concurrent-input">
            <el-option label="Allowed" :value="0" />
            <el-option label="Disallowed" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="Misfire policy" prop="misfirePolicy">
          <el-select v-model="form.misfirePolicy" data-testid="job-misfire-input">
            <el-option label="Catch up all missed executions" :value="1" />
            <el-option label="Run one missed execution" :value="2" />
            <el-option label="Skip" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="Remark" prop="remark">
          <el-input v-model="form.remark" maxlength="500" data-testid="job-remark-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="saving" data-testid="job-save-button" @click="save">
          Save
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recordsVisible" title="Job executions and logs" width="980px" @closed="stopRecordsRefresh">
      <div class="job-page__record-tools">
        <span v-if="recordsJobId">Job ID: {{ recordsJobId }}</span>
        <el-button :loading="recordsLoading" @click="refreshRecords">Refresh records</el-button>
      </div>
      <el-tabs v-model="recordsTab">
        <el-tab-pane label="Executions" name="executions">
          <el-table :data="executions" v-loading="recordsLoading" row-key="id">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="executionKey" label="Execution key" min-width="240" show-overflow-tooltip />
            <el-table-column prop="state" label="State" width="120" />
            <el-table-column prop="triggerSource" label="Source" width="110" />
            <el-table-column prop="nodeId" label="Node" min-width="130" />
            <el-table-column prop="attempt" label="Attempt" width="90" />
            <el-table-column label="Recovery" width="110">
              <template #default="{ row }">
                <el-tag v-if="row.recovered" type="warning">Recovered</el-tag>
                <span v-else>Initial</span>
              </template>
            </el-table-column>
            <el-table-column prop="lastError" label="Last error" min-width="180" show-overflow-tooltip />
            <el-table-column label="Action" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="canRetryExecution(row)"
                  v-permission="'system:job:edit'"
                  link
                  type="warning"
                  @click="retryFailedExecution(row)"
                >Retry</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="Logs" name="logs">
          <el-table :data="logs" v-loading="recordsLoading" row-key="id">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="executionId" label="Execution ID" width="120" />
            <el-table-column prop="executionKey" label="Execution key" min-width="230" show-overflow-tooltip />
            <el-table-column prop="jobMessage" label="Message" min-width="180" show-overflow-tooltip />
            <el-table-column label="Result" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'danger'">
                  {{ row.status === 0 ? 'Succeeded' : 'Failed' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <el-dialog v-model="outboxVisible" title="Schedule synchronization" width="900px" @closed="stopOutboxRefresh">
      <el-table :data="outboxRows" v-loading="outboxLoading" row-key="id">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="jobId" label="Job ID" width="100" />
        <el-table-column prop="operation" label="Operation" width="110" />
        <el-table-column prop="revision" label="Revision" width="100" />
        <el-table-column prop="state" label="State" width="100" />
        <el-table-column prop="attempts" label="Attempts" width="100" />
        <el-table-column prop="lastError" label="Last error" min-width="220" show-overflow-tooltip />
        <el-table-column label="Action" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.state === 'ERROR'"
              v-permission="'system:job:edit'"
              link
              type="warning"
              @click="retryOutbox(row)"
            >Retry</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="outboxPageNum"
        :page-size="20"
        :total="outboxTotal"
        layout="total, prev, pager, next"
        @current-change="loadOutbox"
      />
      <template #footer>
        <el-button @click="loadOutbox">Refresh</el-button>
        <el-button @click="outboxVisible = false">Close</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import type { JobWriteInput } from '@/api/job'

import { canRetryExecution, useJobPage } from './hooks'

import type { FormRules } from 'element-plus'

const {
  rows, loading, saving, pageNum, pageSize, total, query,
  formVisible, formRef, editingId, form,
  recordsVisible, recordsTab, recordsLoading, recordsJobId, executions, logs,
  outboxVisible, outboxLoading, outboxRows, outboxTotal, outboxPageNum,
  canEditJob,
  load, openCreate, openEdit, save, setPaused, execute, remove,
  refreshRecords, openRecords, stopRecordsRefresh, retryFailedExecution,
  loadOutbox, openOutbox, retryOutbox, stopOutboxRefresh,
} = useJobPage()

const rules: FormRules<JobWriteInput> = {
  jobName: [{ required: true, message: 'Enter a job name', trigger: 'blur' }],
  invokeTarget: [{
    pattern: /^[a-zA-Z][a-zA-Z0-9_]*\.[a-zA-Z][a-zA-Z0-9_]*$/,
    message: 'Use beanName.methodName format',
    trigger: 'blur',
  }],
  cronExpression: [{ required: true, message: 'Enter a Cron expression', trigger: 'blur' }],
}

</script>

<style scoped>
.job-page__filters,
.job-page__record-tools,
.job-page__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.job-page__filters {
  flex-wrap: wrap;
}

.job-page__actions {
  flex-wrap: wrap;
}

.job-page__revision {
  margin-left: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.job-page__record-tools,
.job-page__pagination {
  justify-content: flex-end;
}
</style>
