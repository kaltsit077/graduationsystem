import request from './request'

export interface TeacherProfileRequest {
  realName?: string
  title?: string
  researchDirection?: string
  maxStudentCount?: number
}

export interface UserTag {
  tagName: string
  tagType?: 'MAJOR' | 'INTEREST'
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

interface TagRegenerateTaskCreateResponse {
  taskId: string
}

interface TagRegenerateTaskStatusResponse {
  status: 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
  error?: string
  tags?: UserTag[]
}

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

// 异步化“重抽导师标签”：后端立即返回 taskId，前端轮询任务状态，最终返回 tags
export const regenerateTeacherTags = async (data: TeacherTagRegenerateRequest) => {
  // 兼容两种后端返回：
  // 1) 新版：{ data: { taskId } } -> 轮询 /regenerate/{taskId}
  // 2) 旧版/降级：{ data: UserTag[] } -> 直接返回标签列表
  const created = await request.post<TagRegenerateTaskCreateResponse | UserTag[] | any>('/teacher/tags/regenerate', data, {
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
    const statusRes = await request.get<TagRegenerateTaskStatusResponse>(`/teacher/tags/regenerate/${taskId}`, { timeout: 15000 })
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

