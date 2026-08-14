<template>
  <div>
    <el-form :inline="true" :model="table.query" class="mb-4">
      <el-form-item label="租户编码">
        <el-input
          v-model="table.query.tenantCode"
          clearable
          placeholder="请输入租户编码"
        />
      </el-form-item>
      <el-form-item label="租户名称">
        <el-input
          v-model="table.query.tenantName"
          clearable
          placeholder="请输入租户名称"
        />
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
      <el-button
        v-permission="'system:tenant:add'"
        type="primary"
        @click="form.openAdd"
        >新增租户</el-button
      >
    </div>

    <el-table :data="table.data" v-loading="table.loading" border>
      <el-table-column prop="tenantCode" label="租户编码" width="140" />
      <el-table-column prop="tenantName" label="租户名称" min-width="160" />
      <el-table-column prop="contactUser" label="联系人" width="120" />
      <el-table-column prop="contactPhone" label="联系电话" width="140" />
      <el-table-column label="套餐" width="140">
        <template #default="{ row }">{{ packageName(row.packageId) }}</template>
      </el-table-column>
      <el-table-column prop="accountCount" label="账号上限" width="100" />
      <el-table-column prop="expireTime" label="到期时间" width="180" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">
            {{ row.status === 0 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'system:tenant:edit'"
            link
            type="primary"
            @click="form.openEdit(row)"
            >编辑</el-button
          >
          <el-button
            v-if="row.id !== 1 && row.status === 0"
            v-permission="'system:tenant:remove'"
            link
            type="danger"
            @click="deactivate(row.id)"
          >
            停用
          </el-button>
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

    <el-dialog
      v-model="form.visible"
      :title="form.title + '租户'"
      width="620px"
    >
      <el-form :model="form.model" label-width="110px">
        <el-form-item label="租户编码">
          <el-input
            v-model="form.model.tenantCode"
            :disabled="form.mode === 'edit' && form.model.id === 1"
          />
        </el-form-item>
        <el-form-item label="租户名称">
          <el-input v-model="form.model.tenantName" />
        </el-form-item>
        <el-form-item label="套餐">
          <el-select v-model="form.model.packageId" style="width: 100%">
            <el-option
              v-for="item in packages"
              :key="item.id"
              :label="item.packageName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.model.contactUser" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.model.contactPhone" />
        </el-form-item>
        <el-form-item label="账号上限">
          <el-input-number
            v-model="form.model.accountCount"
            :min="1"
            :max="100000"
          />
        </el-form-item>
        <el-form-item label="到期时间">
          <el-date-picker
            v-model="form.model.expireTime"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="不设置则长期有效"
            style="width: 100%"
          />
        </el-form-item>
        <template v-if="form.mode === 'add'">
          <el-form-item label="管理员账号">
            <el-input v-model="form.model.adminUsername" />
          </el-form-item>
          <el-form-item label="管理员密码">
            <el-input
              v-model="form.model.adminPassword"
              type="password"
              show-password
            />
          </el-form-item>
        </template>
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
import { useTenantCrud } from './hooks'

const { packages, loadPackages, table, form, actions } = useTenantCrud()

function packageName(packageId: number) {
  return (
    packages.value.find((item) => item.id === packageId)?.packageName ||
    String(packageId)
  )
}

async function submit() {
  const result = await actions.submit()
  if (result.ok) {
    form.close()
    await table.refresh()
  }
}

async function deactivate(id: number) {
  await ElMessageBox.confirm('停用后该租户将无法登录，确认继续？', '提示', {
    type: 'warning',
  })
  await actions.remove(id)
}

onMounted(async () => {
  await Promise.all([loadPackages(), table.refresh()])
})
</script>
