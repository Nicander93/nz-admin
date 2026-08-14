<template>
  <div>
    <el-alert type="info" :closable="false" class="mb-4">
      实例始终使用发起时的定义快照和变量运行；当前支持顺序流、条件互斥分支及按用户、角色或发起人办理。
    </el-alert>

    <el-form :inline="true" :model="query" class="mb-4">
      <el-form-item label="流程编码">
        <el-input v-model="query.flowCode" clearable placeholder="例如 leave_apply" />
      </el-form-item>
      <el-form-item label="标题">
        <el-input v-model="query.title" clearable placeholder="流程标题" />
      </el-form-item>
      <el-form-item label="业务标识">
        <el-input v-model="query.businessKey" clearable placeholder="业务唯一标识" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 130px">
          <el-option label="运行中" value="RUNNING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已撤回" value="CANCELED" />
          <el-option label="已终止" value="TERMINATED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="query.mine">我发起的</el-checkbox>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4">
      <el-button v-permission="'workflow:instance:start'" type="primary" @click="openStart">
        发起流程
      </el-button>
    </div>

    <el-table v-loading="loading" :data="instances" border>
      <el-table-column prop="businessKey" label="业务标识" min-width="150" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="flowName" label="流程" min-width="150">
        <template #default="{ row }">{{ row.flowName }} v{{ row.versionNo }}</template>
      </el-table-column>
      <el-table-column prop="currentNodeName" label="当前节点" min-width="130" />
      <el-table-column prop="currentAssignee" label="当前办理人" min-width="130">
        <template #default="{ row }">{{ row.currentAssignee || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ instanceStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="运行状态" width="90" align="center">
        <template #default="{ row }">
          <span v-if="row.status !== 'RUNNING'">-</span>
          <el-tag v-else :type="row.activityStatus === 1 ? 'success' : 'warning'">
            {{ row.activityStatus === 1 ? '激活' : '挂起' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="发起时间" width="180" />
      <el-table-column label="操作" width="390" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'workflow:instance:query'" link type="primary" @click="openDetail(row.instanceId)">
            详情
          </el-button>
          <template v-if="row.status === 'RUNNING'">
            <el-button
              v-if="row.activityStatus === 1"
              v-permission="'workflow:instance:action'"
              link
              type="success"
              @click="handleAction(row.instanceId, 'APPROVE')"
            >通过</el-button>
            <el-button
              v-if="row.activityStatus === 1"
              v-permission="'workflow:instance:action'"
              link
              type="danger"
              @click="handleAction(row.instanceId, 'REJECT')"
            >驳回</el-button>
            <el-button
              v-permission="'workflow:instance:cancel'"
              link
              type="warning"
              @click="handleCancel(row.instanceId)"
            >撤回</el-button>
            <el-button
              v-permission="'workflow:instance:terminate'"
              link
              type="danger"
              @click="handleTerminate(row.instanceId)"
            >终止</el-button>
            <el-button
              v-permission="'workflow:instance:active'"
              link
              @click="setActive(row.instanceId, row.activityStatus !== 1)"
            >{{ row.activityStatus === 1 ? '挂起' : '激活' }}</el-button>
          </template>
          <el-button
            v-else
            v-permission="'workflow:instance:remove'"
            link
            type="danger"
            @click="handleRemove(row.instanceId)"
          >删除</el-button>
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

    <el-dialog v-model="startVisible" title="发起流程" width="620px">
      <el-form :model="startForm" label-width="90px">
        <el-form-item label="流程定义" required>
          <el-select v-model="startForm.flowCode" class="w-full" placeholder="请选择已发布流程">
            <el-option
              v-for="item in definitions"
              :key="item.definitionId"
              :label="`${item.flowName}（${item.flowCode} v${item.versionNo}）`"
              :value="item.flowCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="业务标识" required>
          <el-input v-model="startForm.businessKey" maxlength="100" placeholder="同一租户内必须唯一" />
        </el-form-item>
        <el-form-item label="流程标题" required>
          <el-input v-model="startForm.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="流程变量">
          <el-input
            v-model="startForm.variablesJson"
            type="textarea"
            :rows="8"
            class="json-editor"
            placeholder='例如 {"days": 2, "amount": 1200}'
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStart">确认发起</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="流程实例详情" size="620px">
      <template v-if="detail">
        <el-descriptions :column="2" border class="mb-4">
          <el-descriptions-item label="业务标识">{{ detail.businessKey }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ instanceStatusText(detail.status) }}</el-descriptions-item>
          <el-descriptions-item label="流程">{{ detail.flowName }} v{{ detail.versionNo }}</el-descriptions-item>
          <el-descriptions-item label="当前节点">{{ detail.currentNodeName }}</el-descriptions-item>
          <el-descriptions-item label="发起人">{{ detail.initiatorId }}</el-descriptions-item>
          <el-descriptions-item label="办理人">{{ detail.currentAssignee || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4>流程变量</h4>
        <pre class="variables">{{ formatVariables(detail.variablesJson) }}</pre>

        <h4>运行轨迹</h4>
        <el-timeline>
          <el-timeline-item
            v-for="event in detail.events"
            :key="event.eventId"
            :timestamp="event.createTime"
            placement="top"
          >
            <strong>{{ instanceEventText(event.eventType) }}</strong>
            <span> · {{ event.operatorName || event.operatorId }}</span>
            <div v-if="event.fromNodeName || event.toNodeName" class="event-path">
              {{ event.fromNodeName || '开始' }} → {{ event.toNodeName || '结束' }}
            </div>
            <div v-if="event.comment">{{ event.comment }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import type { WorkflowInstanceStatus } from '@/api/workflow/instance'
import {
  formatVariables,
  instanceEventText,
  instanceStatusText,
  useWorkflowInstance,
} from './hooks'

const {
  loading, instances, definitions, total, startVisible, detailVisible, detail,
  query, startForm, load, loadDefinitions, openStart, submitStart, openDetail,
  action, cancel, terminate, setActive, remove, resetQuery,
} = useWorkflowInstance()

function statusTagType(status: WorkflowInstanceStatus) {
  if (status === 'RUNNING') return 'primary'
  if (status === 'COMPLETED') return 'success'
  if (status === 'REJECTED' || status === 'TERMINATED') return 'danger'
  return 'warning'
}

async function handleAction(instanceId: number, actionType: 'APPROVE' | 'REJECT') {
  const { value } = await ElMessageBox.prompt(
    actionType === 'APPROVE' ? '请输入审批意见（可选）' : '请输入驳回原因',
    actionType === 'APPROVE' ? '通过审批' : '驳回流程',
    { inputType: 'textarea', inputValidator: actionType === 'REJECT' ? (value) => Boolean(value?.trim()) || '请输入驳回原因' : undefined },
  )
  await action(instanceId, { action: actionType, comment: value })
}

async function handleCancel(instanceId: number) {
  const { value } = await ElMessageBox.prompt('请输入撤回说明（可选）', '撤回流程', { inputType: 'textarea' })
  await cancel(instanceId, value)
}

async function handleTerminate(instanceId: number) {
  const { value } = await ElMessageBox.prompt('请输入终止原因', '终止流程', {
    type: 'warning',
    inputType: 'textarea',
    inputValidator: (value) => Boolean(value?.trim()) || '请输入终止原因',
  })
  await terminate(instanceId, value)
}

async function handleRemove(instanceId: number) {
  await ElMessageBox.confirm('确认删除该已结束流程实例及其运行轨迹？', '提示', { type: 'warning' })
  await remove(instanceId)
}

onMounted(async () => {
  await Promise.all([loadDefinitions(), load()])
})
</script>

<style scoped>
.json-editor :deep(textarea),
.variables {
  font: 13px/1.55 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

.variables {
  max-height: 220px;
  padding: 12px;
  overflow: auto;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.event-path {
  margin: 6px 0;
  color: var(--el-text-color-secondary);
}
</style>
