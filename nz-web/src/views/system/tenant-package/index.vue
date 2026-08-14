<template>
  <div>
    <el-form :inline="true" :model="table.query" class="mb-4">
      <el-form-item label="套餐名称">
        <el-input
          v-model="table.query.packageName"
          clearable
          placeholder="请输入套餐名称"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="table.query.status" clearable placeholder="全部">
          <el-option label="正常" :value="0" />
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
        v-permission="'system:tenantpackage:add'"
        type="primary"
        @click="openAdd"
        >新增套餐</el-button
      >
    </div>

    <el-table :data="table.data" v-loading="table.loading" border>
      <el-table-column prop="packageName" label="套餐名称" min-width="180" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 0 ? 'success' : 'danger'">
            {{ row.status === 0 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'system:tenantpackage:edit'"
            link
            type="primary"
            @click="openEdit(row.id)"
            >编辑</el-button
          >
          <el-button
            v-permission="'system:tenantpackage:remove'"
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
      v-model="form.visible"
      :title="form.title + '租户套餐'"
      width="620px"
    >
      <el-form :model="form.model" label-width="100px">
        <el-form-item label="套餐名称">
          <el-input v-model="form.model.packageName" />
        </el-form-item>
        <el-form-item label="菜单权限">
          <el-tree
            ref="treeRef"
            :data="menuTree"
            :props="{ label: 'name', children: 'children' }"
            show-checkbox
            node-key="id"
            default-expand-all
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.model.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.model.remark" type="textarea" :rows="3" />
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
import { nextTick, onMounted, ref } from 'vue'
import { ElMessageBox, type ElTree } from 'element-plus'
import { getTenantPackage } from '@/api/system/tenant'
import { listMenus, type SysMenu } from '@/api/system/menu'
import { buildTree } from '@/utils/tree'
import { useTenantPackageCrud } from './hooks'

const { table, form, actions } = useTenantPackageCrud()
const menuTree = ref<SysMenu[]>([])
const flatMenus = ref<SysMenu[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()

async function loadMenus() {
  const result = await listMenus()
  flatMenus.value = result.data || []
  menuTree.value = buildTree(flatMenus.value)
}

async function openAdd() {
  form.openAdd()
  await nextTick()
  treeRef.value?.setCheckedKeys([])
}

async function openEdit(id: number) {
  const result = await getTenantPackage(id)
  form.openEdit(result.data)
  await nextTick()
  const parentIds = new Set(flatMenus.value.map((menu) => menu.parentId))
  const leafIds = (result.data.menuIds || []).filter(
    (menuId) => !parentIds.has(menuId),
  )
  treeRef.value?.setCheckedKeys(leafIds)
}

async function submit() {
  form.model.menuIds = [
    ...((treeRef.value?.getCheckedKeys() as number[]) || []),
    ...((treeRef.value?.getHalfCheckedKeys() as number[]) || []),
  ]
  const result = await actions.submit()
  if (result.ok) {
    form.close()
    await table.refresh()
  }
}

async function remove(id: number) {
  await ElMessageBox.confirm('确认删除该租户套餐？', '提示', {
    type: 'warning',
  })
  await actions.remove(id)
}

onMounted(async () => {
  await Promise.all([loadMenus(), table.refresh()])
})
</script>
