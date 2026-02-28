import request from './request'

export interface StatsResponse {
  applicationCount?: number
  unreadNotificationCount?: number
  approvedApplicationCount?: number
  topicCount?: number
  pendingApplicationCount?: number
  pendingReviewTopicCount?: number
}

// 获取学生端统计数据
export const getStudentStats = () => {
  return request.get<StatsResponse>('/stats/student')
}

// 获取导师端统计数据
export const getTeacherStats = () => {
  return request.get<StatsResponse>('/stats/teacher')
}

// 获取管理员端统计数据
export const getAdminStats = () => {
  return request.get<StatsResponse>('/stats/admin')
}

