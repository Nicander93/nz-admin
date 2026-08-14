import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getProfile,
  getProfileAvatar,
  updateProfile,
  updateProfilePassword,
  uploadProfileAvatar,
  type ProfileUpdateRequest,
  type UserProfile,
} from '@/api/system/profile'
import { useUserStore } from '@/stores/user'

export function validateAvatarFile(file: File): string | null {
  if (!file.type.startsWith('image/')) return '头像必须是图片文件'
  if (file.size > 5 * 1024 * 1024) return '头像不能超过 5 MB'
  return null
}

/** 当前用户资料、密码和头像操作。 */
export function useProfile() {
  const userStore = useUserStore()
  const loading = ref(false)
  const saving = ref(false)
  const profile = ref<UserProfile>()
  const avatarUrl = ref('')
  const form = reactive<ProfileUpdateRequest>({
    nickname: '',
    email: '',
    phone: '',
    gender: '2',
  })
  const passwordForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  })

  async function load() {
    loading.value = true
    try {
      const response = await getProfile()
      profile.value = response.data
      Object.assign(form, {
        nickname: response.data.nickname,
        email: response.data.email ?? '',
        phone: response.data.phone ?? '',
        gender: response.data.gender ?? '2',
      })
      await loadAvatar()
    } finally {
      loading.value = false
    }
  }

  async function loadAvatar() {
    releaseAvatarUrl()
    if (!profile.value?.avatarFileId) return
    const response = await getProfileAvatar()
    avatarUrl.value = URL.createObjectURL(response.data)
  }

  async function saveProfile() {
    if (!form.nickname.trim()) {
      ElMessage.warning('请输入昵称')
      return
    }
    saving.value = true
    try {
      await updateProfile({ ...form, nickname: form.nickname.trim() })
      await userStore.fetchUserInfo()
      if (profile.value) profile.value.nickname = form.nickname.trim()
      ElMessage.success('个人资料已保存')
    } finally {
      saving.value = false
    }
  }

  async function changePassword() {
    if (!passwordForm.oldPassword || passwordForm.newPassword.length < 6) {
      ElMessage.warning('请填写旧密码，新密码至少 6 个字符')
      return
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      ElMessage.warning('两次输入的新密码不一致')
      return
    }
    await updateProfilePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    })
    Object.assign(passwordForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    ElMessage.success('密码修改成功')
  }

  async function uploadAvatar(file: File) {
    const error = validateAvatarFile(file)
    if (error) {
      ElMessage.warning(error)
      return
    }
    const response = await uploadProfileAvatar(file)
    if (profile.value) profile.value.avatarFileId = response.data
    await userStore.fetchUserInfo()
    await loadAvatar()
    ElMessage.success('头像已更新')
  }

  function releaseAvatarUrl() {
    if (avatarUrl.value) {
      URL.revokeObjectURL(avatarUrl.value)
      avatarUrl.value = ''
    }
  }

  return {
    loading,
    saving,
    profile,
    avatarUrl,
    form,
    passwordForm,
    actions: { load, saveProfile, changePassword, uploadAvatar, releaseAvatarUrl },
  }
}
