import request from '@/api/request'

export interface EngineSnapshot {
  instance: {
    id: string
    flowName: string
    businessId: string
    flowStatus: string
  }
  tasks: { id: string; nodeName: string }[]
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
