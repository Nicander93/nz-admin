import request from '@/api/request'

export interface WorkflowCategory {
  categoryId: number
  parentId: number
  ancestors: string
  categoryName: string
  orderNum: number
  builtIn: number
  createTime?: string
  children?: WorkflowCategory[]
}

export interface WorkflowCategoryForm {
  categoryId?: number
  parentId: number
  categoryName: string
  orderNum: number
}

export interface WorkflowCategoryQuery {
  categoryName?: string
  parentId?: number
}

export function listWorkflowCategories(params?: WorkflowCategoryQuery) {
  return request.get<WorkflowCategory[]>('/api/workflow/category/list', { params })
}

export function getWorkflowCategoryTree(params?: Pick<WorkflowCategoryQuery, 'categoryName'>) {
  return request.get<WorkflowCategory[]>('/api/workflow/category/tree', { params })
}

export function getWorkflowCategory(categoryId: number) {
  return request.get<WorkflowCategory>(`/api/workflow/category/${categoryId}`)
}

export function createWorkflowCategory(data: WorkflowCategoryForm) {
  return request.post<number>('/api/workflow/category', data)
}

export function updateWorkflowCategory(data: WorkflowCategoryForm) {
  return request.put<void>('/api/workflow/category', data)
}

export function deleteWorkflowCategory(categoryId: number) {
  return request.delete<void>(`/api/workflow/category/${categoryId}`)
}

export function exportWorkflowCategories(params?: WorkflowCategoryQuery) {
  return request.getBlob('/api/workflow/category/export', { params })
}
