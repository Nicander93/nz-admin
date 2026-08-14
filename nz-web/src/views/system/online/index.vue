<template>
  <div>
    <el-form :inline="true" :model="table.query" class="mb-4">
      <el-form-item label="用户名">
        <el-input v-model="table.query.username" clearable placeholder="请输入用户名" @keyup.enter="table.load" />
      </el-form-item>
      <el-form-item label="登录 IP">
        <el-input v-model="table.query.loginIp" clearable placeholder="请输入 IP" @keyup.enter="table.load" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="table.load">查询</el-button>
        <el-button @click="table.reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="table.rows" v-loading="table.loading" border>
      <el-table-column prop="username" label="用户名" min-width="130" />
      <el-table-column prop="tenantCode" label="租户" min-width="120">
        <template #default="{ row }">{{ row.tenantCode || row.tenantId || '—' }}</template>
      </el-table-column>
      <el-table-column prop="deptName" label="部门" min-width="140">
        <template #default="{ row }">{{ row.deptName || '—' }}</template>
      </el-table-column>
      <el-table-column prop="loginIp" label="登录 IP" width="150" />
      <el-table-column prop="loginTime" label="登录时间" width="180" />
      <el-table-column prop="tokenTimeout" label="剩余有效期" width="140">
        <template #default="{ row }">{{ table.formatTimeout(row.tokenTimeout) }}</template>
      </el-table-column>
      <el-table-column prop="userAgent" label="客户端" min-width="220" show-overflow-tooltip />
      <el-table-column prop="tokenValue" label="Token" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'system:online:force'"
            link
            type="danger"
            @click="confirmForceLogout(row.tokenValue, row.username)"
          >
            强制退出
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useOnlineUsers } from './hooks'

const { table, actions } = useOnlineUsers()

async function confirmForceLogout(tokenValue: string, username?: string) {
  await ElMessageBox.confirm(`确认强制退出 ${username || '该用户'}？`, '提示', { type: 'warning' })
  await actions.forceLogout(tokenValue)
}

onMounted(() => table.load())
</script>
