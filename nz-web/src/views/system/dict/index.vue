<template>
  <div>
    <el-form :inline="true" :model="table.query" class="mb-4">
      <el-form-item label="字典名称">
        <el-input v-model="table.query.name" placeholder="请输入" clearable />
      </el-form-item>
      <el-form-item label="字典类型">
        <el-input v-model="table.query.type" placeholder="请输入" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="table.refresh">查询</el-button>
        <el-button @click="table.handleResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4">
      <el-button type="primary" @click="form.toAdd">新增类型</el-button>
    </div>

    <el-table :data="table.data" v-loading="table.loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="字典名称" />
      <el-table-column prop="type" label="字典类型">
        <template #default="{ row }">
          <el-button link type="primary" @click="dataDrawerRef?.open(row)">
            {{ row.type }}
          </el-button>
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
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="form.toEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="actions.remove(row.id)">删除</el-button>
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

    <el-dialog v-model="form.visible" :title="form.title" width="500px">
      <el-form :model="form.model" label-width="100px">
        <el-form-item label="字典名称">
          <el-input v-model="form.model.name" />
        </el-form-item>
        <el-form-item label="字典类型">
          <el-input v-model="form.model.type" :disabled="form.mode === 'edit'" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.model.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.model.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="form.close">取消</el-button>
        <el-button type="primary" @click="form.submit">确定</el-button>
      </template>
    </el-dialog>

    <DictDataDrawer ref="dataDrawerRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useDictCrud } from './hooks'
import DictDataDrawer from './DictDataDrawer.vue'

const { table, form, actions } = useDictCrud()
const dataDrawerRef = ref<InstanceType<typeof DictDataDrawer>>()

onMounted(() => table.refresh())
</script>
