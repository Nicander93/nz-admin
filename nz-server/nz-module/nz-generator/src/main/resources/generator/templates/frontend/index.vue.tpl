<template>
  <div>
    <el-form :inline="true" :model="table.query" class="mb-4">
@@VUE_QUERY_FIELDS@@
      <el-form-item>
        <el-button type="primary" @click="table.refresh">查询</el-button>
        <el-button @click="table.resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4">
      <el-button v-permission="'@@PERMISSION_PREFIX@@:add'" type="primary" @click="form.openAdd">新增@@FEATURE_HTML@@</el-button>
    </div>

    <el-table v-loading="table.loading" :data="table.data" border>
@@VUE_COLUMNS@@
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'@@PERMISSION_PREFIX@@:edit'" link type="primary" @click="form.openEdit(row)">编辑</el-button>
          <el-button v-permission="'@@PERMISSION_PREFIX@@:remove'" link type="danger" @click="remove(row.@@PK_FIELD@@)">删除</el-button>
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

    <el-dialog v-model="form.visible" :title="form.title" width="620px">
      <el-form :model="form.model" label-width="110px">
@@VUE_FORM_FIELDS@@
      </el-form>
      <template #footer>
        <el-button @click="form.close">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { use@@CLASS@@Crud } from './hooks'

const { table, form, actions } = use@@CLASS@@Crud()

async function submit() {
  const result = await actions.submit()
  if (result.ok) {
    form.close()
    await table.refresh()
  }
}

async function remove(@@PK_FIELD@@: @@TS_PK_TYPE@@) {
  await ElMessageBox.confirm('确认删除该@@FEATURE_TS@@？', '提示', { type: 'warning' })
  await actions.remove(@@PK_FIELD@@)
}

onMounted(table.refresh)
</script>
