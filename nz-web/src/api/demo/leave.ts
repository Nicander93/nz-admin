import request from '@/api/request'
export interface LeaveDraft {
  reason: string
  startDate: string
  endDate: string
  flowCode: string
}
export interface LeaveApplication extends LeaveDraft {
  id: string
  status: string
  instanceId?: string
}
export const listLeaveApplications = () =>
  request.get<LeaveApplication[]>('/api/demo/leave')
export const saveLeaveApplication = (
  body: LeaveDraft,
  key: string,
  id?: string,
) =>
  id
    ? request.put<string>(`/api/demo/leave/${id}`, body)
    : request.post<string>('/api/demo/leave', body, {
        headers: { 'Idempotency-Key': key },
      })
export const submitLeaveApplication = (id: string, key: string) =>
  request.post<string>(`/api/demo/leave/${id}/submit`, undefined, {
    headers: { 'Idempotency-Key': key },
  })

export const listLeaveFlows = () =>
  request.get<{ flowCode: string; flowName: string }[]>('/api/demo/leave/flows')
