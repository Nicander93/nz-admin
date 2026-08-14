import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  getProfile: vi.fn(),
  getProfileAvatar: vi.fn(),
  updateProfile: vi.fn(),
  updateProfilePassword: vi.fn(),
  uploadProfileAvatar: vi.fn(),
  fetchUserInfo: vi.fn(),
  success: vi.fn(),
  warning: vi.fn(),
}))

vi.mock('@/api/system/profile', () => ({
  getProfile: mocks.getProfile,
  getProfileAvatar: mocks.getProfileAvatar,
  updateProfile: mocks.updateProfile,
  updateProfilePassword: mocks.updateProfilePassword,
  uploadProfileAvatar: mocks.uploadProfileAvatar,
}))
vi.mock('@/stores/user', () => ({
  useUserStore: () => ({ fetchUserInfo: mocks.fetchUserInfo }),
}))
vi.mock('element-plus', () => ({
  ElMessage: { success: mocks.success, warning: mocks.warning },
}))

import { useProfile, validateAvatarFile } from '@/views/profile/hooks'

describe('profile hooks', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.getProfile.mockResolvedValue({
      data: {
        id: 7,
        username: 'alice',
        nickname: 'Alice',
        email: 'alice@example.com',
        phone: '13800138000',
        gender: '2',
        roleGroup: '运维',
        postGroup: '工程师',
      },
    })
    mocks.updateProfile.mockResolvedValue({ data: undefined })
    mocks.fetchUserInfo.mockResolvedValue(undefined)
  })

  it('validates avatar type and size', () => {
    expect(validateAvatarFile(new File(['x'], 'avatar.txt', { type: 'text/plain' })))
      .toBe('头像必须是图片文件')
    expect(validateAvatarFile(new File(['x'], 'avatar.png', { type: 'image/png' })))
      .toBeNull()
  })

  it('loads and saves the current users own profile', async () => {
    const { actions, form, profile } = useProfile()

    await actions.load()
    form.nickname = 'Alice Chen'
    await actions.saveProfile()

    expect(profile.value?.roleGroup).toBe('运维')
    expect(mocks.updateProfile).toHaveBeenCalledWith({
      nickname: 'Alice Chen',
      email: 'alice@example.com',
      phone: '13800138000',
      gender: '2',
    })
    expect(mocks.fetchUserInfo).toHaveBeenCalled()
    expect(mocks.success).toHaveBeenCalledWith('个人资料已保存')
  })

  it('requires matching password confirmation', async () => {
    const { actions, passwordForm } = useProfile()
    passwordForm.oldPassword = 'old-password'
    passwordForm.newPassword = 'new-password'
    passwordForm.confirmPassword = 'different'

    await actions.changePassword()

    expect(mocks.updateProfilePassword).not.toHaveBeenCalled()
    expect(mocks.warning).toHaveBeenCalledWith('两次输入的新密码不一致')
  })
})
