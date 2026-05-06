<template>
  <div>
    <div class="mb-4">
      <el-button type="primary" @click="form.openAdd()">新增</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="postCode" label="岗位编码" width="150" />
      <el-table-column prop="postName" label="岗位名称" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">
            {{ row.status === 0 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="form.openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="form.visible" :title="form.title" width="500px">
      <el-form :model="form.form" label-width="100px">
        <el-form-item label="岗位编码">
          <el-input v-model="form.form.postCode" />
        </el-form-item>
        <el-form-item label="岗位名称">
          <el-input v-model="form.form.postName" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="form.close">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { usePostCrud } from './hooks'
import { ElMessageBox } from 'element-plus'

const {
  loading, tableData, form,
  handleSubmit, handleDelete, loadData,
} = usePostCrud()

async function onDelete(id: number) {
  await ElMessageBox.confirm('确认删除该岗位？', '提示', { type: 'warning' })
  handleDelete(id)
}

onMounted(() => loadData())
</script>
