<template>
  <div>
    <div class="mb-4">
      <el-button type="primary" @click="openAdd()">新增</el-button>
      <el-button @click="toggleExpand">{{ isExpand ? '折叠' : '展开' }}</el-button>
    </div>

    <el-table :data="treeData" v-loading="loading" border row-key="id" :default-expand-all="isExpand">
      <el-table-column prop="name" label="菜单名称" width="200" />
      <el-table-column prop="icon" label="图标" width="80">
        <template #default="{ row }">
          <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="60" />
      <el-table-column prop="perm" label="权限标识" />
      <el-table-column prop="type" label="类型" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.type === 'M'" type="primary">目录</el-tag>
          <el-tag v-else-if="row.type === 'C'" type="success">菜单</el-tag>
          <el-tag v-else type="warning">按钮</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="visible" label="可见" width="70">
        <template #default="{ row }">
          {{ row.visible === 0 ? '显示' : '隐藏' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" v-if="row.type !== 'F'" @click="openAdd(row.id)">新增</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" :title="title" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="menuSelectTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            placeholder="请选择上级菜单"
            clearable
          />
        </el-form-item>
        <el-form-item label="菜单类型">
          <el-radio-group v-model="form.type">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="路由路径" v-if="form.type !== 'F'">
          <el-input v-model="form.path" />
        </el-form-item>
        <el-form-item label="组件路径" v-if="form.type === 'C'">
          <el-input v-model="form.component" />
        </el-form-item>
        <el-form-item label="权限标识" v-if="form.type !== 'M'">
          <el-input v-model="form.perm" placeholder="如 system:user:list" />
        </el-form-item>
        <el-form-item label="图标" v-if="form.type !== 'F'">
          <el-input v-model="form.icon" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="是否可见" v-if="form.type !== 'F'">
          <el-radio-group v-model="form.visible">
            <el-radio :value="0">显示</el-radio>
            <el-radio :value="1">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="close">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useMenuCrud } from '@/hooks/useMenuCrud'
import { ElMessageBox } from 'element-plus'

const {
  loading, treeData, isExpand, menuSelectTree,
  form, visible, title, close,
  openAdd, openEdit, toggleExpand, handleSubmit, handleDelete, loadData,
} = useMenuCrud()

async function onDelete(id: number) {
  await ElMessageBox.confirm('确认删除该菜单？', '提示', { type: 'warning' })
  handleDelete(id)
}

onMounted(() => loadData())
</script>
