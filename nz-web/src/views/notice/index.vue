<template>
  <div>
    <el-form :inline="true" :model="table.query" class="mb-4">
      <el-form-item label="公告标题">
        <el-input v-model="table.query.title" placeholder="请输入公告标题" clearable />
      </el-form-item>
      <el-form-item label="公告类型">
        <el-select v-model="table.query.type" placeholder="请选择" clearable>
          <el-option label="通知" :value="1" />
          <el-option label="公告" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="table.query.status" placeholder="请选择" clearable>
          <el-option label="正常" :value="0" />
          <el-option label="禁用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="table.refresh">查询</el-button>
        <el-button @click="table.handleResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4">
      <el-button v-permission="'system:notice:add'" type="primary" @click="form.openAdd">新增</el-button>
    </div>

    <el-table :data="table.data" v-loading="table.loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.type === 2 ? 'warning' : 'primary'">
            {{ row.type === 2 ? '公告' : '通知' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">
            {{ row.status === 0 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'system:notice:edit'" link type="primary" @click="form.openEdit(row)">编辑</el-button>
          <el-button v-permission="'system:notice:remove'" link type="danger" @click="actions.remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="mt-4 justify-end"
      v-model:current-page="table.pagination.current"
      v-model:page-size="table.pagination.size"
      :total="table.pagination.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
    />

    <el-dialog v-model="form.visible" :title="form.title" width="700px">
      <el-form :model="form.model" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="form.model.title" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.model.type">
            <el-radio :value="1">通知</el-radio>
            <el-radio :value="2">公告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.model.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.model.content" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.model.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="form.close">取消</el-button>
        <el-button
          v-permission="[form.mode === 'edit' ? 'system:notice:edit' : 'system:notice:add']"
          type="primary"
          @click="actions.submit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useNoticeCrud } from './hooks'

const { table, form, actions } = useNoticeCrud()

onMounted(() => table.loadData())
</script>
