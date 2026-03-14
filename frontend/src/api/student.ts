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

interface TagRegenerateTaskCreateResponse {
  taskId: string
}

interface TagRegenerateTaskStatusResponse {
  status: 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
  error?: string
  tags?: UserTag[]
}

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

// 异步化“重抽标签”：后端立即返回 taskId，前端轮询任务状态，最终返回 tags
export const regenerateStudentTags = async (data: TagRegenerateRequest) => {
  const created = await request.post<TagRegenerateTaskCreateResponse>('/student/tags/regenerate', data, { timeout: 15000 })
  const taskId = created.data?.taskId
  if (!taskId) throw new Error('任务创建失败：缺少 taskId')

  const startedAt = Date.now()
  const timeoutMs = 120000
  const pollIntervalMs = 800

  while (Date.now() - startedAt < timeoutMs) {
    const statusRes = await request.get<TagRegenerateTaskStatusResponse>(`/student/tags/regenerate/${taskId}`, { timeout: 15000 })
    const s = statusRes.data
    if (!s) throw new Error('任务状态为空')
    if (s.status === 'SUCCEEDED') {
      return { ...statusRes, data: s.tags || [] }
    }
    if (s.status === 'FAILED') {
      throw new Error(s.error || 'AI 标签生成失败')
    }
    await sleep(pollIntervalMs)
  }

  throw new Error('AI 标签生成超时，请稍后重试')
}

// 学生修改自己的登录密码
export const changeStudentPassword = (oldPassword: string, newPassword: string) => {
  return request.post<void>('/student/change-password', { oldPassword, newPassword })
}

