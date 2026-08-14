import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export interface WorkflowDefinition {
  definitionId: number
  flowCode: string
  flowName: string
  categoryId: number
  categoryName?: string
  versionNo: number
  publishStatus: number
  activityStatus: number
  formPath?: string
  modelJson: string
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface WorkflowDefinitionForm {
  definitionId?: number
  flowCode: string
  flowName: string
  categoryId?: number
  formPath?: string
  modelJson: string
  remark?: string
}

export interface WorkflowDefinitionQuery extends PageQuery {
  flowCode?: string
  flowName?: string
  categoryId?: number
  publishStatus?: number
}

export interface WorkflowDefinitionCopyForm {
  sourceDefinitionId: number
  flowCode: string
  flowName: string
}

export function pageWorkflowDefinitions(params: WorkflowDefinitionQuery) {
  return request.get<PageResult<WorkflowDefinition>>('/api/workflow/definition/page', { params })
}

export function getWorkflowDefinition(definitionId: number) {
  return request.get<WorkflowDefinition>(`/api/workflow/definition/${definitionId}`)
}

export function createWorkflowDefinition(data: WorkflowDefinitionForm) {
  return request.post<number>('/api/workflow/definition', data)
}

export function updateWorkflowDefinition(data: WorkflowDefinitionForm) {
  return request.put<void>('/api/workflow/definition', data)
}

export function publishWorkflowDefinition(definitionId: number) {
  return request.post<void>(`/api/workflow/definition/${definitionId}/publish`)
}

export function unpublishWorkflowDefinition(definitionId: number) {
  return request.post<void>(`/api/workflow/definition/${definitionId}/unpublish`)
}

export function setWorkflowDefinitionActive(definitionId: number, active: boolean) {
  return request.put<void>(`/api/workflow/definition/${definitionId}/active`, undefined, { params: { active } })
}

export function copyWorkflowDefinition(data: WorkflowDefinitionCopyForm) {
  return request.post<number>('/api/workflow/definition/copy', data)
}

export function importWorkflowDefinition(file: Blob, categoryId?: number) {
  const form = new FormData()
  form.append('file', file)
  return request.post<number>('/api/workflow/definition/import', form, { params: { categoryId } })
}

export function exportWorkflowDefinition(definitionId: number) {
  return request.getBlob(`/api/workflow/definition/${definitionId}/export`)
}

export function deleteWorkflowDefinition(definitionId: number) {
  return request.delete<void>(`/api/workflow/definition/${definitionId}`)
}
