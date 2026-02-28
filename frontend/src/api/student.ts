import request from './request'

export interface StudentProfileRequest {
  major?: string
  grade?: string
  interestDesc?: string
}

export interface UserTag {
  tagName: string
  weight: number
}

export interface StudentProfileResponse {
  userId: number
  major?: string
  grade?: string
  interestDesc?: string
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

