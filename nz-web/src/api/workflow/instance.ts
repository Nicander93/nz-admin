import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export type WorkflowInstanceStatus =
  | 'RUNNING'
  | 'COMPLETED'
  | 'REJECTED'
  | 'CANCELED'
  | 'TERMINATED'

export interface WorkflowInstanceEvent {
  eventId: number
  eventType: string
  fromNodeId?: string
  fromNodeName?: string
  toNodeId?: string
  toNodeName?: string
  operatorId: number
  operatorName?: string
  comment?: string
  createTime?: string
}

export interface WorkflowInstance {
  instanceId: number
  definitionId: number
  businessKey: string
  title: string
  flowCode: string
  flowName: string
  versionNo: number
  initiatorId: number
  currentNodeId: string
  currentNodeName: string
  currentNodeType: string
  currentAssignee?: string
  status: WorkflowInstanceStatus
  activityStatus: number
  variablesJson: string
  createTime?: string
  updateTime?: string
  endTime?: string
  events?: WorkflowInstanceEvent[]
}

export interface WorkflowInstanceQuery extends PageQuery {
  flowCode?: string
  title?: string
  businessKey?: string
  status?: WorkflowInstanceStatus
  mine?: boolean
}

export interface WorkflowInstanceStartForm {
  flowCode: string
  businessKey: string
  title: string
  variables: Record<string, unknown>
}

export interface WorkflowInstanceActionForm {
  action: 'APPROVE' | 'REJECT'
  comment?: string
}

export function pageWorkflowInstances(params: WorkflowInstanceQuery) {
  return request.get<PageResult<WorkflowInstance>>('/api/workflow/instance/page', { params })
}

export function getWorkflowInstance(instanceId: number) {
  return request.get<WorkflowInstance>(`/api/workflow/instance/${instanceId}`)
}

export function startWorkflowInstance(data: WorkflowInstanceStartForm) {
  return request.post<number>('/api/workflow/instance/start', data)
}

export function actionWorkflowInstance(instanceId: number, data: WorkflowInstanceActionForm) {
  return request.post<void>(`/api/workflow/instance/${instanceId}/action`, data)
}

export function cancelWorkflowInstance(instanceId: number, comment?: string) {
  return request.post<void>(`/api/workflow/instance/${instanceId}/cancel`, { comment })
}

export function terminateWorkflowInstance(instanceId: number, comment?: string) {
  return request.post<void>(`/api/workflow/instance/${instanceId}/terminate`, { comment })
}

export function urgeWorkflowInstance(instanceId: number, content: string) {
  return request.post<void>(`/api/workflow/instance/${instanceId}/urge`, { content })
}

export function setWorkflowInstanceActive(instanceId: number, active: boolean) {
  return request.put<void>(`/api/workflow/instance/${instanceId}/active`, undefined, { params: { active } })
}

export function deleteWorkflowInstance(instanceId: number) {
  return request.delete<void>(`/api/workflow/instance/${instanceId}`)
}
