<template>
  <div>
    <el-form :inline="true" :model="table.query" class="mb-4">
      <el-form-item label="用户名">
        <el-input
          v-model="table.query.username"
          placeholder="请输入用户名"
          clearable
        />
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
      <el-button
        v-permission="'system:user:add'"
        type="primary"
        @click="form.openAdd"
        >新增</el-button
      >
      <el-button
        v-permission="'system:user:contact:plain'"
        plain
        @click="contact.toggle"
      >
        {{ contact.revealed ? '隐藏明文' : '查看明文联系方式' }}
      </el-button>
      <el-button
        v-permission="'system:user:contact:encrypt'"
        type="warning"
        plain
        @click="handleReEncrypt"
      >
        重加密联系方式
      </el-button>
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
      <el-table-column label="邮箱">
        <template #default="{ row }">
          {{ row.email || row.emailMasked || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="手机号">
        <template #default="{ row }">
          {{ row.phone || row.phoneMasked || '-' }}
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
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'system:user:edit'"
            link
            type="primary"
            @click="form.openEdit(row)"
            >编辑</el-button
          >
          <el-button
            v-permission="'system:user:resetPwd'"
            link
            type="warning"
            @click="handleResetPwd(row)"
            >重置密码</el-button
          >
          <el-button
            v-permission="'system:user:edit'"
            link
            type="primary"
            @click="role.openDialog(row)"
            >分配角色</el-button
          >
          <el-button
            v-permission="'system:user:remove'"
            link
            type="danger"
            @click="actions.remove(row.id)"
            >删除</el-button
          >
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
          <el-input
            v-model="form.model.username"
            :disabled="form.mode === 'edit'"
          />
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
        <el-form-item label="岗位">
          <el-select
            v-model="form.model.postIds"
            multiple
            placeholder="请选择岗位"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="p in posts"
              :key="p.id"
              :label="p.postName"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input
            v-model="form.model.email"
            :disabled="form.mode === 'edit' && !contact.canReveal"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="form.model.phone"
            :disabled="form.mode === 'edit' && !contact.canReveal"
          />
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
          v-permission="[
            form.mode === 'edit' ? 'system:user:edit' : 'system:user:add',
          ]"
          type="primary"
          @click="actions.submit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="role.dialogVisible" title="分配角色" width="500px">
      <el-checkbox-group v-model="role.selectedIds">
        <el-checkbox
          v-for="item in role.all"
          :key="item.id"
          :value="item.id"
          :label="item.name"
        />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { userApi, type SysUser } from '@/api/system'
import { useUserCrud } from './hooks'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const { table, form, actions, dept, role, posts, contact } = useUserCrud({
  canRevealContacts: () => userStore.hasPermission('system:user:contact:plain'),
})

async function handleResetPwd(row: SysUser) {
  await ElMessageBox.confirm(
    '确定将密码重置为参数配置中的默认密码（sys.user.initPassword）吗？',
    '重置密码',
  )
  await userApi.resetUserPassword(row.id)
  ElMessage.success('已重置为默认密码')
}

async function handleReEncrypt() {
  await ElMessageBox.confirm(
    '将使用当前活动密钥重写本租户全部用户联系方式，是否继续？',
    '重加密联系方式',
  )
  const res = await userApi.reEncryptUserContacts()
  ElMessage.success(`已重加密 ${res.data} 个用户的联系方式`)
}
onMounted(() => table.loadData())
</script>
