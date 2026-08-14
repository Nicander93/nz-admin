import request from '@/api/request'

export interface UserProfile {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  gender: '0' | '1' | '2'
  avatarFileId?: number
  roleGroup: string
  postGroup: string
  createTime?: string
}

export interface ProfileUpdateRequest {
  nickname: string
  email?: string
  phone?: string
  gender: '0' | '1' | '2'
}

export function getProfile() {
  return request.get<UserProfile>('/api/system/profile')
}

export function updateProfile(data: ProfileUpdateRequest) {
  return request.put<void>('/api/system/profile', data)
}

export function updateProfilePassword(data: { oldPassword: string; newPassword: string }) {
  return request.put<void>('/api/system/profile/password', data)
}

export function uploadProfileAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<number>('/api/system/profile/avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getProfileAvatar() {
  return request.getBlob('/api/system/profile/avatar')
}
