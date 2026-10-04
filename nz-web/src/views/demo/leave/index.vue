<template>
  <el-card v-loading="busy" header="我的请假申请">
    <div class="toolbar">
      <el-button v-permission="'demo:leave:save'" type="primary" @click="edit()"
        >新建申请</el-button
      ><el-button @click="refresh">刷新</el-button>
    </div>
    <el-table :data="rows" empty-text="尚无申请，点击新建申请开始。">
      <el-table-column
        prop="reason"
        label="申请原因"
        min-width="180"
        show-overflow-tooltip
      /><el-table-column prop="startDate" label="开始日期" /><el-table-column
        prop="endDate"
        label="结束日期"
      />
      <el-table-column label="状态"
        ><template #default="{ row }">{{
          statuses[row.status] ?? row.status
        }}</template></el-table-column
      >
      <el-table-column label="操作" width="240"
        ><template #default="{ row }"
          ><template v-if="editable(row.status)"
            ><el-button v-permission="'demo:leave:save'" link @click="edit(row)"
              >编辑</el-button
            ><el-button
              v-permission="'demo:leave:submit'"
              link
              type="primary"
              @click="submit(row)"
              >提交审批</el-button
            ></template
          ><el-button v-if="row.instanceId" link @click="detail(row)"
            >审批详情</el-button
          ></template
        ></el-table-column
      >
    </el-table>
    <el-dialog v-model="editing" title="请假申请" width="min(560px, 94vw)">
      <el-alert
        v-if="!definitions.length"
        title="暂无已发布的流程，请联系管理员先设计并发布审批流程。"
        type="info"
        :closable="false"
      />
      <el-form label-width="90px"
        ><el-form-item label="审批流程"
          ><el-select v-model="draft.flowCode" aria-label="审批流程"
            ><el-option
              v-for="item in definitions"
              :key="item.flowCode"
              :label="item.flowName"
              :value="item.flowCode" /></el-select
        ></el-form-item>
        <el-form-item label="开始日期" for="leave-start-date"
          ><el-date-picker
            id="leave-start-date"
            v-model="draft.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            aria-label="开始日期" /></el-form-item
        ><el-form-item label="结束日期" for="leave-end-date"
          ><el-date-picker
            id="leave-end-date"
            v-model="draft.endDate"
            type="date"
            value-format="YYYY-MM-DD"
        /></el-form-item>
        <el-form-item label="申请原因"
          ><el-input
            v-model="draft.reason"
            type="textarea"
            :rows="4"
            maxlength="1000"
            aria-label="申请原因" /></el-form-item
      ></el-form>
      <template #footer
        ><el-button @click="editing = false">取消</el-button
        ><el-button
          type="primary"
          :disabled="
            !draft.reason.trim() ||
            !draft.startDate ||
            !draft.endDate ||
            !draft.flowCode
          "
          @click="save"
          >保存草稿</el-button
        ></template
      >
    </el-dialog>
  </el-card>
</template>
<script setup lang="ts">
import { useLeaveApplications } from './hooks'
const statuses: Record<string, string> = {
  DRAFT: '草稿',
  PENDING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已退回',
  CANCELLED: '已取消',
}
const editable = (status: string) =>
  ['DRAFT', 'REJECTED', 'CANCELLED'].includes(status)
const {
  busy,
  rows,
  definitions,
  editing,
  draft,
  edit,
  save,
  submit,
  detail,
  refresh,
} = useLeaveApplications()
</script>
<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
