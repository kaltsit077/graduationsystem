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
  return request.put<StudentProfileResponse>('/student/profile', data, { timeout: 15000 })
}

// 获取学生标签
export const getStudentTags = () => {
  return request.get<UserTag[]>('/student/tags')
}

// 保存（覆盖）学生标签：用于手动编辑/删除后落库
export const updateStudentTags = (tags: UserTag[]) => {
  return request.put<UserTag[]>('/student/tags', tags, { timeout: 15000 })
}

export interface TagRegenerateRequest {
  interestDesc?: string
  major?: string
  tagMode?: 'MAJOR' | 'INTEREST' | 'BOTH'
  pinnedTags?: UserTag[]
  excludeTagNames?: string[]
  desiredTotal?: number
}

// 交互式“重抽标签”：保留 pinnedTags，排除 excludeTagNames
export const regenerateStudentTags = (data: TagRegenerateRequest) => {
  return request.post<UserTag[]>('/student/tags/regenerate', data, { timeout: 120000 })
}

// 学生修改自己的登录密码
export const changeStudentPassword = (oldPassword: string, newPassword: string) => {
  return request.post<void>('/student/change-password', { oldPassword, newPassword })
}

