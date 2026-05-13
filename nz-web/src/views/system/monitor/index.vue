<template>
  <div class="monitor-page">
    <div class="monitor-page__header">
      <h1 class="monitor-page__title">运行状态</h1>
      <el-button type="primary" @click="actions.refresh">刷新</el-button>
    </div>

    <el-row :gutter="20" v-loading="table.loading">
      <el-col :xs="24" :sm="12" :lg="6">
        <section class="monitor-card">
          <div class="monitor-card__label">数据库</div>
          <div class="monitor-card__value">
            <el-tag :type="table.summary?.databaseOk ? 'success' : 'danger'" size="large">
              {{ table.summary?.databaseOk ? '正常' : '异常' }}
            </el-tag>
          </div>
          <p v-if="!table.summary?.databaseOk" class="monitor-card__hint">
            {{ table.summary?.databaseMessage }}
          </p>
        </section>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <section class="monitor-card">
          <div class="monitor-card__label">堆内存已用</div>
          <div class="monitor-card__value">{{ table.formatBytes(table.summary?.heapUsedBytes ?? 0) }}</div>
          <p class="monitor-card__hint">上限 {{ table.formatBytes(table.summary?.heapMaxBytes ?? 0) }}</p>
        </section>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <section class="monitor-card">
          <div class="monitor-card__label">运行时间</div>
          <div class="monitor-card__value">
            {{ table.formatDuration(table.summary?.uptimeMs ?? 0) }}
          </div>
        </section>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <section class="monitor-card">
          <div class="monitor-card__label">CPU 逻辑核</div>
          <div class="monitor-card__value">{{ table.summary?.availableProcessors ?? '—' }}</div>
        </section>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { useMonitorSummary } from './hooks'

const { table, actions } = useMonitorSummary()
</script>

<style scoped>
.monitor-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--nz-space-page, 18px);
}
.monitor-page__title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--nz-color-text-1);
}
.monitor-card {
  background: var(--nz-color-bg-elevated);
  border: 1px solid var(--nz-color-border);
  border-radius: var(--nz-radius-xl);
  box-shadow: var(--nz-shadow-soft);
  padding: 16px 18px;
  margin-bottom: 16px;
  min-height: 120px;
}
.monitor-card__label {
  font-size: 13px;
  color: var(--nz-color-text-2);
  margin-bottom: 8px;
}
.monitor-card__value {
  font-size: 18px;
  font-weight: 600;
  color: var(--nz-color-text-1);
}
.monitor-card__hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--nz-color-text-3);
  line-height: 1.4;
}
</style>
