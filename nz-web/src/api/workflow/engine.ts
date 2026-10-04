import request from '@/api/request'

export interface EngineSnapshot {
  business: Record<string, unknown>
  sync: { pending: boolean; failed: boolean }
  instance: {
    id: string
    flowName: string
    businessId: string
    flowStatus: string
    creator: boolean
    active: boolean
  }
  tasks: { id: string; nodeName: string; actionable: boolean }[]
  history: {
    nodeName: string
    approver: string
    message: string
    skipType: string
  }[]
}
export const engineCapabilities = () =>
  request.get<{ enabled: boolean }>('/api/workflow/engine/capabilities')
export const importEngineDefinition = (definition: unknown, key: string) =>
  request.post<string>('/api/workflow/engine/definitions', definition, {
    headers: { 'Idempotency-Key': key },
  })
export const publishEngineDefinition = (id: string) =>
  request.post(`/api/workflow/engine/definitions/${id}/publish`)
export const getEngineInstance = (id: string) =>
  request.get<EngineSnapshot>(`/api/workflow/engine/instances/${id}`)
export const startEngineInstance = (body: unknown, key: string) =>
  request.post<string>('/api/workflow/engine/instances', body, {
    headers: { 'Idempotency-Key': key },
  })
export const actionEngineTask = (id: string, body: unknown, key: string) =>
  request.post<string>(`/api/workflow/engine/tasks/${id}/action`, body, {
    headers: { 'Idempotency-Key': key },
  })

export const getEngineDefinition = (id: string) =>
  request.get<unknown>(`/api/workflow/engine/definitions/${id}`)

export interface EngineDefinition {
  businessType: string
  id: string
  flowCode: string
  flowName: string
  version: string
  isPublish: number
}
export interface EngineInstance {
  id: string
  flowName: string
  businessId: string
  flowStatus: string
  createTime: string
}
export const listEngineDefinitions = () =>
  request.get<EngineDefinition[]>('/api/workflow/designer/definitions')
export const createEngineDraft = (
  body: {
    flowCode: string
    flowName: string
    sourceId?: string
    businessType?: string
  },
  key: string,
) =>
  request.post<string>('/api/workflow/designer/definitions', body, {
    headers: { 'Idempotency-Key': key },
  })
export const listEngineCenter = (category: string, page: number) =>
  request.get<EngineInstance[]>(`/api/workflow/engine/center/${category}`, {
    params: { page, size: 20 },
  })
export const publishedEngineDefinitions = () =>
  request.get<{ flowCode: string; flowName: string }[]>(
    '/api/workflow/engine/published-definitions',
  )

export const manageEngineTask = (id: string, body: unknown, key: string) =>
  request.post<string>(`/api/workflow/engine/tasks/${id}/manage`, body, {
    headers: { 'Idempotency-Key': key },
  })
export const revokeEngineInstance = (
  id: string,
  comment: string,
  key: string,
) =>
  request.post<string>(
    `/api/workflow/engine/instances/${id}/revoke`,
    { comment },
    { headers: { 'Idempotency-Key': key } },
  )
export const engineReturnNodes = (id: string) =>
  request.get<{ nodeCode: string; nodeName: string }[]>(
    `/api/workflow/engine/tasks/${id}/return-nodes`,
  )
export const engineParticipants = (name = '') =>
  request.get<{
    list: { storageId: string; handlerName: string; handlerCode: string }[]
    total: number
  }>('/api/workflow/engine/participants', { params: { name } })

export const listEngineBusinessTypes = () =>
  request.get<{ code: string; name: string }[]>(
    '/api/workflow/designer/business-types',
  )
