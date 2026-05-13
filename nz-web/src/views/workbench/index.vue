<template>
  <div class="workbench">
    <div class="workbench__header">
      <h1 class="workbench__title">工作台</h1>
      <p class="workbench__subtitle">常用入口与最近系统活动</p>
    </div>

    <el-row :gutter="20">
      <el-col :xs="24" :md="14">
        <section class="workbench-card">
          <div class="workbench-card__head">
            <span class="workbench-card__title">快捷入口</span>
          </div>
          <div class="workbench-card__body workbench-shortcuts">
            <el-button
              v-for="s in shortcuts"
              :key="s.path"
              class="workbench-shortcuts__btn"
              @click="$router.push(s.path)"
            >
              {{ s.title }}
            </el-button>
          </div>
        </section>
      </el-col>
      <el-col :xs="24" :md="10">
        <section class="workbench-card">
          <div class="workbench-card__head">
            <span class="workbench-card__title">数据概览</span>
            <el-button link type="primary" @click="actions.refresh">刷新</el-button>
          </div>
          <div class="workbench-card__body workbench-meta">
            <p>登录日志与操作日志各展示最近 5 条。</p>
          </div>
        </section>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="workbench__logs">
      <el-col :xs="24" :lg="12">
        <section class="workbench-card" v-loading="table.loading">
          <div class="workbench-card__head">
            <span class="workbench-card__title">最近登录</span>
          </div>
          <el-table :data="table.snapshot?.recentLoginLogs ?? []" size="small" stripe>
            <el-table-column prop="username" label="用户" width="120" />
            <el-table-column prop="ip" label="IP" width="130" />
            <el-table-column prop="loginTime" label="时间" min-width="160" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
                  {{ row.status === 0 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </el-col>
      <el-col :xs="24" :lg="12">
        <section class="workbench-card" v-loading="table.loading">
          <div class="workbench-card__head">
            <span class="workbench-card__title">最近操作</span>
          </div>
          <el-table :data="table.snapshot?.recentOperLogs ?? []" size="small" stripe>
            <el-table-column prop="title" label="标题" min-width="120" />
            <el-table-column prop="operName" label="操作人" width="100" />
            <el-table-column prop="operTime" label="时间" min-width="160" />
          </el-table>
        </section>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { useWorkbenchHome } from './hooks'

const { table, actions, shortcuts } = useWorkbenchHome()
</script>

<style scoped>
.workbench {
  max-width: 1200px;
}
.workbench__header {
  margin-bottom: var(--nz-space-page, 18px);
}
.workbench__title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--nz-color-text-1);
}
.workbench__subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--nz-color-text-2);
}
.workbench-card {
  background: var(--nz-color-bg-elevated);
  border: 1px solid var(--nz-color-border);
  border-radius: var(--nz-radius-xl);
  box-shadow: var(--nz-shadow-soft);
  padding: 16px 18px 18px;
  margin-bottom: 16px;
}
.workbench-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.workbench-card__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--nz-color-text-1);
}
.workbench-shortcuts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.workbench-shortcuts__btn {
  min-height: 40px;
}
.workbench-meta p {
  margin: 0;
  font-size: 13px;
  color: var(--nz-color-text-2);
  line-height: 1.5;
}
.workbench__logs {
  margin-top: 16px;
}
</style>
