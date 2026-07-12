<template>
  <div>
    <el-form :inline="true" :model="table.query" class="mb-4">
      <el-form-item label="客户端标识">
        <el-input v-model="table.query.clientId" clearable placeholder="请输入 clientId" />
      </el-form-item>
      <el-form-item label="客户端名称">
        <el-input v-model="table.query.clientName" clearable placeholder="请输入名称" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="table.query.status" clearable placeholder="全部">
          <el-option label="正常" :value="0" />
          <el-option label="停用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="table.refresh">查询</el-button>
        <el-button @click="table.handleResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4">
      <el-button v-permission="'system:client:add'" type="primary" @click="form.openAdd">新增客户端</el-button>
    </div>

    <el-table :data="table.data" v-loading="table.loading" border>
      <el-table-column prop="clientId" label="客户端标识" width="180" />
      <el-table-column prop="clientName" label="客户端名称" min-width="160" />
      <el-table-column prop="loginType" label="登录类型" width="120" />
      <el-table-column prop="tokenTimeout" label="Token 有效期（秒）" width="160" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">
            {{ row.status === 0 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'system:client:edit'" link type="primary" @click="form.openEdit(row)">编辑</el-button>
          <el-button v-permission="'system:client:remove'" link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="table.pagination.current"
      v-model:page-size="table.pagination.size"
      :total="table.pagination.total"
      :page-sizes="[10, 20, 50]"
      class="mt-4 justify-end"
      layout="total, sizes, prev, pager, next"
      @current-change="table.refresh"
      @size-change="table.refresh"
    />

    <el-dialog v-model="form.visible" :title="form.title" width="520px">
      <el-form :model="form.model" label-width="130px">
        <el-form-item label="客户端标识">
          <el-input v-model="form.model.clientId" :disabled="form.mode === 'edit'" />
        </el-form-item>
        <el-form-item label="客户端名称">
          <el-input v-model="form.model.clientName" />
        </el-form-item>
        <el-form-item label="登录类型">
          <el-select v-model="form.model.loginType" style="width: 100%">
            <el-option label="账号密码" value="account" />
            <el-option label="短信（预留）" value="sms" />
            <el-option label="第三方（预留）" value="social" />
          </el-select>
        </el-form-item>
        <el-form-item label="Token 有效期">
          <el-input-number v-model="form.model.tokenTimeout" :min="60" :max="2592000" />
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
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useClientCrud } from './hooks'

const { table, form, actions } = useClientCrud()

async function submit() {
  const result = await actions.submit()
  if (result.ok) {
    form.close()
    await table.refresh()
  }
}

async function remove(id: number) {
  await ElMessageBox.confirm('确认删除该客户端？', '提示', { type: 'warning' })
  await actions.remove(id)
}

onMounted(() => table.refresh())
</script>
