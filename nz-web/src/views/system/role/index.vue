<template>
  <div>
    <el-form :inline="true" :model="table.query" class="mb-4">
      <el-form-item label="角色名称">
        <el-input v-model="table.query.name" placeholder="请输入" clearable />
      </el-form-item>
      <el-form-item label="权限字符">
        <el-input v-model="table.query.roleKey" placeholder="请输入" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="table.query.status" placeholder="请选择" clearable>
          <el-option label="正常" :value="0" />
          <el-option label="禁用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item label="数据范围">
        <el-select v-model="table.query.dataScope" placeholder="请选择" clearable>
          <el-option label="全部数据权限" :value="1" />
          <el-option label="自定数据权限" :value="2" />
          <el-option label="本部门数据权限" :value="3" />
          <el-option label="本部门及以下数据权限" :value="4" />
          <el-option label="仅本人数据权限" :value="5" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="table.refresh">查询</el-button>
        <el-button @click="table.handleResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4">
      <el-button v-permission="'system:role:add'" type="primary" @click="form.openAdd">新增</el-button>
    </div>

    <el-table :data="table.data" v-loading="table.loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="角色名称" />
      <el-table-column prop="roleKey" label="权限字符" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">
            {{ row.status === 0 ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-permission="'system:role:edit'" link type="primary" @click="form.openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="menuDialogRef?.open(row)">菜单权限</el-button>
          <el-button v-permission="'system:role:remove'" link type="danger" @click="actions.remove(row.id)">删除</el-button>
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
        <el-form-item label="角色名称">
          <el-input v-model="form.model.name" />
        </el-form-item>
        <el-form-item label="权限字符">
          <el-input v-model="form.model.roleKey" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.model.sort" :min="0" />
        </el-form-item>
         <el-form-item label="状态">
           <el-radio-group v-model="form.model.status">
             <el-radio :value="0">正常</el-radio>
             <el-radio :value="1">禁用</el-radio>
           </el-radio-group>
         </el-form-item>
         <el-form-item label="数据范围">
           <el-radio-group v-model="form.model.dataScope">
             <el-radio :value="1">全部数据权限</el-radio>
             <el-radio :value="2">自定数据权限</el-radio>
             <el-radio :value="3">本部门数据权限</el-radio>
             <el-radio :value="4">本部门及以下数据权限</el-radio>
             <el-radio :value="5">仅本人数据权限</el-radio>
           </el-radio-group>
         </el-form-item>
         <el-form-item label="备注">
           <el-input v-model="form.model.remark" type="textarea" />
         </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="form.close">取消</el-button>
        <el-button
          v-permission="[form.model.id ? 'system:role:edit' : 'system:role:add']"
          type="primary"
          @click="actions.submit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <RoleMenuDialog ref="menuDialogRef" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoleCrud } from './hooks'
import RoleMenuDialog from './RoleMenuDialog.vue'

const { table, form, actions } = useRoleCrud()
const menuDialogRef = ref<InstanceType<typeof RoleMenuDialog>>()

onMounted(() => table.refresh())
</script>
