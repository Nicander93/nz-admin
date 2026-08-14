<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2>第三方账号</h2>
        <p>绑定后可使用第三方账号登录。系统不会保存服务商令牌。</p>
      </div>
      <el-dropdown
        v-if="providers.length"
        v-permission="['system:social:bind']"
        @command="handleBind"
      >
        <el-button type="primary" :loading="authorizing">
          绑定账号
          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="provider in providers"
              :key="provider.code"
              :command="provider.code"
              :disabled="boundProviders.has(provider.code)"
            >
              {{ provider.displayName }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-empty
      v-if="!loading && bindings.length === 0"
      description="尚未绑定第三方账号"
    />
    <el-table v-else v-loading="loading" :data="bindings">
      <el-table-column label="服务商" min-width="120">
        <template #default="{ row }">
          {{ row.providerName }}
        </template>
      </el-table-column>
      <el-table-column label="账号" min-width="180">
        <template #default="{ row }">
          <div class="account-cell">
            <el-avatar :size="34" :src="row.avatar">
              {{ (row.nickname || row.username || row.providerName).slice(0, 1) }}
            </el-avatar>
            <div>
              <strong>{{ row.nickname || row.username || '未提供昵称' }}</strong>
              <small v-if="row.username">{{ row.username }}</small>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="180">
        <template #default="{ row }">{{ row.email || '-' }}</template>
      </el-table-column>
      <el-table-column prop="bindTime" label="绑定时间" min-width="170" />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="['system:social:remove']"
            link
            type="danger"
            @click="handleUnbind(row)"
          >
            解除绑定
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  authorizeSocialBinding,
  getSocialProviders,
  listSocialBindings,
  unbindSocialAccount,
  type SocialBinding,
  type SocialProvider,
} from '@/api/system/social'

const loading = ref(false)
const authorizing = ref(false)
const bindings = ref<SocialBinding[]>([])
const providers = ref<SocialProvider[]>([])
const boundProviders = computed(
  () => new Set(bindings.value.map((binding) => binding.provider)),
)

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const [bindingResponse, providerResponse] = await Promise.all([
      listSocialBindings(),
      getSocialProviders(),
    ])
    bindings.value = bindingResponse.data || []
    providers.value = providerResponse.data || []
  } finally {
    loading.value = false
  }
}

async function handleBind(provider: string) {
  authorizing.value = true
  try {
    const response = await authorizeSocialBinding(provider)
    window.location.assign(response.data.authorizeUrl)
  } finally {
    authorizing.value = false
  }
}

async function handleUnbind(binding: SocialBinding) {
  await ElMessageBox.confirm(
    `确定解除与 ${binding.providerName} 的绑定吗？`,
    '解除绑定',
    { type: 'warning' },
  )
  await unbindSocialAccount(binding.id)
  ElMessage.success('已解除绑定')
  await loadData()
}
</script>

<style scoped>
.page-card {
  padding: 20px;
  background: var(--el-bg-color);
  border-radius: 8px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 6px;
}

.page-header p {
  margin: 0;
  color: var(--el-text-color-secondary);
}

.account-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.account-cell div {
  display: grid;
  gap: 2px;
}

.account-cell small {
  color: var(--el-text-color-secondary);
}
</style>
