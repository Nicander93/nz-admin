<template>
  <el-dialog v-model="visible" title="分配菜单权限" width="500px">
    <el-tree
      ref="treeRef"
      :data="menuTree"
      :props="{ label: 'name', children: 'children' }"
      show-checkbox
      node-key="id"
      default-expand-all
    />
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="submit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { listMenus } from '@/api/system/menu'
import type { SysMenu } from '@/api/system/menu'
import { getRoleMenuIds, assignRoleMenus } from '@/api/system/role'
import type { SysRole } from '@/api/system/role'
import { buildTree } from '@/utils/tree'
import type { ElTree } from 'element-plus'

const visible = ref(false)
const menuTree = ref<SysMenu[]>([])
const currentRoleId = ref<number>()
const treeRef = ref<InstanceType<typeof ElTree>>()

async function open(row: SysRole) {
  currentRoleId.value = row.id

  const [menusRes, idsRes] = await Promise.all([listMenus(), getRoleMenuIds(row.id)])
  menuTree.value = buildTree(menusRes.data)
  visible.value = true
  await nextTick()

  // 只回填叶子节点，避免父节点全选和半选状态冲突。
  const parentIds = new Set(menusRes.data.map(m => m.parentId))
  const leafIds = idsRes.data.filter(id => !parentIds.has(id))
  treeRef.value?.setCheckedKeys(leafIds)
}

async function submit() {
  const allKeys = [
    ...treeRef.value!.getCheckedKeys() as number[],
    ...treeRef.value!.getHalfCheckedKeys() as number[],
  ]
  await assignRoleMenus(currentRoleId.value!, allKeys)
  visible.value = false
}

defineExpose({ open })
</script>
