import request from '@/api/request'
import type { PageQuery, PageResult, R } from '@/api/types'

export type ScheduleState = 'PENDING' | 'SYNCED' | 'ERROR'
export type ExecutionState =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'SKIPPED'

export interface JobRow {
  id: number
  jobName: string
  jobGroup: string
  invokeTarget: string
  cronExpression: string
  status: number
  concurrent?: number
  misfirePolicy?: number
  remark?: string
  revision: number
  scheduleState: ScheduleState
  scheduleError?: string
}

export interface JobQuery extends PageQuery {
  jobName?: string
  jobGroup?: string
  status?: number
}

export interface JobWriteInput {
  id?: number
  jobName: string
  jobGroup: string
  invokeTarget: string
  cronExpression: string
  status: number
  concurrent: number
  misfirePolicy: number
  remark: string
}

export interface JobMutationReceipt {
  jobId: number
  revision: number
  scheduleState: ScheduleState
}

export interface ExecutionReceipt {
  executionId: number
  executionKey: string
  state: ExecutionState
}

export interface JobExecution {
  id: number
  jobId: number
  executionKey: string
  state: ExecutionState
  triggerSource: string
  nodeId?: string
  recovered: boolean
  recoverable: boolean
  attempt: number
  lastError?: string
  createTime: string
  startTime?: string
  endTime?: string
}

export interface ScheduleOutbox {
  id: number
  jobId: number
  revision: number
  operation: 'UPSERT' | 'DELETE'
  state: ScheduleState
  attempts: number
  lastError?: string
  nextAttemptTime?: string
}

export interface ScheduleOutboxQuery extends PageQuery {
  jobId?: number
  state?: ScheduleState
}

export interface JobExecutionQuery extends PageQuery {
  jobId?: number
  state?: ExecutionState
  executionKey?: string
}

export interface JobLog {
  id: number
  jobId: number
  jobName: string
  jobGroup: string
  invokeTarget: string
  jobMessage: string
  status: number
  exceptionInfo?: string
  createTime: string
  executionId?: number
  executionKey?: string
  triggerSource?: string
  nodeId?: string
  recovered?: boolean
  attempt?: number
}

export interface JobLogQuery extends PageQuery {
  jobId?: number
  jobName?: string
  jobGroup?: string
  status?: number
}

export function pageJobs(params: JobQuery) {
  return request.get<PageResult<JobRow>>('/api/system/job/page', { params })
}

export function getJob(id: number) {
  return request.get<JobRow>(`/api/system/job/${id}`)
}

export function createJob(input: JobWriteInput) {
  return request.post<JobMutationReceipt>('/api/system/job', input)
}

export function updateJob(input: JobWriteInput & { id: number }) {
  return request.put<JobMutationReceipt>('/api/system/job', input)
}

export function deleteJob(id: number) {
  return request.delete<JobMutationReceipt>(`/api/system/job/${id}`)
}

export function runJob(id: number, idempotencyKey: string) {
  return request.put<ExecutionReceipt>(`/api/system/job/run/${id}`, undefined, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
}

export function pauseJob(id: number) {
  return request.put<JobMutationReceipt>(`/api/system/job/pause/${id}`)
}

export function resumeJob(id: number) {
  return request.put<JobMutationReceipt>(`/api/system/job/resume/${id}`)
}

export function pageExecutions(params: JobExecutionQuery) {
  return request.get<PageResult<JobExecution>>(
    '/api/system/job/execution/page',
    { params },
  )
}

export function pageScheduleOutbox(params: ScheduleOutboxQuery) {
  return request.get<PageResult<ScheduleOutbox>>('/api/system/job/schedule-outbox/page', { params })
}

export function retryScheduleOutbox(id: number) {
  return request.put<JobMutationReceipt>(`/api/system/job/schedule-outbox/${id}/retry`)
}

export function retryExecution(id: number) {
  return request.put<ExecutionReceipt>(`/api/system/job/execution/${id}/retry`)
}

export function getExecution(id: number) {
  return request.get<JobExecution>(`/api/system/job/execution/${id}`)
}

export function pageJobLogs(params: JobLogQuery) {
  return request.get<PageResult<JobLog>>('/api/system/job/log/page', { params })
}

export function getJobLog(id: number) {
  return request.get<JobLog>(`/api/system/job/log/${id}`)
}

export type { R }
