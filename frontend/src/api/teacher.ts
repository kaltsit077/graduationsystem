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
  // 自动生成标签可能触发 AI 调用，耗时会明显长于普通接口
  return request.put<TeacherProfileResponse>('/teacher/profile', data, { timeout: 30000 })
}

// 获取导师标签
export const getTeacherTags = () => {
  return request.get<UserTag[]>('/teacher/tags')
}

// 导师修改自己的登录密码
export const changeTeacherPassword = (oldPassword: string, newPassword: string) => {
  return request.post<void>('/teacher/change-password', { oldPassword, newPassword })
}

export interface TeacherTagRegenerateRequest {
  interestDesc?: string
  pinnedTags?: UserTag[]
  excludeTagNames?: string[]
  desiredTotal?: number
}

// 保存（覆盖）导师标签：用于手动编辑/删除后落库
export const updateTeacherTags = (tags: UserTag[]) => {
  return request.put<UserTag[]>('/teacher/tags', tags, { timeout: 15000 })
}

// 交互式“重抽导师标签”：保留 pinnedTags，排除 excludeTagNames
export const regenerateTeacherTags = (data: TeacherTagRegenerateRequest) => {
  return request.post<UserTag[]>('/teacher/tags/regenerate', data, { timeout: 120000 })
}

