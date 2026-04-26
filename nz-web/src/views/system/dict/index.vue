<template>
  <div>
    <el-form :inline="true" :model="typeCrud.query" class="mb-4">
      <el-form-item label="字典名称">
        <el-input v-model="typeCrud.query.name" placeholder="请输入" clearable />
      </el-form-item>
      <el-form-item label="字典类型">
        <el-input v-model="typeCrud.query.type" placeholder="请输入" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="typeCrud.refresh">查询</el-button>
        <el-button @click="handleTypeResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4">
      <el-button type="primary" @click="typeCrud.toAdd">新增类型</el-button>
    </div>

    <el-table :data="typeCrud.data.value" v-loading="typeCrud.loading.value" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="字典名称" />
      <el-table-column prop="type" label="字典类型">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDataPanel(row)">
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
          <el-button link type="primary" @click="typeCrud.toEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="typeCrud.remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="mt-4 justify-end"
      v-model:current-page="typeCrud.pagination.current"
      v-model:page-size="typeCrud.pagination.size"
      :total="typeCrud.pagination.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
    />

    <el-dialog v-model="typeCrud.visible.value" :title="typeCrud.title.value" width="500px">
      <el-form :model="typeCrud.form" label-width="100px">
        <el-form-item label="字典名称">
          <el-input v-model="typeCrud.form.name" />
        </el-form-item>
        <el-form-item label="字典类型">
          <el-input v-model="typeCrud.form.type" :disabled="typeCrud.mode.value === 'edit'" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="typeCrud.form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeCrud.form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeCrud.close">取消</el-button>
        <el-button type="primary" @click="typeCrud.submit">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="dataDrawerVisible" :title="`字典数据 - ${currentDictType}`" size="600px">
      <div class="mb-4">
        <el-button type="primary" @click="openDataAdd">新增数据</el-button>
      </div>
      <el-table :data="dataList" v-loading="dataLoading" border>
        <el-table-column prop="label" label="字典标签" />
        <el-table-column prop="value" label="字典值" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="dataForm.openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDeleteData(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="dataForm.visible.value" :title="dataForm.title.value" width="500px" append-to-body>
        <el-form :model="dataForm.form" label-width="100px">
          <el-form-item label="字典标签">
            <el-input v-model="dataForm.form.label" />
          </el-form-item>
          <el-form-item label="字典值">
            <el-input v-model="dataForm.form.value" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="dataForm.form.sort" :min="0" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="dataForm.form.status">
              <el-radio :value="0">正常</el-radio>
              <el-radio :value="1">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="dataForm.form.remark" type="textarea" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dataForm.close">取消</el-button>
          <el-button type="primary" @click="handleDataSubmit">确定</el-button>
        </template>
      </el-dialog>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useDictCrud } from './hooks'

const {
  typeCrud,
  dataDrawerVisible, dataLoading, dataList, currentDictType, dataForm,
  openDataPanel, openDataAdd, handleDataSubmit, handleDeleteData, handleTypeResetQuery,
} = useDictCrud()

onMounted(() => typeCrud.refresh())
</script>
