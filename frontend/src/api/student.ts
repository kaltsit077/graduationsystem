import request from './request'

export interface StudentProfileRequest {
  realName?: string
  major?: string
  majorCourses?: string
  grade?: string
  interestDesc?: string
  // 标签生成模式：MAJOR / INTEREST / BOTH
  tagMode?: 'MAJOR' | 'INTEREST' | 'BOTH'
}

export interface UserTag {
  tagName: string
  tagType?: 'MAJOR' | 'INTEREST'
  weight: number
}

export interface StudentProfileResponse {
  userId: number
  username?: string
  realName?: string
  major?: string
  majorCourses?: string
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
  majorCourses?: string
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
  // 兼容两种后端返回：
  // 1) 新版：{ data: { taskId } } -> 轮询 /regenerate/{taskId}
  // 2) 旧版/降级：{ data: UserTag[] } -> 直接返回标签列表
  const created = await request.post<TagRegenerateTaskCreateResponse | UserTag[] | any>('/student/tags/regenerate', data, {
    timeout: 15000
  })

  if (Array.isArray(created.data)) {
    return { ...created, data: created.data as UserTag[] }
  }

  const taskId: string | undefined = created.data?.taskId
  if (!taskId) {
    const tags = created.data?.tags
    if (Array.isArray(tags)) {
      return { ...created, data: tags as UserTag[] }
    }
    throw new Error('任务创建失败：缺少 taskId')
  }

  const startedAt = Date.now()
  // 最大等待时长加长，避免 AI 慢导致频繁超时
  const timeoutMs = 300000
  // 统一轮询间隔：2.5s（减少请求量）
  const pollIntervalMs = 2500

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

export const changeStudentPassword = (oldPassword: string, newPassword: string) => {
  return request.post<void>('/student/change-password', { oldPassword, newPassword })
}

