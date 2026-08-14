<template>
  <div v-loading="loading" class="profile-page">
    <section class="profile-card profile-card--identity">
      <el-upload
        :show-file-list="false"
        :http-request="handleAvatarUpload"
        accept="image/png,image/jpeg,image/webp,image/gif"
      >
        <button type="button" class="profile-avatar" aria-label="更换头像">
          <el-avatar :size="104" :src="avatarUrl">
            <el-icon :size="42"><UserFilled /></el-icon>
          </el-avatar>
          <span>更换头像</span>
        </button>
      </el-upload>
      <h2>{{ profile?.nickname || profile?.username }}</h2>
      <p>@{{ profile?.username }}</p>
      <dl>
        <div><dt>角色</dt><dd>{{ profile?.roleGroup }}</dd></div>
        <div><dt>岗位</dt><dd>{{ profile?.postGroup }}</dd></div>
        <div><dt>加入时间</dt><dd>{{ profile?.createTime || '-' }}</dd></div>
      </dl>
    </section>

    <section class="profile-content">
      <el-card shadow="never">
        <template #header><strong>基本资料</strong></template>
        <el-form :model="form" label-position="top">
          <div class="profile-form-grid">
            <el-form-item label="用户名"><el-input :model-value="profile?.username" disabled /></el-form-item>
            <el-form-item label="昵称"><el-input v-model="form.nickname" maxlength="50" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="form.email" maxlength="320" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="form.phone" maxlength="20" /></el-form-item>
          </div>
          <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
              <el-radio value="0">男</el-radio>
              <el-radio value="1">女</el-radio>
              <el-radio value="2">未设置</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-button type="primary" :loading="saving" @click="actions.saveProfile">保存资料</el-button>
        </el-form>
      </el-card>

      <el-card shadow="never">
        <template #header><strong>修改密码</strong></template>
        <el-form :model="passwordForm" label-position="top">
          <el-form-item label="当前密码">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
          </el-form-item>
          <div class="profile-form-grid">
            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
          </div>
          <el-button type="primary" @click="actions.changePassword">修改密码</el-button>
        </el-form>
      </el-card>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue'
import { UserFilled } from '@element-plus/icons-vue'
import type { UploadRequestOptions } from 'element-plus'
import { useProfile } from './hooks'

const {
  loading, saving, profile, avatarUrl, form, passwordForm, actions,
} = useProfile()

function handleAvatarUpload(options: UploadRequestOptions) {
  return actions.uploadAvatar(options.file)
}

onMounted(actions.load)
onBeforeUnmount(actions.releaseAvatarUrl)
</script>

<style scoped>
.profile-page { display: grid; grid-template-columns: 300px minmax(0, 1fr); gap: 20px; align-items: start; }
.profile-card { border: 1px solid var(--el-border-color-light); border-radius: 10px; background: var(--el-bg-color); }
.profile-card--identity { padding: 30px 24px; text-align: center; }
.profile-avatar { display: inline-flex; flex-direction: column; align-items: center; gap: 8px; padding: 0; border: 0; color: var(--el-color-primary); background: transparent; cursor: pointer; }
.profile-card h2 { margin: 18px 0 4px; font-size: 22px; }
.profile-card > p { margin: 0 0 24px; color: var(--el-text-color-secondary); }
.profile-card dl { margin: 0; text-align: left; }
.profile-card dl div { display: grid; grid-template-columns: 70px 1fr; gap: 12px; padding: 12px 0; border-top: 1px solid var(--el-border-color-lighter); }
.profile-card dt { color: var(--el-text-color-secondary); }
.profile-card dd { margin: 0; word-break: break-all; }
.profile-content { display: grid; gap: 20px; }
.profile-form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; }
@media (max-width: 900px) {
  .profile-page { grid-template-columns: 1fr; }
  .profile-form-grid { grid-template-columns: 1fr; }
}
</style>
