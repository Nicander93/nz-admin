<template>
  <el-drawer v-model="visible" :title="`字典数据 - ${currentType}`" size="600px">
    <div class="mb-4">
      <el-button type="primary" @click="openAdd">新增数据</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border>
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
          <el-button link type="primary" @click="form.openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="form.visible" :title="form.title" width="500px" append-to-body>
      <el-form :model="form.form" label-width="100px">
        <el-form-item label="字典标签">
          <el-input v-model="form.form.label" />
        </el-form-item>
        <el-form-item label="字典值">
          <el-input v-model="form.form.value" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.form.status">
            <el-radio :value="0">正常</el-radio>
            <el-radio :value="1">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="form.close">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useForm } from '@/utils/CRUD'
import { listDictDataByType, addDictData, updateDictData, deleteDictData } from '@/api/system/dict'
import type { SysDictType, SysDictData } from '@/api/system/dict'
import { ElMessage } from 'element-plus'

const visible = ref(false)
const loading = ref(false)
const list = ref<SysDictData[]>([])
const currentType = ref('')

const form = useForm<SysDictData>({
  defaultForm: () => ({
    id: undefined as unknown as number,
    dictType: '', label: '', value: '', sort: 0, status: 0, remark: '',
    createTime: undefined as unknown as string,
  }),
  addApi: (data) => addDictData(data),
  updateApi: (data) => updateDictData(data),
})

async function refresh() {
  loading.value = true
  try {
    const res = await listDictDataByType(currentType.value)
    list.value = res.data
  } finally {
    loading.value = false
  }
}

function openAdd() {
  form.openAdd()
  form.form.dictType = currentType.value
}

async function submit() {
  const result = await form.submit()
  if (result.ok) {
    ElMessage.success(result.mode === 'add' ? '新增成功' : '更新成功')
    form.close()
    await refresh()
  }
}

async function remove(id: number) {
  await deleteDictData(id)
  ElMessage.success('删除成功')
  await refresh()
}

async function open(row: SysDictType) {
  currentType.value = row.type
  visible.value = true
  await refresh()
}

defineExpose({ open })
</script>
