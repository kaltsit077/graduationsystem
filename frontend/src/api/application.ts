import request from './request'

export interface Application {
  id: number
  topicId: number
  topicTitle?: string
  teacherId?: number
  teacherName?: string
  studentId: number
  studentName?: string
  status: string
  remark?: string
  teacherFeedback?: string
  matchScore?: number
  createdAt?: string
  updatedAt?: string
}

export interface ApplicationRequest {
  topicId: number
  remark?: string
}

export interface ApplicationProcessRequest {
  status: 'APPROVED' | 'REJECTED'
  feedback?: string
}

// 提交选题申请
export const submitApplication = (data: ApplicationRequest) => {
  return request.post<Application>('/applications', data)
}

// 获取我的申请列表
export const getMyApplications = () => {
  return request.get<Application[]>('/applications/my')
}

// 获取选题的申请列表
export const getTopicApplications = (topicId: number, sortByMatchScore = true) => {
  return request.get<Application[]>(`/applications/topic/${topicId}`, {
    params: { sortByMatchScore }
  })
}

// 处理申请
export const processApplication = (id: number, data: ApplicationProcessRequest) => {
  return request.post(`/applications/${id}/process`, data)
}

