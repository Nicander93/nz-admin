import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useProfile, validateAvatarFile } from '@/views/profile/hooks'

const mocks = vi.hoisted(() => ({
  getProfile: vi.fn(),
  getProfileAvatar: vi.fn(),
  updateProfile: vi.fn(),
  updateProfilePassword: vi.fn(),
  uploadProfileAvatar: vi.fn(),
  fetchUserInfo: vi.fn(),
  logout: vi.fn(),
  replace: vi.fn(),
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
  useUserStore: () => ({
    fetchUserInfo: mocks.fetchUserInfo,
    logout: mocks.logout,
  }),
}))
vi.mock('@/router', () => ({ default: { replace: mocks.replace } }))
vi.mock('element-plus', () => ({
  ElMessage: { success: mocks.success, warning: mocks.warning },
}))

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
    mocks.updateProfilePassword.mockResolvedValue({ data: undefined })
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
    expect(mocks.logout).not.toHaveBeenCalled()
    expect(mocks.replace).not.toHaveBeenCalled()
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


describe('profile security changes', () => {
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
    mocks.updateProfilePassword.mockResolvedValue({ data: undefined })
  })

  it('logs out after a successful phone change without fetching info with the revoked token', async () => {
    const { actions, form } = useProfile()
    await actions.load()
    form.phone = ''

    await actions.saveProfile()

    expect(mocks.logout).toHaveBeenCalledOnce()
    expect(mocks.replace).toHaveBeenCalledWith('/login')
    expect(mocks.fetchUserInfo).not.toHaveBeenCalled()
    expect(mocks.success).toHaveBeenCalledWith('手机号已修改，请重新登录')
  })

  it('keeps the session and entered data when a phone change fails', async () => {
    mocks.updateProfile.mockRejectedValue(new Error('保存失败'))
    const { actions, form } = useProfile()
    await actions.load()
    form.phone = ''

    await expect(actions.saveProfile()).rejects.toThrow('保存失败')

    expect(form.phone).toBe('')
    expect(mocks.logout).not.toHaveBeenCalled()
    expect(mocks.replace).not.toHaveBeenCalled()
  })

  it('clears password fields and logs out after a successful password change', async () => {
    const { actions, passwordForm } = useProfile()
    Object.assign(passwordForm, {
      oldPassword: 'old-password',
      newPassword: 'new-password',
      confirmPassword: 'new-password',
    })

    await actions.changePassword()

    expect(passwordForm).toEqual({ oldPassword: '', newPassword: '', confirmPassword: '' })
    expect(mocks.logout).toHaveBeenCalledOnce()
    expect(mocks.replace).toHaveBeenCalledWith('/login')
    expect(mocks.success).toHaveBeenCalledWith('密码修改成功，请重新登录')
  })

  it('retains password fields and session when the password change fails', async () => {
    mocks.updateProfilePassword.mockRejectedValue(new Error('旧密码错误'))
    const { actions, passwordForm } = useProfile()
    Object.assign(passwordForm, {
      oldPassword: 'wrong-password',
      newPassword: 'new-password',
      confirmPassword: 'new-password',
    })

    await expect(actions.changePassword()).rejects.toThrow('旧密码错误')

    expect(passwordForm.oldPassword).toBe('wrong-password')
    expect(mocks.logout).not.toHaveBeenCalled()
    expect(mocks.replace).not.toHaveBeenCalled()
  })
})
