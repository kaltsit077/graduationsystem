import request from './request'

export interface TeacherProfileRequest {
  realName?: string
  title?: string
  researchDirection?: string
  maxStudentCount?: number
}

export interface UserTag {
  tagName: string
  weight: number
}

export interface TeacherProfileResponse {
  userId: number
  username?: string
  realName?: string
  title?: string
  researchDirection?: string
  maxStudentCount?: number
  tags: UserTag[]
}

// 获取导师个人信息
export const getTeacherProfile = () => {
  return request.get<TeacherProfileResponse>('/teacher/profile')
}

// 更新导师个人信息
export const updateTeacherProfile = (data: TeacherProfileRequest) => {
  return request.put<TeacherProfileResponse>('/teacher/profile', data)
}

// 获取导师标签
export const getTeacherTags = () => {
  return request.get<UserTag[]>('/teacher/tags')
}

