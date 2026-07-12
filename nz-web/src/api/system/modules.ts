import request from '@/api/request'
import type { BackendModuleStatus } from '@/core/modules/types'

export function getModuleStatuses() {
  return request.get<BackendModuleStatus[]>('/api/system/modules')
}
