<template>
  <div>
    <el-form :inline="true" :model="query" class="mb-4">
      <el-form-item label="分类名称">
        <el-input
          v-model="query.categoryName"
          clearable
          placeholder="请输入分类名称"
          @keyup.enter="load"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4">
      <el-button v-permission="'workflow:category:add'" type="primary" @click="openCreate()">
        新增分类
      </el-button>
      <el-button v-permission="'workflow:category:export'" @click="exportFile">导出</el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="categories"
      row-key="categoryId"
      :tree-props="{ children: 'children' }"
      default-expand-all
      border
    >
      <el-table-column prop="categoryName" label="分类名称" min-width="240" />
      <el-table-column prop="orderNum" label="排序" width="90" align="center" />
      <el-table-column label="类型" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.builtIn === 1" type="info">内置</el-tag>
          <el-tag v-else type="success">自定义</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'workflow:category:add'" link type="primary" @click="openCreate(row.categoryId)">
            新增下级
          </el-button>
          <el-button v-permission="'workflow:category:edit'" link type="primary" @click="openEdit(row.categoryId)">
            修改
          </el-button>
          <el-button
            v-if="row.builtIn !== 1"
            v-permission="'workflow:category:remove'"
            link
            type="danger"
            @click="confirmRemove(row.categoryId)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" destroy-on-close>
      <el-form :model="form" label-width="96px">
        <el-form-item label="上级分类" required>
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="treeProps"
            value-key="categoryId"
            node-key="categoryId"
            check-strictly
            default-expand-all
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="分类名称" required>
          <el-input v-model="form.categoryName" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="显示顺序" required>
          <el-input-number v-model="form.orderNum" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { useWorkflowCategory } from './hooks'

const treeProps = {
  label: 'categoryName',
  value: 'categoryId',
  children: 'children',
  disabled: 'disabled',
}

const {
  loading,
  categories,
  query,
  form,
  dialogVisible,
  dialogTitle,
  parentOptions,
  load,
  resetQuery,
  openCreate,
  openEdit,
  submit,
  remove,
  exportFile,
} = useWorkflowCategory()

async function confirmRemove(categoryId: number) {
  await ElMessageBox.confirm('确认删除该流程分类？', '提示', { type: 'warning' })
  await remove(categoryId)
}

onMounted(load)
</script>
