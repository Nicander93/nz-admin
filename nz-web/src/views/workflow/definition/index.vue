<template>
  <div>
    <el-alert type="info" :closable="false" class="mb-4">
      同一流程编码按版本管理。草稿通过模型校验后才能发布；发布新版本会自动将旧版本标记为失效。
    </el-alert>

    <el-form :inline="true" :model="query" class="mb-4">
      <el-form-item label="流程编码">
        <el-input v-model="query.flowCode" clearable placeholder="例如 leave_apply" />
      </el-form-item>
      <el-form-item label="流程名称">
        <el-input v-model="query.flowName" clearable placeholder="请输入流程名称" />
      </el-form-item>
      <el-form-item label="流程分类">
        <el-tree-select
          v-model="query.categoryId"
          :data="categories"
          :props="categoryProps"
          value-key="categoryId"
          node-key="categoryId"
          check-strictly
          clearable
          placeholder="全部分类"
          style="width: 190px"
        />
      </el-form-item>
      <el-form-item label="发布状态">
        <el-select v-model="query.publishStatus" clearable placeholder="全部" style="width: 130px">
          <el-option label="草稿" :value="0" />
          <el-option label="已发布" :value="1" />
          <el-option label="已失效" :value="9" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4">
      <el-button v-permission="'workflow:definition:add'" type="primary" @click="openCreate">
        新增流程
      </el-button>
      <el-upload
        v-permission="'workflow:definition:import'"
        :show-file-list="false"
        :http-request="uploadDefinition"
        accept=".json,application/json"
      >
        <el-button>导入 JSON</el-button>
      </el-upload>
    </div>

    <el-table v-loading="loading" :data="definitions" border>
      <el-table-column prop="flowCode" label="流程编码" min-width="150" />
      <el-table-column prop="flowName" label="流程名称" min-width="180" />
      <el-table-column prop="categoryName" label="分类" min-width="130" />
      <el-table-column prop="versionNo" label="版本" width="80" align="center">
        <template #default="{ row }">v{{ row.versionNo }}</template>
      </el-table-column>
      <el-table-column label="发布状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="publishTagType(row.publishStatus)">{{ publishStatusText(row.publishStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="运行状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.publishStatus === 1" :type="row.activityStatus === 1 ? 'success' : 'warning'">
            {{ row.activityStatus === 1 ? '激活' : '挂起' }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" width="180" />
      <el-table-column label="操作" width="430" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.publishStatus === 0"
            v-permission="'workflow:definition:edit'"
            link
            type="primary"
            @click="openEdit(row.definitionId)"
          >编辑</el-button>
          <el-button
            v-if="row.publishStatus === 0"
            v-permission="'workflow:definition:publish'"
            link
            type="success"
            @click="confirmPublish(row.definitionId)"
          >发布</el-button>
          <el-button
            v-if="row.publishStatus === 1"
            v-permission="'workflow:definition:publish'"
            link
            type="warning"
            @click="confirmUnpublish(row.definitionId)"
          >取消发布</el-button>
          <el-button
            v-if="row.publishStatus === 1"
            v-permission="'workflow:definition:active'"
            link
            type="primary"
            @click="setActive(row.definitionId, row.activityStatus !== 1)"
          >{{ row.activityStatus === 1 ? '挂起' : '激活' }}</el-button>
          <el-button v-permission="'workflow:definition:copy'" link type="primary" @click="openCopy(row)">
            复制
          </el-button>
          <el-button v-permission="'workflow:definition:export'" link @click="exportFile(row)">
            导出
          </el-button>
          <el-button
            v-if="row.publishStatus !== 1"
            v-permission="'workflow:definition:remove'"
            link
            type="danger"
            @click="confirmRemove(row.definitionId)"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.pageNum"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      class="mt-4 justify-end"
      layout="total, sizes, prev, pager, next"
      @current-change="load"
      @size-change="load"
    />

    <el-dialog
      v-model="editorVisible"
      :title="editorMode === 'create' ? '新增流程定义' : '编辑流程定义草稿'"
      width="860px"
      destroy-on-close
    >
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="流程编码" required>
              <el-input v-model="form.flowCode" :disabled="editorMode === 'update'" maxlength="40" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="流程名称" required>
              <el-input v-model="form.flowName" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="流程分类" required>
              <el-tree-select
                v-model="form.categoryId"
                :data="categories"
                :props="categoryProps"
                value-key="categoryId"
                node-key="categoryId"
                check-strictly
                default-expand-all
                class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="表单路径">
              <el-input v-model="form.formPath" placeholder="例如 /leave/apply" maxlength="200" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="模型 JSON" required>
          <el-input v-model="form.modelJson" type="textarea" :rows="17" class="model-editor" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="copyVisible" title="复制流程定义" width="520px">
      <el-form :model="copyForm" label-width="100px">
        <el-form-item label="新流程编码" required>
          <el-input v-model="copyForm.flowCode" maxlength="40" />
        </el-form-item>
        <el-form-item label="新流程名称" required>
          <el-input v-model="copyForm.flowName" maxlength="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCopy">复制为草稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { ElMessageBox, type UploadRequestOptions } from 'element-plus'
import { useWorkflowDefinition } from './hooks'

const categoryProps = { label: 'categoryName', value: 'categoryId', children: 'children' }
const {
  loading, definitions, categories, total, editorVisible, editorMode, copyVisible,
  query, form, copyForm, load, loadCategories, resetQuery, openCreate, openEdit, submit,
  publish, unpublish, setActive, openCopy, submitCopy, importFile, exportFile, remove,
} = useWorkflowDefinition()

function publishStatusText(status: number) {
  return status === 0 ? '草稿' : status === 1 ? '已发布' : '已失效'
}

function publishTagType(status: number) {
  return status === 0 ? 'warning' : status === 1 ? 'success' : 'info'
}

async function confirmPublish(definitionId: number) {
  await ElMessageBox.confirm('发布后当前已发布版本会自动失效，确认继续？', '发布流程', { type: 'warning' })
  await publish(definitionId)
}

async function confirmUnpublish(definitionId: number) {
  await ElMessageBox.confirm('确认取消发布并转回草稿？', '取消发布', { type: 'warning' })
  await unpublish(definitionId)
}

async function confirmRemove(definitionId: number) {
  await ElMessageBox.confirm('确认删除该流程定义版本？', '提示', { type: 'warning' })
  await remove(definitionId)
}

async function uploadDefinition(options: UploadRequestOptions) {
  await importFile(options.file)
}

onMounted(async () => {
  await loadCategories()
  await load()
})
</script>

<style scoped>
.mb-4 :deep(.el-upload) {
  display: inline-flex;
  margin-left: 12px;
}

.model-editor :deep(textarea) {
  font: 13px/1.55 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}
</style>
