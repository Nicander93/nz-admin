<template>
  <div class="sms-page">
    <el-alert type="info" :closable="false" class="mb-4">
      日志渠道用于本地验证；Webhook 渠道会向配置的 endpoint 发送标准 JSON 请求。
    </el-alert>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="短信渠道" name="channels">
        <el-form :inline="true" :model="channelQuery">
          <el-form-item label="关键词"><el-input v-model="channelQuery.keyword" clearable /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="channelQuery.status" clearable style="width: 120px">
              <el-option label="启用" :value="0" /><el-option label="停用" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="loadChannels">查询</el-button></el-form-item>
        </el-form>
        <el-button v-permission="'system:sms:add'" type="primary" class="mb-4" @click="openChannel()">新增渠道</el-button>
        <el-table v-loading="channels.loading" :data="channels.records" border>
          <el-table-column prop="channelCode" label="渠道编码" min-width="130" />
          <el-table-column prop="channelName" label="渠道名称" min-width="140" />
          <el-table-column prop="providerCode" label="供应商" width="110" />
          <el-table-column prop="endpoint" label="Webhook 地址" min-width="220" show-overflow-tooltip />
          <el-table-column label="密钥" width="90">
            <template #default="{ row }">{{ row.accessKeySecretConfigured ? '已配置' : '未配置' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }"><el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '启用' : '停用' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button v-permission="'system:sms:edit'" link type="primary" @click="openChannel(row)">编辑</el-button>
              <el-button v-permission="'system:sms:remove'" link type="danger" @click="removeChannel(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="channels.pageNum" v-model:page-size="channels.pageSize"
          :total="channels.total" class="mt-4 justify-end" layout="total, sizes, prev, pager, next"
          @current-change="loadChannels" @size-change="loadChannels" />
      </el-tab-pane>

      <el-tab-pane label="短信模板" name="templates">
        <el-form :inline="true" :model="templateQuery">
          <el-form-item label="渠道">
            <el-select v-model="templateQuery.channelId" clearable style="width: 180px">
              <el-option v-for="item in channelOptions" :key="item.id" :label="item.channelName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词"><el-input v-model="templateQuery.keyword" clearable /></el-form-item>
          <el-form-item><el-button type="primary" @click="loadTemplates">查询</el-button></el-form-item>
        </el-form>
        <div class="mb-4">
          <el-button v-permission="'system:sms:add'" type="primary" @click="openTemplate()">新增模板</el-button>
          <el-button v-permission="'system:sms:send'" @click="openSend()">测试发送</el-button>
        </div>
        <el-table v-loading="templates.loading" :data="templates.records" border>
          <el-table-column prop="templateCode" label="模板编码" min-width="130" />
          <el-table-column prop="templateName" label="模板名称" min-width="140" />
          <el-table-column prop="channelName" label="渠道" min-width="130" />
          <el-table-column prop="content" label="模板内容" min-width="260" show-overflow-tooltip />
          <el-table-column label="状态" width="80">
            <template #default="{ row }"><el-tag :type="row.status === 0 ? 'success' : 'info'">{{ row.status === 0 ? '启用' : '停用' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button v-permission="'system:sms:send'" link type="success" @click="openSend(row)">发送</el-button>
              <el-button v-permission="'system:sms:edit'" link type="primary" @click="openTemplate(row)">编辑</el-button>
              <el-button v-permission="'system:sms:remove'" link type="danger" @click="removeTemplate(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="templates.pageNum" v-model:page-size="templates.pageSize"
          :total="templates.total" class="mt-4 justify-end" layout="total, sizes, prev, pager, next"
          @current-change="loadTemplates" @size-change="loadTemplates" />
      </el-tab-pane>

      <el-tab-pane label="发送记录" name="logs">
        <el-form :inline="true" :model="logQuery">
          <el-form-item label="发送状态">
            <el-select v-model="logQuery.sendStatus" clearable style="width: 140px">
              <el-option label="发送中" value="PENDING" /><el-option label="成功" value="SUCCESS" /><el-option label="失败" value="FAILED" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="loadLogs">查询</el-button></el-form-item>
        </el-form>
        <el-table v-loading="logs.loading" :data="logs.records" border>
          <el-table-column prop="phoneNumberMasked" label="手机号" width="140" />
          <el-table-column prop="templateCode" label="模板编码" width="150" />
          <el-table-column prop="content" label="发送内容" min-width="260" show-overflow-tooltip />
          <el-table-column prop="sendStatus" label="状态" width="100" />
          <el-table-column prop="providerMessageId" label="供应商消息 ID" min-width="190" show-overflow-tooltip />
          <el-table-column prop="errorMessage" label="错误信息" min-width="180" show-overflow-tooltip />
          <el-table-column prop="sendTime" label="发送时间" width="180" />
        </el-table>
        <el-pagination v-model:current-page="logs.pageNum" v-model:page-size="logs.pageSize"
          :total="logs.total" class="mt-4 justify-end" layout="total, sizes, prev, pager, next"
          @current-change="loadLogs" @size-change="loadLogs" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="channelDialog.visible" :title="channelDialog.editing ? '编辑短信渠道' : '新增短信渠道'" width="620px">
      <el-form :model="channelForm" label-width="120px">
        <el-form-item label="渠道编码"><el-input v-model="channelForm.channelCode" /></el-form-item>
        <el-form-item label="渠道名称"><el-input v-model="channelForm.channelName" /></el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="channelForm.providerCode" style="width: 100%">
            <el-option label="日志（本地验证）" value="log" /><el-option label="Webhook" value="webhook" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="channelForm.providerCode === 'webhook'" label="Webhook 地址"><el-input v-model="channelForm.endpoint" /></el-form-item>
        <el-form-item label="AccessKey"><el-input v-model="channelForm.accessKeyId" /></el-form-item>
        <el-form-item label="Secret"><el-input v-model="channelForm.accessKeySecret" show-password :placeholder="channelDialog.editing ? '留空表示不修改' : ''" /></el-form-item>
        <el-form-item label="签名"><el-input v-model="channelForm.signature" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="channelForm.status"><el-radio :value="0">启用</el-radio><el-radio :value="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="channelForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="channelDialog.visible = false">取消</el-button><el-button type="primary" @click="saveChannel">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="templateDialog.visible" :title="templateDialog.editing ? '编辑短信模板' : '新增短信模板'" width="620px">
      <el-form :model="templateForm" label-width="130px">
        <el-form-item label="短信渠道"><el-select v-model="templateForm.channelId" style="width: 100%"><el-option v-for="item in channelOptions" :key="item.id" :label="item.channelName" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="模板编码"><el-input v-model="templateForm.templateCode" /></el-form-item>
        <el-form-item label="模板名称"><el-input v-model="templateForm.templateName" /></el-form-item>
        <el-form-item label="供应商模板 ID"><el-input v-model="templateForm.providerTemplateId" /></el-form-item>
        <el-form-item label="模板内容"><el-input v-model="templateForm.content" type="textarea" :rows="5" placeholder="变量使用 {{code}} 格式" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="templateForm.status"><el-radio :value="0">启用</el-radio><el-radio :value="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="templateForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="templateDialog.visible = false">取消</el-button><el-button type="primary" @click="saveTemplate">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="sendDialog.visible" title="测试发送短信" width="560px">
      <el-form :model="sendForm" label-width="100px">
        <el-form-item label="模板"><el-select v-model="sendForm.templateId" style="width: 100%"><el-option v-for="item in templates.records" :key="item.id" :label="item.templateName" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="手机号"><el-input v-model="sendForm.phoneNumber" /></el-form-item>
        <el-form-item label="模板参数"><el-input v-model="sendForm.parametersText" type="textarea" :rows="7" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="sendDialog.visible = false">取消</el-button><el-button type="primary" :loading="sendDialog.loading" @click="send">发送</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useSmsManagement } from './hooks'

const activeTab = ref('channels')
const {
  channels, templates, logs, channelQuery, templateQuery, logQuery, channelOptions,
  channelDialog, channelForm, templateDialog, templateForm, sendDialog, sendForm,
  loadChannels, loadChannelOptions, loadTemplates, loadLogs,
  openChannel, saveChannel, removeChannel, openTemplate, saveTemplate, removeTemplate, openSend, send,
} = useSmsManagement()

watch(activeTab, value => {
  if (value === 'templates') void loadTemplates()
  if (value === 'logs') void loadLogs()
})

onMounted(() => Promise.all([loadChannels(), loadChannelOptions()]))
</script>

<style scoped>
.sms-page :deep(.el-tabs__content) { overflow: visible; }
</style>
