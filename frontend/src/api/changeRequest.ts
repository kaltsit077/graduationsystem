import request from './request'

export type ChangeType = 'CHANGE_TOPIC' | 'CHANGE_TEACHER'
export type ChangeStatus = 'PENDING_TEACHER' | 'PENDING_ADMIN' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
export type Decision = 'APPROVED' | 'REJECTED'

export interface ChangeRequest {
  id: number
  studentId: number
  currentApplicationId: number
  type: ChangeType
  reason: string
  targetTopicId?: number
  targetTeacherId?: number
  status: ChangeStatus
  teacherDecision?: Decision
  teacherComment?: string
  adminDecision?: Decision
  adminComment?: string
  createdAt: string
  updatedAt: string
}

export interface ChangeRequestCreatePayload {
  type: ChangeType
  reason: string
  targetTopicId?: number
  targetTeacherId?: number
}

// 学生发起变更申请
export const createChangeRequest = (payload: ChangeRequestCreatePayload) => {
  return request.post<ChangeRequest>('/change-requests', payload)
}

// 学生查看自己的变更申请
export const getMyChangeRequests = () => {
  return request.get<ChangeRequest[]>('/change-requests/my')
}

// 导师查看名下待处理的“更换选题”申请
export const getTeacherPendingChangeRequests = () => {
  return request.get<ChangeRequest[]>('/change-requests/teacher/pending')
}

// 导师审批“更换选题”申请
export const submitTeacherChangeDecision = (id: number, decision: Decision, comment?: string) => {
  return request.post<void>('/change-requests/' + id + '/teacher-decision', null, {
    params: { decision, comment }
  })
}

// 管理员查看待处理的“更换导师”申请
export const getAdminPendingChangeRequests = () => {
  return request.get<ChangeRequest[]>('/change-requests/admin/pending')
}

// 管理员审批“更换导师”申请
export const submitAdminChangeDecision = (id: number, decision: Decision, comment?: string) => {
  return request.post<void>('/change-requests/' + id + '/admin-decision', null, {
    params: { decision, comment }
  })
}

