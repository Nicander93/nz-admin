import request from '@/api/request'

export type SocialProvider = {
  code: string
  displayName: string
}

export type SocialAuthorization = {
  authorizeUrl: string
  state: string
  expiresAt: string
}

export type SocialBinding = {
  id: number
  provider: string
  providerName: string
  username?: string
  nickname?: string
  email?: string
  avatar?: string
  bindTime?: string
}

export type SocialCallbackResult = {
  purpose: 'LOGIN' | 'BIND'
  token?: string
  binding?: SocialBinding
}

export function getSocialProviders() {
  return request.get<SocialProvider[]>('/api/auth/social/providers')
}

export function authorizeSocialLogin(data: {
  tenantCode: string
  clientId: string
  provider: string
}) {
  return request.post<SocialAuthorization>('/api/auth/social/authorize', data)
}

export function completeSocialCallback(data: {
  provider: string
  code: string
  state: string
}) {
  return request.post<SocialCallbackResult>('/api/auth/social/callback', data)
}

export function listSocialBindings() {
  return request.get<SocialBinding[]>('/api/system/social/list')
}

export function authorizeSocialBinding(provider: string) {
  return request.post<SocialAuthorization>(
    '/api/system/social/authorize/' + encodeURIComponent(provider),
  )
}

export function unbindSocialAccount(id: number) {
  return request.delete<void>('/api/system/social/' + id)
}
