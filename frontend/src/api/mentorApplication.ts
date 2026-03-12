import request from './request'

export type MentorApplicationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

export interface MentorApplication {
  id: number
  studentId: number
  teacherId: number
  status: MentorApplicationStatus
  reason?: string
  teacherComment?: string
  createdAt: string
  updatedAt: string
}

export interface MentorApplicationCreatePayload {
  teacherId: number
  reason?: string
}

export interface TeacherOverview {
  teacherId: number
  realName?: string
  title?: string
  researchDirection?: string
  maxStudentCount?: number
  currentStudentCount?: number
  openTopicCount?: number
  tags?: string[]
  totalStudents?: number
  avgScore?: number
  excellentRatio?: number
  failRatio?: number
}

// 学生端：获取导师列表概览
export const getTeacherOverviewList = () => {
  return request.get<TeacherOverview[]>('/mentor-applications/teachers/overview')
}

// 学生端：发起拜师申请
export const createMentorApplication = (payload: MentorApplicationCreatePayload) => {
  return request.post<MentorApplication>('/mentor-applications', payload)
}

// 学生端：查看自己的拜师申请
export const getMyMentorApplications = () => {
  return request.get<MentorApplication[]>('/mentor-applications/my')
}

// 导师端：查看待处理的拜师申请
export const getTeacherPendingMentorApplications = () => {
  return request.get<MentorApplication[]>('/mentor-applications/teacher/pending')
}

// 导师端：审批拜师申请
export const submitMentorApplicationDecision = (
  id: number,
  status: Exclude<MentorApplicationStatus, 'PENDING' | 'CANCELLED'>,
  comment?: string
) => {
  return request.post<void>(`/mentor-applications/${id}/decision`, null, {
    params: { status, comment }
  })
}

// 导师端：为已通过的拜师申请指派课题
export const assignTopicForMentorApplication = (id: number, topicId: number) => {
  return request.post<void>(`/mentor-applications/${id}/assign-topic`, null, {
    params: { topicId }
  })
}

