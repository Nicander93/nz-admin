<template>
  <div>
    <div class="mb-4">
      <el-button
        v-permission="'system:menu:add'"
        type="primary"
        @click="form.openAdd()"
        >新增</el-button
      >
      <el-button @click="table.toggleExpand">{{
        table.isExpand ? '折叠' : '展开'
      }}</el-button>
    </div>

    <el-table
      :key="'menu-expand-' + table.isExpand"
      :data="table.treeData"
      v-loading="table.loading"
      border
      row-key="id"
      :default-expand-all="table.isExpand"
    >
      <el-table-column prop="name" label="菜单名称" width="200" />
      <el-table-column
        prop="path"
        label="路由路径"
        min-width="120"
        show-overflow-tooltip
      />
      <el-table-column
        prop="component"
        label="组件路径"
        min-width="160"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          <span v-if="row.type === 'C'">{{ row.component || '-' }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="icon" label="图标" width="80">
        <template #default="{ row }">
          <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column
        prop="perm"
        label="权限标识"
        min-width="160"
        show-overflow-tooltip
      >
        <template #default="{ row }">
          {{ row.perm || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="88">
        <template #default="{ row }">
          <el-tag v-if="row.type === 'M'" type="primary">目录</el-tag>
          <el-tag v-else-if="row.type === 'C'" type="success">菜单</el-tag>
          <el-tag v-else type="warning">按钮</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="visible" label="显示" width="72">
        <template #default="{ row }">
          <span v-if="row.type === 'F'">-</span>
          <template v-else>
            <el-tag :type="row.visible === 0 ? 'success' : 'info'" size="small">
              {{ row.visible === 0 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="72">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
            {{ row.status === 0 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'system:menu:add'"
            link
            type="primary"
            v-if="row.type !== 'F'"
            @click="form.openAdd(row.id)"
          >
            新增
          </el-button>
          <el-button
            v-permission="'system:menu:edit'"
            link
            type="primary"
            @click="form.openEdit(row)"
            >编辑</el-button
          >
          <el-button
            v-permission="'system:menu:remove'"
            link
            type="danger"
            @click="onDelete(row.id)"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="form.visible" :title="form.title" width="600px">
      <el-form :model="form.model" label-width="100px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.model.parentId"
            :data="table.menuSelectTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            placeholder="请选择上级菜单"
            clearable
          />
        </el-form-item>
        <el-form-item label="菜单类型">
          <el-radio-group v-model="form.model.type">
            <el-radio value="M">目录</el-radio>
            <el-radio value="C">菜单</el-radio>
            <el-radio value="F">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单名称">
          <el-input v-model="form.model.name" />
        </el-form-item>
        <el-form-item label="路由路径" v-if="form.model.type !== 'F'">
          <el-input
            v-model="form.model.path"
            placeholder="相对上级路由的路径，如 user 或 /system"
          />
        </el-form-item>
        <el-form-item label="组件路径" v-if="form.model.type === 'C'">
          <el-input
            v-model="form.model.component"
            placeholder="如 system/user/index，对应 views 下路径"
          />
        </el-form-item>
        <el-form-item label="权限标识" v-if="form.model.type !== 'M'">
          <el-input
            v-model="form.model.perm"
            placeholder="菜单填列表权限如 system:user:list；按钮填按钮权限"
          />
        </el-form-item>
        <el-form-item label="图标" v-if="form.model.type !== 'F'">
          <el-input
            v-model="form.model.icon"
            placeholder="Element Plus 图标组件名，如 User"
          />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.model.sort" :min="0" />
        </el-form-item>
        <el-form-item label="是否可见" v-if="form.model.type !== 'F'">
          <el-radio-group v-model="form.model.visible">
            <el-radio :value="0">显示</el-radio>
            <el-radio :value="1">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.model.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="form.close">取消</el-button>
        <el-button
          v-permission="[
            form.model.id ? 'system:menu:edit' : 'system:menu:add',
          ]"
          type="primary"
          @click="actions.submit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useMenuCrud } from './hooks'
import { ElMessageBox } from 'element-plus'

const { table, form, actions } = useMenuCrud()

async function onDelete(id: number) {
  await ElMessageBox.confirm('确认删除该菜单？', '提示', { type: 'warning' })
  actions.remove(id)
}

onMounted(() => table.loadData())
</script>
