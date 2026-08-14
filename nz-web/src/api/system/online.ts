import request from '@/api/request'

export type OnlineUser = {
  tokenValue: string
  userId: number
  tenantId?: number
  tenantCode?: string
  username?: string
  deptName?: string
  loginIp?: string
  loginTime?: string
  userAgent?: string
  tokenTimeout: number
}

export type OnlineUserQuery = {
  username?: string
  loginIp?: string
}

export function listOnlineUsers(params?: OnlineUserQuery) {
  return request.get<OnlineUser[]>('/api/system/online', { params })
}

export function forceLogout(tokenValue: string) {
  return request.delete<void>(`/api/system/online/${encodeURIComponent(tokenValue)}`)
}
