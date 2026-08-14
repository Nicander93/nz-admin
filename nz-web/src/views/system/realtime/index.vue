<template>
  <div class="realtime-page">
    <el-row :gutter="16">
      <el-col :xs="24" :lg="9">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>实时连接</span>
              <el-tag :type="statusTagType">{{ statusText }}</el-tag>
            </div>
          </template>

          <el-alert
            title="连接使用 30 秒一次性票据，不会把登录令牌放入长连接地址。"
            type="info"
            :closable="false"
            class="mb-4"
          />

          <el-form label-width="90px">
            <el-form-item label="传输方式">
              <el-radio-group v-model="transport" :disabled="connected">
                <el-radio-button value="SSE">SSE</el-radio-button>
                <el-radio-button value="WEBSOCKET">WebSocket</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="当前节点">
              <el-space wrap>
                <el-tag>SSE {{ stats.sseConnections }}</el-tag>
                <el-tag type="success">WS {{ stats.webSocketConnections }}</el-tag>
                <el-tag type="info">合计 {{ stats.totalConnections }}</el-tag>
              </el-space>
            </el-form-item>
            <el-form-item>
              <el-space>
                <el-button
                  v-permission="'system:realtime:view'"
                  type="primary"
                  :loading="status === 'connecting'"
                  :disabled="connected"
                  @click="connect"
                >
                  建立连接
                </el-button>
                <el-button :disabled="status === 'disconnected'" @click="disconnect">
                  断开
                </el-button>
                <el-button @click="refreshStats">刷新统计</el-button>
              </el-space>
            </el-form-item>
          </el-form>

          <el-divider content-position="left">定向测试</el-divider>
          <el-input
            v-model="testMessage"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
          <el-button
            v-permission="'system:realtime:send'"
            type="success"
            class="send-button"
            :disabled="!connected"
            @click="sendTest"
          >
            发送给我的连接
          </el-button>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="15">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>接收事件（最近 100 条）</span>
              <el-button link type="primary" @click="clearEvents">清空</el-button>
            </div>
          </template>
          <el-table :data="events" height="520" empty-text="建立连接后等待消息">
            <el-table-column prop="type" label="类型" width="130" />
            <el-table-column prop="sentAt" label="时间" width="190">
              <template #default="{ row }">{{ formatTime(row.sentAt) }}</template>
            </el-table-column>
            <el-table-column label="内容" min-width="260">
              <template #default="{ row }">
                <code class="payload">{{ formatPayload(row.payload) }}</code>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { useRealtimeConsole } from './hooks'

const {
  transport,
  status,
  connected,
  events,
  stats,
  testMessage,
  connect,
  disconnect,
  refreshStats,
  sendTest,
  clearEvents,
} = useRealtimeConsole()

const statusText = computed(() => ({
  disconnected: '未连接',
  connecting: '连接中',
  connected: '已连接',
  error: '连接失败',
})[status.value])

const statusTagType = computed(() => ({
  disconnected: 'info',
  connecting: 'warning',
  connected: 'success',
  error: 'danger',
})[status.value] as 'info' | 'warning' | 'success' | 'danger')

function formatPayload(payload: unknown) {
  return typeof payload === 'string' ? payload : JSON.stringify(payload)
}

function formatTime(value: string) {
  return value ? new Date(value).toLocaleString() : '-'
}

onMounted(refreshStats)
onBeforeUnmount(disconnect)
</script>

<style scoped>
.realtime-page {
  padding: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mb-4 {
  margin-bottom: 16px;
}

.send-button {
  width: 100%;
  margin-top: 12px;
}

.payload {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
