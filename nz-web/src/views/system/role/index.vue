<template>
  <div>
    <el-form :inline="true" :model="query" class="mb-4">
      <el-form-item label="角色名称">
        <el-input v-model="query.name" placeholder="请输入" clearable />
      </el-form-item>
      <el-form-item label="权限字符">
        <el-input v-model="query.roleKey" placeholder="请输入" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="请选择" clearable>
          <el-option label="正常" :value="0" />
          <el-option label="禁用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="refresh">查询</el-button>
        <el-button @click="handleResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4">
      <el-button type="primary" @click="toAdd">新增</el-button>
    </div>

    <el-table :data="data" v-loading="loading" border>
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
          <el-button link type="primary" @click="toEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="openMenuDialog(row, k => menuTreeRef?.setCheckedKeys(k))">菜单权限</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="mt-4 justify-end"
      v-model:current-page="pagination.current"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
    />

    <el-dialog v-model="visible" :title="title" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="角色名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="权限字符">
          <el-input v-model="form.roleKey" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="menuDialogVisible" title="分配菜单权限" width="500px">
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        :props="{ label: 'name', children: 'children' }"
        show-checkbox
        node-key="id"
        default-expand-all
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignMenus(() => [
          ...menuTreeRef!.getCheckedKeys() as number[],
          ...menuTreeRef!.getHalfCheckedKeys() as number[],
        ])">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoleCrud } from '@/hooks/useRoleCrud'
import type { ElTree } from 'element-plus'

const menuTreeRef = ref<InstanceType<typeof ElTree>>()

const {
  data, loading, pagination, query, refresh, form, visible, title, close, submit,
  toAdd, toEdit, remove, handleResetQuery,
  menuDialogVisible, menuTree, openMenuDialog, handleAssignMenus,
} = useRoleCrud()

onMounted(() => refresh())
</script>
