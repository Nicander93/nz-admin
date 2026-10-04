<template>
  <div class="engine-center" v-loading="busy">
    <el-alert
      v-if="!enabled"
      title="新引擎尚未启用，请联系管理员。"
      type="info"
      :closable="false"
    />
    <el-card v-else>
      <el-tabs v-model="tab">
        <el-tab-pane label="我的待办" name="pending" />
        <el-tab-pane label="我的申请" name="applications" />
        <el-tab-pane label="我的已办" name="completed" />
        <el-tab-pane v-if="canDesign" label="流程定义" name="definitions" />
      </el-tabs>
      <template v-if="tab === 'definitions'">
        <div class="toolbar">
          <el-button type="primary" @click="draftOpen = true"
            >创建流程</el-button
          ><el-button @click="refresh">刷新</el-button>
        </div>
        <el-table
          :data="definitions"
          empty-text="尚无流程，请先创建并设计一个流程。"
        >
          <el-table-column prop="flowName" label="流程名称" /><el-table-column
            prop="flowCode"
            label="编码"
          />
          <el-table-column label="业务用途" width="130"
            ><template #default="{ row }">{{
              businessTypes.find((item) => item.code === row.businessType)
                ?.name ?? '通用流程'
            }}</template></el-table-column
          >
          <el-table-column prop="version" label="版本" width="90" />
          <el-table-column label="状态" width="100"
            ><template #default="{ row }">{{
              row.isPublish === 1
                ? '已发布'
                : row.isPublish === 0
                  ? '草稿'
                  : '历史版本'
            }}</template></el-table-column
          >
          <el-table-column label="操作" width="250"
            ><template #default="{ row }">
              <el-button
                link
                type="primary"
                @click="selectedDefinition = row"
                >{{ row.isPublish === 0 ? '设计' : '查看' }}</el-button
              >
              <el-button
                v-if="row.isPublish === 0"
                link
                type="primary"
                @click="publish(row)"
                >发布</el-button
              >
              <el-button link @click="create(row)">创建新版本</el-button>
            </template></el-table-column
          >
        </el-table>
      </template>
      <template v-else>
        <el-table :data="rows" empty-text="暂无流程记录">
          <el-table-column prop="flowName" label="流程名称" /><el-table-column
            prop="businessId"
            label="业务编号"
          />
          <el-table-column label="状态"
            ><template #default="{ row }">{{
              statusLabels[row.flowStatus] ?? row.flowStatus
            }}</template></el-table-column
          >
          <el-table-column prop="createTime" label="申请时间" />
          <el-table-column label="操作" width="100"
            ><template #default="{ row }"
              ><el-button link type="primary" @click="open(row)">{{
                tab === 'pending' ? '办理' : '详情'
              }}</el-button></template
            ></el-table-column
          >
        </el-table>
        <div class="pagination">
          <el-button :disabled="page === 1" @click="page--">上一页</el-button
          ><span>第 {{ page }} 页</span
          ><el-button :disabled="rows.length < 20" @click="page++"
            >下一页</el-button
          >
        </div>
      </template>
    </el-card>
    <el-dialog v-model="draftOpen" title="创建流程" width="480px">
      <el-form label-width="90px"
        ><el-form-item label="流程名称"
          ><el-input
            v-model="draft.flowName"
            aria-label="流程名称"
            maxlength="100" /></el-form-item
        ><el-form-item label="流程编码"
          ><el-input
            v-model="draft.flowCode"
            aria-label="流程编码"
            placeholder="如 leave_approval"
            maxlength="64" /></el-form-item
      ></el-form>
      <template #footer
        ><el-button @click="draftOpen = false">取消</el-button
        ><el-button
          type="primary"
          :disabled="
            !draft.flowName ||
            !/^[A-Za-z][A-Za-z0-9_-]{0,63}$/.test(draft.flowCode)
          "
          @click="create()"
          >创建并设计</el-button
        ></template
      >
    </el-dialog>
    <el-dialog
      :model-value="!!selectedDefinition"
      :title="selectedDefinition?.flowName"
      fullscreen
      destroy-on-close
      @close="selectedDefinition = undefined"
    >
      <WorkflowDesigner
        v-if="selectedDefinition"
        :key="selectedDefinition.id"
        :definition-id="selectedDefinition.id"
        :readonly="selectedDefinition.isPublish !== 0"
        @saved="saved"
      />
    </el-dialog>
    <el-drawer v-model="detailOpen" title="流程详情" size="min(800px, 95vw)">
      <template v-if="snapshot">
        <el-alert
          v-if="snapshot.sync.pending"
          :title="
            snapshot.sync.failed
              ? '申请状态暂未更新，系统会自动重试。'
              : '审批记录已保存，申请状态正在更新。'
          "
          type="info"
          :closable="false"
        />
        <h3>{{ snapshot.instance.flowName }}</h3>
        <p>业务编号：{{ snapshot.instance.businessId }}</p>
        <p>
          状态：{{
            statusLabels[snapshot.instance.flowStatus] ??
            snapshot.instance.flowStatus
          }}
        </p>
        <el-descriptions :column="1" border
          ><el-descriptions-item
            v-for="(value, label) in snapshot.business"
            :key="label"
            :label="String(label)"
            >{{ value }}</el-descriptions-item
          ></el-descriptions
        >
        <el-input
          v-model="comment"
          type="textarea"
          :rows="3"
          placeholder="办理意见"
          aria-label="办理意见"
          maxlength="1000"
        />
        <el-table :data="snapshot.tasks"
          ><el-table-column prop="nodeName" label="当前节点" /><el-table-column
            label="操作"
            ><template #default="{ row }"
              ><template v-if="row.actionable"
                ><el-button
                  v-permission="'workflow:engine:action'"
                  link
                  type="primary"
                  @click="action(row.id, 'PASS')"
                  >通过</el-button
                ><el-button
                  v-permission="'workflow:engine:action'"
                  link
                  type="danger"
                  @click="action(row.id, 'REJECT')"
                  >退回</el-button
                ><el-dropdown
                  v-permission="'workflow:engine:action'"
                  @command="(type: string) => prepareManage(row.id, type)"
                  ><el-button link>更多操作</el-button
                  ><template #dropdown
                    ><el-dropdown-menu
                      ><el-dropdown-item
                        v-for="(label, type) in operations"
                        :key="type"
                        :command="type"
                        >{{ label }}</el-dropdown-item
                      ></el-dropdown-menu
                    ></template
                  ></el-dropdown
                ></template
              ><span v-else>等待办理人处理</span></template
            ></el-table-column
          ></el-table
        >
        <el-button
          v-if="snapshot.instance.creator && snapshot.instance.active"
          v-permission="'workflow:engine:start'"
          type="warning"
          @click="revoke"
          >撤回申请</el-button
        >
        <h4>审批轨迹</h4>
        <el-table :data="snapshot.history"
          ><el-table-column prop="nodeName" label="节点" /><el-table-column
            prop="approver"
            label="办理人" /><el-table-column label="操作"
            ><template #default="{ row }">{{
              row.skipType === 'PASS'
                ? '通过'
                : row.skipType === 'REJECT'
                  ? '退回'
                  : row.skipType
            }}</template></el-table-column
          ><el-table-column prop="message" label="意见"
        /></el-table>
      </template>
    </el-drawer>
    <el-dialog
      v-model="managing"
      :title="operations[manage.type]"
      width="480px"
    >
      <p v-if="manage.type === 'TERMINATE'">
        终止将结束全部在途任务，业务申请会同步取消。
      </p>
      <el-select
        v-else-if="manage.type === 'RETURN'"
        v-model="manage.nodeCode"
        aria-label="退回节点"
        ><el-option
          v-for="node in returnNodes"
          :key="node.nodeCode"
          :label="node.nodeName"
          :value="node.nodeCode"
      /></el-select>
      <template v-else
        ><el-select
          v-model="manage.targets"
          multiple
          filterable
          remote
          :remote-method="searchParticipants"
          aria-label="目标办理人"
          placeholder="按名称搜索办理人"
          ><el-option
            v-for="user in candidates"
            :key="user.storageId"
            :label="`${user.handlerName} (${user.handlerCode})`"
            :value="user.storageId.replace('user:', '')"
        /></el-select>
        <p v-if="manage.type === 'ADD'">
          加签沿用节点设置的通过比例；需要全部人员通过时请设置为 100%。
        </p></template
      >
      <template #footer
        ><el-button @click="managing = false">取消</el-button
        ><el-button
          type="primary"
          :disabled="
            busy ||
            (manage.type === 'RETURN'
              ? !manage.nodeCode
              : manage.type !== 'TERMINATE' && !manage.targets.length)
          "
          @click="submitManage"
          >确认操作</el-button
        ></template
      >
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { useEngineWorkbench } from './hooks'
import WorkflowDesigner from './WorkflowDesigner.vue'
const operations: Record<string, string> = {
  TRANSFER: '转办',
  DEPUTE: '委派',
  RETURN: '指定节点退回',
  ADD: '加签',
  REDUCE: '减签',
  TERMINATE: '终止',
}
const statusLabels: Record<string, string> = {
  '0': '待提交',
  '1': '审批中',
  '2': '审批通过',
  '3': '自动完成',
  '4': '已终止',
  '5': '已作废',
  '6': '已撤销',
  '7': '已取回',
  '8': '已完成',
  '9': '已退回',
  '10': '已失效',
  '11': '已拿回',
  '12': '已重启',
  '13': '暂存',
}
const {
  canDesign,
  enabled,
  busy,
  tab,
  page,
  rows,
  definitions,
  businessTypes,
  selectedDefinition,
  snapshot,
  detailOpen,
  draftOpen,
  draft,
  comment,
  managing,
  manage,
  candidates,
  returnNodes,
  prepareManage,
  submitManage,
  searchParticipants,
  revoke,
  create,
  saved,
  publish,
  open,
  action,
  refresh,
} = useEngineWorkbench()
</script>
<style scoped>
.engine-center {
  display: grid;
  gap: 16px;
}
.toolbar,
.pagination {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
}
.pagination {
  justify-content: flex-end;
}
</style>
