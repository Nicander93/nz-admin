<template>
  <div class="engine-workbench" v-loading="busy">
    <el-alert
      v-if="!enabled"
      title="新引擎尚未启用，请联系管理员启用后使用。"
      type="info"
      :closable="false"
    />
    <el-alert
      v-else
      title="这里发起的新实例使用新引擎；存量实例继续在原流程页面办理。"
      type="info"
      :closable="false"
    />
    <el-card v-permission="'workflow:engine:design'" header="定义导入与发布">
      <el-tabs>
        <el-tab-pane label="模型编辑"
          ><WorkflowModelEditor v-model="modelJson"
        /></el-tab-pane>
        <el-tab-pane label="JSON 导入"
          ><el-input
            v-model="modelJson"
            type="textarea"
            :rows="12"
            aria-label="流程模型 JSON"
        /></el-tab-pane>
      </el-tabs>
      <div class="actions">
        <el-button :disabled="!enabled" @click="importDefinition"
          >导入新版本</el-button
        >
        <el-input
          v-model="definitionId"
          placeholder="定义 ID"
          aria-label="定义 ID"
        />
        <el-button :disabled="!enabled || !definitionId" @click="loadDefinition"
          >读取模型</el-button
        >
        <el-button
          type="primary"
          :disabled="!enabled || !definitionId"
          @click="publish"
          >发布定义</el-button
        >
      </div>
    </el-card>
    <el-card header="发起与查询">
      <el-form label-width="90px">
        <el-form-item label="流程编码"
          ><el-input v-model="flowCode"
        /></el-form-item>
        <el-form-item label="业务编号"
          ><el-input v-model="businessId" placeholder="填写业务单据的唯一编号"
        /></el-form-item>
        <el-form-item label="流程变量"
          ><el-input v-model="variablesJson" type="textarea" :rows="3"
        /></el-form-item>
        <el-form-item
          ><el-button
            v-permission="'workflow:engine:start'"
            type="primary"
            :disabled="!enabled || !businessId || !flowCode"
            @click="start"
            >发起流程</el-button
          ></el-form-item
        >
      </el-form>
      <div class="actions">
        <el-input
          v-model="instanceId"
          placeholder="实例 ID"
          aria-label="实例 ID"
        />
        <el-button :disabled="!enabled || !instanceId" @click="load"
          >查询实例</el-button
        >
      </div>
    </el-card>
    <el-card v-if="snapshot" :header="snapshot.instance.flowName">
      <p>
        业务编号：{{ snapshot.instance.businessId }} · 状态：{{
          statusLabels[snapshot.instance.flowStatus] ?? '未知状态'
        }}
      </p>
      <el-input
        v-model="comment"
        placeholder="办理意见"
        aria-label="办理意见"
      />
      <el-table :data="snapshot.tasks">
        <el-table-column prop="nodeName" label="待办节点" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button
              v-permission="'workflow:engine:action'"
              link
              type="primary"
              @click="action(row.id, 'PASS')"
              >通过</el-button
            >
            <el-button
              v-permission="'workflow:engine:action'"
              link
              type="danger"
              @click="action(row.id, 'REJECT')"
              >退回</el-button
            >
          </template>
        </el-table-column>
      </el-table>
      <el-table :data="snapshot.history">
        <el-table-column prop="nodeName" label="历史节点" /><el-table-column
          prop="approver"
          label="办理人"
        />
        <el-table-column label="操作"
          ><template #default="{ row }">{{
            row.skipType === 'PASS'
              ? '通过'
              : row.skipType === 'REJECT'
                ? '退回'
                : '其他操作'
          }}</template></el-table-column
        ><el-table-column prop="message" label="意见" />
      </el-table>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { useEngineWorkbench } from './hooks'
import WorkflowModelEditor from './WorkflowModelEditor.vue'

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
  enabled,
  busy,
  definitionId,
  instanceId,
  businessId,
  flowCode,
  variablesJson,
  comment,
  snapshot,
  modelJson,
  importDefinition,
  loadDefinition,
  publish,
  start,
  action,
  load,
} = useEngineWorkbench()
</script>
<style scoped>
.engine-workbench {
  display: grid;
  gap: 16px;
}
.actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  align-items: center;
}
.actions .el-input {
  max-width: 360px;
}
</style>
