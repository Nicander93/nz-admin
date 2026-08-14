import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'
import type { WorkflowInstanceActionForm } from '@/api/workflow/instance'

export type WorkflowTaskTab = 'todo' | 'done' | 'copy'
export type WorkflowTaskAction =
  'APPROVE' | 'REJECT' | 'TRANSFER' | 'DELEGATE' | 'RESOLVE' | 'CANCEL' | 'TERMINATE'

export interface WorkflowTask {
  taskId?: number
  historyId?: number
  copyId?: number
  instanceId: number
  definitionId?: number
  businessKey?: string
  title?: string
  flowCode?: string
  flowName?: string
  versionNo?: number
  nodeId?: string
  nodeName?: string
  assignee?: string
  ownerAssignee?: string
  delegationStatus?: number
  operatorId?: number
  operatorName?: string
  action?: WorkflowTaskAction
  targetNodeName?: string
  targetAssignee?: string
  comment?: string
  readStatus?: number
  createTime?: string
  updateTime?: string
}

export interface WorkflowTaskQuery extends PageQuery {
  nodeName?: string
  action?: WorkflowTaskAction
  readStatus?: number
}

export interface WorkflowTaskTransferForm {
  targetUserId: number
  comment?: string
}

export interface WorkflowTaskDelegateForm {
  targetUserId: number
  comment?: string
}

export interface WorkflowTaskCopyForm {
  receiverIds: number[]
  comment?: string
}

export function pageWorkflowTodoTasks(params: WorkflowTaskQuery) {
  return request.get<PageResult<WorkflowTask>>('/api/workflow/task/todo/page', { params })
}

export function pageWorkflowDoneTasks(params: WorkflowTaskQuery) {
  return request.get<PageResult<WorkflowTask>>('/api/workflow/task/done/page', { params })
}

export function pageWorkflowCopyTasks(params: WorkflowTaskQuery) {
  return request.get<PageResult<WorkflowTask>>('/api/workflow/task/copy/page', { params })
}

export function getWorkflowTask(taskId: number) {
  return request.get<WorkflowTask>(`/api/workflow/task/${taskId}`)
}

export function actionWorkflowTask(taskId: number, data: WorkflowInstanceActionForm) {
  return request.post<void>(`/api/workflow/task/${taskId}/action`, data)
}

export function transferWorkflowTask(taskId: number, data: WorkflowTaskTransferForm) {
  return request.post<void>(`/api/workflow/task/${taskId}/transfer`, data)
}

export function delegateWorkflowTask(taskId: number, data: WorkflowTaskDelegateForm) {
  return request.post<void>(`/api/workflow/task/${taskId}/delegate`, data)
}

export function resolveWorkflowTask(taskId: number, comment?: string) {
  return request.post<void>(`/api/workflow/task/${taskId}/resolve`, { comment })
}

export function copyWorkflowTask(taskId: number, data: WorkflowTaskCopyForm) {
  return request.post<void>(`/api/workflow/task/${taskId}/copy`, data)
}

export function markWorkflowTaskCopyRead(copyId: number) {
  return request.put<void>(`/api/workflow/task/copy/${copyId}/read`)
}
