import request from './request'

export interface StudentProfileRequest {
  realName?: string
  major?: string
  grade?: string
  interestDesc?: string
  // 标签生成模式：MAJOR / INTEREST / BOTH
  tagMode?: 'MAJOR' | 'INTEREST' | 'BOTH'
}

export interface UserTag {
  tagName: string
  weight: number
}

export interface StudentProfileResponse {
  userId: number
  username?: string
  realName?: string
  major?: string
  grade?: string
  interestDesc?: string
  tagMode?: 'MAJOR' | 'INTEREST' | 'BOTH'
  tags: UserTag[]
}

// 获取学生个人信息
export const getStudentProfile = () => {
  return request.get<StudentProfileResponse>('/student/profile')
}

// 更新学生个人信息
export const updateStudentProfile = (data: StudentProfileRequest) => {
  return request.put<StudentProfileResponse>('/student/profile', data)
}

// 获取学生标签
export const getStudentTags = () => {
  return request.get<UserTag[]>('/student/tags')
}

