import request from '@/api/request'
import type { PageQuery, PageResult } from '@/api/types'

export interface JobRow {
  id: number
  jobName: string
  jobGroup: string
  invokeTarget: string
  cronExpression: string
  status: number
  concurrent?: number
  remark?: string
}

export interface JobQuery extends PageQuery {
  jobName?: string
  jobGroup?: string
  status?: number
}

export function pageJobs(params: JobQuery) {
  return request.get<PageResult<JobRow>>('/api/system/job/page', { params })
}

export function runJob(id: number) {
  return request.put<void>(`/api/system/job/run/${id}`)
}

export function pauseJob(id: number) {
  return request.put<void>(`/api/system/job/pause/${id}`)
}

export function resumeJob(id: number) {
  return request.put<void>(`/api/system/job/resume/${id}`)
}