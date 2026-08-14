<template>
  <div class="message-page">
    <el-form :inline="true" :model="table.query">
      <el-form-item label="分类">
        <el-select v-model="table.query.category" clearable style="width: 130px">
          <el-option label="系统" value="system" />
          <el-option label="通知" value="notice" />
          <el-option label="流程" value="workflow" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="table.query.readStatus" clearable style="width: 120px">
          <el-option label="未读" :value="0" />
          <el-option label="已读" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题">
        <el-input v-model="table.query.title" clearable @keyup.enter="search" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="table.reset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="message-page__toolbar">
      <el-button v-permission="'system:message:send'" type="primary" @click="actions.openSend">
        发送消息
      </el-button>
      <el-button v-permission="'system:message:read'" @click="actions.readAll">
        全部已读
      </el-button>
    </div>

    <el-table
      v-loading="table.loading"
      :data="table.rows"
      border
      :row-class-name="rowClassName"
      @row-dblclick="actions.openDetail"
    >
      <el-table-column label="状态" width="78">
        <template #default="{ row }">
          <span class="read-state">
            <i v-if="row.readStatus === 0" class="read-state__dot" />
            {{ row.readStatus === 0 ? '未读' : '已读' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="分类" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="categoryType(row.category)">
            {{ categoryLabel(row.category) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="210" show-overflow-tooltip />
      <el-table-column prop="summary" label="摘要" min-width="280" show-overflow-tooltip />
      <el-table-column prop="source" label="来源" width="120" />
      <el-table-column prop="createTime" label="发送时间" width="180" />
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'system:message:query'" link type="primary" @click="actions.openDetail(row)">
            查看
          </el-button>
          <el-button
            v-if="row.readStatus === 0"
            v-permission="'system:message:read'"
            link
            type="success"
            @click="actions.read(row)"
          >
            已读
          </el-button>
          <el-button v-permission="'system:message:remove'" link type="danger" @click="actions.remove(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="table.query.pageNum"
      v-model:page-size="table.query.pageSize"
      :total="table.total"
      class="message-page__pagination"
      layout="total, sizes, prev, pager, next"
      @current-change="table.load"
      @size-change="search"
    />

    <el-dialog v-model="detail.visible" title="消息详情" width="680px">
      <div v-loading="detail.loading" class="message-detail">
        <template v-if="detail.data">
          <h3>{{ detail.data.title }}</h3>
          <div class="message-detail__meta">
            <el-tag size="small">{{ categoryLabel(detail.data.category) }}</el-tag>
            <span>{{ detail.data.source }}</span>
            <span>{{ detail.data.createTime }}</span>
          </div>
          <p class="message-detail__content">{{ detail.data.content }}</p>
          <el-button v-if="detail.data.path" type="primary" @click="goToMessagePath">
            前往处理
          </el-button>
        </template>
      </div>
    </el-dialog>

    <el-dialog v-model="sendDialog.visible" title="发送站内消息" width="680px">
      <el-form :model="sendForm" label-width="100px">
        <el-form-item label="分类">
          <el-radio-group v-model="sendForm.category">
            <el-radio value="system">系统</el-radio>
            <el-radio value="notice">通知</el-radio>
            <el-radio value="workflow">流程</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="接收范围">
          <el-radio-group v-model="sendForm.targetType">
            <el-radio value="ALL">全部启用用户</el-radio>
            <el-radio value="USERS">指定用户</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="sendForm.targetType === 'USERS'" label="接收用户">
          <el-select v-model="sendForm.userIds" multiple filterable style="width: 100%">
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="`${user.nickname}（${user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="sendForm.title" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="sendForm.summary" maxlength="500" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="sendForm.content" type="textarea" :rows="6" maxlength="10000" show-word-limit />
        </el-form-item>
        <el-form-item label="站内路径">
          <el-input v-model="sendForm.path" placeholder="/system/user" />
        </el-form-item>
        <el-form-item label="扩展 JSON">
          <el-input v-model="sendForm.dataJson" type="textarea" :rows="3" placeholder='{"businessId": 1}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sendDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="sendDialog.loading" @click="actions.submitSend">
          发送
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { SystemMessage } from '@/api/system/message'
import { messageCategoryLabels, useMessageCenter } from './hooks'

const router = useRouter()
const { table, detail, sendDialog, sendForm, userOptions, actions } = useMessageCenter()

function search() {
  table.query.pageNum = 1
  return table.load()
}

function rowClassName({ row }: { row: SystemMessage; rowIndex: number }) {
  return row.readStatus === 0 ? 'message-row--unread' : ''
}

function categoryType(category: SystemMessage['category']): 'success' | 'warning' | 'info' {
  if (category === 'workflow') return 'warning'
  if (category === 'notice') return 'success'
  return 'info'
}

function categoryLabel(category: unknown): string {
  if (typeof category === 'string' && category in messageCategoryLabels) {
    return messageCategoryLabels[category as keyof typeof messageCategoryLabels]
  }
  return '其他'
}

async function goToMessagePath() {
  const path = detail.data?.path
  if (!path) return
  detail.visible = false
  await router.push(path)
}

onMounted(table.load)
</script>

<style scoped>
.message-page__toolbar { display: flex; gap: 8px; margin-bottom: 16px; }
.message-page__pagination { justify-content: flex-end; margin-top: 16px; }
.read-state { display: inline-flex; align-items: center; gap: 6px; }
.read-state__dot { width: 7px; height: 7px; border-radius: 50%; background: var(--el-color-primary); }
.message-detail h3 { margin: 0 0 12px; font-size: 19px; }
.message-detail__meta { display: flex; align-items: center; gap: 12px; color: var(--el-text-color-secondary); font-size: 13px; }
.message-detail__content { min-height: 120px; margin: 20px 0; white-space: pre-wrap; line-height: 1.75; }
:deep(.message-row--unread td) { font-weight: 600; background: var(--el-color-primary-light-9) !important; }
</style>
