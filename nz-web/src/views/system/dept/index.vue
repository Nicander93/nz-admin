<template>
  <div>
    <div class="mb-4">
      <el-button type="primary" @click="openAdd()">新增</el-button>
      <el-button @click="toggleExpand">{{ isExpand ? '折叠' : '展开' }}</el-button>
    </div>

    <el-table :data="treeData" v-loading="loading" border row-key="id" :default-expand-all="isExpand">
      <el-table-column prop="name" label="部门名称" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">
            {{ row.status === 0 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAdd(row.id)">新增</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="title" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="上级部门">
          <el-tree-select
            v-model="form.parentId"
            :data="treeData"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            placeholder="请选择上级部门"
            clearable
          />
        </el-form-item>
        <el-form-item label="部门名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useDeptCrud } from './hooks'
import { ElMessageBox } from 'element-plus'

const {
  loading, treeData, isExpand, form, visible, title, close,
  openAdd, openEdit, toggleExpand, handleSubmit, handleDelete, loadData,
} = useDeptCrud()

async function onDelete(id: number) {
  await ElMessageBox.confirm('确认删除该部门？', '提示', { type: 'warning' })
  handleDelete(id)
}

onMounted(() => loadData())
</script>
