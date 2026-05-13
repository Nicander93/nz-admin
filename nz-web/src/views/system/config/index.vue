<template>
  <div>
    <div class="mb-4">
      <el-button type="primary" @click="form.openAdd()">新增</el-button>
    </div>

    <el-table :data="table.data" v-loading="table.loading" border>
      <el-table-column prop="configName" label="参数名称" />
      <el-table-column prop="configKey" label="参数键名" width="180" />
      <el-table-column prop="configValue" label="参数键值" show-overflow-tooltip />
      <el-table-column prop="configType" label="参数类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.configType === 1 ? 'info' : 'success'">
            {{ row.configType === 1 ? '内置' : '自定义' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">
            {{ row.status === 0 ? '正常' : '停用' }}
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
      <el-form :model="form.model" label-width="100px">
        <el-form-item label="参数名称">
          <el-input v-model="form.model.configName" />
        </el-form-item>
        <el-form-item label="参数键名">
          <el-input v-model="form.model.configKey" />
        </el-form-item>
        <el-form-item label="参数键值">
          <el-input v-model="form.model.configValue" />
        </el-form-item>
        <el-form-item label="参数类型">
          <el-radio-group v-model="form.model.configType">
            <el-radio :value="1">内置</el-radio>
            <el-radio :value="2">自定义</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.model.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.model.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="form.close">取消</el-button>
        <el-button type="primary" @click="actions.submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useConfigCrud } from './hooks'
import { ElMessageBox } from 'element-plus'

const { table, form, actions } = useConfigCrud()

async function onDelete(id: number) {
  await ElMessageBox.confirm('确认删除该参数？', '提示', { type: 'warning' })
  actions.remove(id)
}

onMounted(() => table.loadData())
</script>
