<template>
  <div>
    <el-form :inline="true" :model="table.query" class="mb-4">
      <el-form-item label="条目名称">
        <el-input
          v-model="table.query.name"
          clearable
          placeholder="请输入条目名称"
        />
      </el-form-item>
      <el-form-item label="分类">
        <el-input
          v-model="table.query.category"
          clearable
          placeholder="请输入分类"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="table.query.status" clearable placeholder="全部">
          <el-option label="启用" :value="0" />
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
        v-permission="'demo:item:add'"
        type="primary"
        @click="form.openAdd"
        >新增条目</el-button
      >
      <el-button v-permission="'demo:item:import'" @click="excel.openImport"
        >导入</el-button
      >
      <el-button v-permission="'demo:item:export'" @click="excel.exportCurrent"
        >按筛选导出</el-button
      >
    </div>

    <el-table v-loading="table.loading" :data="table.data" border>
      <el-table-column prop="name" label="条目名称" min-width="180" />
      <el-table-column prop="category" label="分类" width="160" />
      <el-table-column prop="sort" label="排序" width="90" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'info'">
            {{ row.status === 0 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'demo:item:edit'"
            link
            type="primary"
            @click="form.openEdit(row)"
            >编辑</el-button
          >
          <el-button
            v-permission="'demo:item:remove'"
            link
            type="danger"
            @click="remove(row.id)"
            >删除</el-button
          >
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
      v-model="excel.importVisible"
      title="导入示例条目"
      width="min(900px, calc(100vw - 32px))"
      destroy-on-close
      :before-close="excel.beforeClose"
    >
      <div class="mb-4 flex flex-wrap items-center gap-3">
        <el-button link type="primary" @click="excel.downloadTemplate"
          >下载导入模板</el-button
        >
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          accept=".xlsx"
          :on-change="excel.selectFile"
          :disabled="excel.importing"
        >
          <el-button
            type="primary"
            :loading="excel.previewing"
            :disabled="excel.importing"
            >选择 Excel 文件</el-button
          >
        </el-upload>
        <span v-if="excel.selectedFile" class="text-sm text-gray-500">{{
          excel.selectedFile.name
        }}</span>
      </div>
      <el-alert
        v-if="excel.preview"
        :closable="false"
        class="mb-3"
        :title="`共 ${excel.preview.total} 行，有效 ${excel.preview.validCount} 行，错误 ${excel.preview.errors.length} 项`"
        :type="excel.preview.errors.length ? 'warning' : 'success'"
      />
      <el-table
        v-if="excel.preview?.errors.length"
        :data="excel.preview.errors"
        border
        max-height="220"
        class="mb-4"
      >
        <el-table-column prop="rowNum" label="Excel 行号" width="120" />
        <el-table-column prop="message" label="错误原因" />
      </el-table>
      <el-table
        v-if="excel.preview?.rows.length"
        :data="excel.preview.rows"
        border
        max-height="300"
      >
        <el-table-column prop="name" label="条目名称" min-width="160" />
        <el-table-column prop="category" label="分类" width="140" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button :disabled="excel.importing" @click="excel.closeImport"
          >关闭</el-button
        >
        <el-button
          type="primary"
          :disabled="
            !excel.preview ||
            excel.preview.errors.length > 0 ||
            excel.preview.validCount === 0 ||
            excel.importing
          "
          :loading="excel.importing"
          @click="excel.confirmImport"
          >确认导入</el-button
        >
      </template>
    </el-dialog>

    <el-dialog v-model="form.visible" :title="form.title" width="520px">
      <el-form :model="form.model" label-width="90px">
        <el-form-item label="条目名称">
          <el-input v-model="form.model.name" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.model.category" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.model.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.model.status">
            <el-radio :value="0">启用</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="form.model.remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
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
import { ElMessageBox } from 'element-plus'
import { onMounted } from 'vue'

import { useDemoItemCrud } from './hooks'

const { table, form, actions, excel } = useDemoItemCrud()

async function submit() {
  const result = await actions.submit()
  if (result.ok) {
    form.close()
    await table.refresh()
  }
}

async function remove(id: number) {
  await ElMessageBox.confirm('确认删除该示例条目？', '提示', {
    type: 'warning',
  })
  await actions.remove(id)
}

onMounted(() => table.refresh())
</script>
