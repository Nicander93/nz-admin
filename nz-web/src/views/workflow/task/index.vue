<template>
  <div>
    <el-alert type="info" :closable="false" class="mb-4">
      待办只显示分配给当前用户或其角色的任务；已办和抄送按当前用户隔离。所有办理动作同时写入实例轨迹和任务历史。
    </el-alert>

    <el-tabs :model-value="activeTab" class="task-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="我的待办" name="todo" />
      <el-tab-pane label="我的已办" name="done" />
      <el-tab-pane label="抄送给我" name="copy" />
    </el-tabs>

    <el-form :inline="true" :model="query" class="mb-4">
      <el-form-item v-if="activeTab !== 'copy'" label="节点名称">
        <el-input v-model="query.nodeName" clearable placeholder="请输入节点名称" />
      </el-form-item>
      <el-form-item v-if="activeTab === 'done'" label="办理动作">
        <el-select v-model="query.action" clearable placeholder="全部" style="width: 130px">
          <el-option label="同意" value="APPROVE" />
          <el-option label="驳回" value="REJECT" />
          <el-option label="转办" value="TRANSFER" />
          <el-option label="委派" value="DELEGATE" />
          <el-option label="完成委派" value="RESOLVE" />
          <el-option label="撤回" value="CANCEL" />
          <el-option label="终止" value="TERMINATE" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="activeTab === 'copy'" label="阅读状态">
        <el-select v-model="query.readStatus" clearable placeholder="全部" style="width: 130px">
          <el-option label="未读" :value="0" />
          <el-option label="已读" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="tasks" border>
      <el-table-column prop="businessKey" label="业务标识" min-width="150" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="flowName" label="流程" min-width="150">
        <template #default="{ row }">
          {{ row.flowName }}<span v-if="row.versionNo"> v{{ row.versionNo }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="activeTab !== 'copy'" prop="nodeName" label="节点" min-width="120" />
      <el-table-column v-if="activeTab === 'todo'" label="办理人规则" min-width="180">
        <template #default="{ row }">
          {{ row.assignee }}
          <el-tag v-if="row.delegationStatus === 1" type="warning" size="small">受托任务</el-tag>
          <div v-if="row.ownerAssignee" class="owner">原办理人：{{ row.ownerAssignee }}</div>
        </template>
      </el-table-column>
      <el-table-column v-if="activeTab === 'done'" label="办理动作" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="actionTagType(row.action)">{{ taskActionText(row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="activeTab === 'done'" prop="targetNodeName" label="流向节点" min-width="120">
        <template #default="{ row }">{{ row.targetNodeName || '-' }}</template>
      </el-table-column>
      <el-table-column v-if="activeTab === 'done'" prop="comment" label="办理意见" min-width="160" show-overflow-tooltip />
      <el-table-column v-if="activeTab === 'copy'" prop="operatorName" label="抄送人" min-width="110" />
      <el-table-column v-if="activeTab === 'copy'" prop="comment" label="抄送说明" min-width="180" show-overflow-tooltip />
      <el-table-column v-if="activeTab === 'copy'" label="阅读状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.readStatus === 1 ? 'success' : 'warning'">
            {{ row.readStatus === 1 ? '已读' : '未读' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" :label="activeTab === 'todo' ? '到达时间' : '开始时间'" width="180" />
      <el-table-column v-if="activeTab === 'done'" prop="updateTime" label="办理时间" width="180" />
      <el-table-column label="操作" :width="activeTab === 'todo' ? 330 : 150" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'workflow:task:query'"
            link
            type="primary"
            @click="openDetail(row.instanceId)"
          >详情</el-button>
          <template v-if="activeTab === 'todo'">
            <template v-if="row.delegationStatus !== 1">
              <el-button
                v-permission="'workflow:task:action'"
                link
                type="success"
                @click="handleAction(row.taskId, 'APPROVE')"
              >通过</el-button>
              <el-button
                v-permission="'workflow:task:action'"
                link
                type="danger"
                @click="handleAction(row.taskId, 'REJECT')"
              >驳回</el-button>
              <el-button
                v-permission="'workflow:task:transfer'"
                link
                type="warning"
                @click="openTransfer(row)"
              >转办</el-button>
              <el-button
                v-permission="'workflow:task:delegate'"
                link
                type="warning"
                @click="openDelegate(row)"
              >委派</el-button>
            </template>
            <el-button
              v-else
              v-permission="'workflow:task:delegate'"
              link
              type="success"
              @click="handleResolve(row.taskId)"
            >完成委派</el-button>
            <el-button
              v-permission="'workflow:task:copy'"
              link
              @click="openCopy(row)"
            >抄送</el-button>
          </template>
          <el-button
            v-if="activeTab === 'copy' && row.readStatus === 0"
            v-permission="'workflow:task:read'"
            link
            type="success"
            @click="handleMarkRead(row.copyId)"
          >标记已读</el-button>
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

    <el-dialog v-model="transferVisible" title="转办任务" width="520px">
      <el-form :model="transferForm" label-width="90px">
        <el-form-item label="转办用户" required>
          <el-select v-model="transferForm.targetUserId" filterable class="w-full">
            <el-option
              v-for="user in users"
              :key="user.id"
              :label="`${user.nickname}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="转办说明">
          <el-input v-model="transferForm.comment" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTransfer">确认转办</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="copyVisible" title="抄送任务" width="560px">
      <el-form :model="copyForm" label-width="90px">
        <el-form-item label="抄送用户" required>
          <el-select v-model="copyForm.receiverIds" multiple filterable collapse-tags class="w-full">
            <el-option
              v-for="user in users"
              :key="user.id"
              :label="`${user.nickname}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="抄送说明">
          <el-input v-model="copyForm.comment" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCopy">确认抄送</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="流程详情与轨迹" size="620px">
    <el-dialog v-model="delegateVisible" title="委派任务" width="520px">
      <el-alert type="info" :closable="false" class="mb-4">
        受托人完成后任务会归还原办理人，由原办理人最终通过或驳回。
      </el-alert>
      <el-form :model="delegateForm" label-width="90px">
        <el-form-item label="受托用户" required>
          <el-select v-model="delegateForm.targetUserId" filterable class="w-full">
            <el-option
              v-for="user in users"
              :key="user.id"
              :label="`${user.nickname}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="委派说明">
          <el-input v-model="delegateForm.comment" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="delegateVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDelegate">确认委派</el-button>
      </template>
    </el-dialog>

      <template v-if="detail">
        <el-descriptions :column="2" border class="mb-4">
          <el-descriptions-item label="业务标识">{{ detail.businessKey }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ instanceStatusText(detail.status) }}</el-descriptions-item>
          <el-descriptions-item label="流程">{{ detail.flowName }} v{{ detail.versionNo }}</el-descriptions-item>
          <el-descriptions-item label="当前节点">{{ detail.currentNodeName }}</el-descriptions-item>
          <el-descriptions-item label="发起人">{{ detail.initiatorId }}</el-descriptions-item>
          <el-descriptions-item label="当前办理人">{{ detail.currentAssignee || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4>流程变量</h4>
        <pre class="variables">{{ formatVariables(detail.variablesJson) }}</pre>

        <h4>实例轨迹</h4>
        <el-timeline>
          <el-timeline-item
            v-for="event in detail.events"
            :key="event.eventId"
            :timestamp="event.createTime"
            placement="top"
          >
            <strong>{{ instanceEventText(event.eventType) }}</strong>
            <span> · {{ event.operatorName || event.operatorId }}</span>
            <div class="event-path">
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
import { ElMessageBox, type TabPaneName } from 'element-plus'
import type { WorkflowTaskAction, WorkflowTaskTab } from '@/api/workflow/task'
import {
  formatVariables,
  instanceEventText,
  instanceStatusText,
} from '@/views/workflow/instance/hooks'
import { taskActionText, useWorkflowTask } from './hooks'

const {
  loading, activeTab, tasks, users, total, detailVisible, detail,
  transferVisible, delegateVisible, copyVisible, query, transferForm, delegateForm, copyForm,
  load, loadUsers, changeTab, openDetail, action, openTransfer,
  submitTransfer, openDelegate, submitDelegate, resolveDelegation,
  openCopy, submitCopy, markRead, resetQuery,
} = useWorkflowTask()

function handleTabChange(name: TabPaneName) {
  changeTab(String(name) as WorkflowTaskTab)
}

function actionTagType(action?: WorkflowTaskAction) {
  if (action === 'APPROVE') return 'success'
  if (action === 'DELEGATE' || action === 'RESOLVE') return 'warning'
  if (action === 'TRANSFER') return 'warning'
  if (action === 'REJECT' || action === 'TERMINATE') return 'danger'
  return 'info'
}

async function handleAction(taskId: number | undefined, actionType: 'APPROVE' | 'REJECT') {
  if (!taskId) return
  const { value } = await ElMessageBox.prompt(
    actionType === 'APPROVE' ? '请输入审批意见（可选）' : '请输入驳回原因',
    actionType === 'APPROVE' ? '通过任务' : '驳回任务',
    {
      inputType: 'textarea',
      inputValidator: actionType === 'REJECT'
        ? (text) => Boolean(text?.trim()) || '请输入驳回原因'
        : undefined,
    },
  )
  await action(taskId, actionType, value)
}

async function handleResolve(taskId?: number) {
  if (!taskId) return
  const { value } = await ElMessageBox.prompt(
    '请输入受托处理说明（可选）',
    '完成委派',
    { inputType: 'textarea' },
  )
  await resolveDelegation(taskId, value)
}

async function handleMarkRead(copyId?: number) {
  if (copyId) await markRead(copyId)
}

onMounted(async () => {
  await Promise.all([loadUsers(), load()])
})
</script>

<style scoped>
.task-tabs {
  margin-bottom: 16px;
}

.variables {
  max-height: 220px;
  padding: 12px;
  overflow: auto;
  font: 13px/1.55 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.event-path {
  margin: 6px 0;
.owner {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

  color: var(--el-text-color-secondary);
}
</style>
