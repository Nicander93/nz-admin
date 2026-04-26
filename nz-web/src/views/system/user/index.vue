<template>
  <div>
    <el-form :inline="true" :model="table.query" class="mb-4">
      <el-form-item label="用户名">
        <el-input v-model="table.query.username" placeholder="请输入用户名" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="table.query.status" placeholder="请选择" clearable>
          <el-option label="正常" :value="0" />
          <el-option label="禁用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="table.refresh">查询</el-button>
        <el-button @click="table.handleResetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="mb-4">
      <el-button v-permission="'system:user:add'" type="primary" @click="form.toAdd">新增</el-button>
    </div>

    <el-table :data="table.data" v-loading="table.loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="deptId" label="部门" width="120">
        <template #default="{ row }">
          {{ dept.getName(row.deptId) }}
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="phone" label="手机号" />
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
          <el-button v-permission="'system:user:edit'" link type="primary" @click="form.toEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="role.openDialog(row)">分配角色</el-button>
          <el-button v-permission="'system:user:remove'" link type="danger" @click="actions.remove(row.id)">删除</el-button>
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
      <el-form :model="form.model" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.model.username" :disabled="form.mode === 'edit'" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.model.password"
            type="password"
            show-password
            :placeholder="form.mode === 'edit' ? '留空则不修改' : '请输入密码'"
          />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.model.nickname" />
        </el-form-item>
        <el-form-item label="部门">
          <el-tree-select
            v-model="form.model.deptId"
            :data="dept.tree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            placeholder="请选择部门"
            clearable
          />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.model.email" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.model.phone" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.model.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="form.close">取消</el-button>
        <el-button
          v-permission="[form.mode === 'edit' ? 'system:user:edit' : 'system:user:add']"
          type="primary"
          @click="form.submit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="role.dialogVisible" title="分配角色" width="500px">
      <el-checkbox-group v-model="role.selectedIds">
        <el-checkbox v-for="item in role.all" :key="item.id" :value="item.id" :label="item.name" />
      </el-checkbox-group>
      <template #footer>
        <el-button @click="role.dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="role.assign">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserCrud } from './hooks'

const { table, form, actions, dept, role, lifecycle } = useUserCrud()

onMounted(() => lifecycle.init())
</script>
