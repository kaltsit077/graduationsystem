import request from './request'

export interface Topic {
  id: number
  teacherId: number
  teacherName?: string
  title: string
  description?: string
  status: string
  maxApplicants: number
  currentApplicants: number
  /** 学生端可选：当前学生与该选题的匹配度（0.30-1.00） */
  matchScore?: number
  tags?: string[]
  createdAt?: string
  updatedAt?: string
}

export interface TopicRequest {
  title: string
  description?: string
  maxApplicants: number
  tags?: string[]
}

export interface TopicDuplicateCheckRequest {
  topicId?: number
  title: string
  description?: string
}

export interface TopicDuplicateCheckResponse {
  passed: boolean
  maxSimilarity: number
  similarTopicId?: number
  similarTopicTitle?: string
}

export interface AiGeneratedTopic {
  title: string
  description?: string
  tags?: string[]
  maxSimilarity?: number
  similarTopicTitle?: string
  passed: boolean
}

// 获取已开放的选题列表（学生端）
export const getOpenTopics = () => {
  return request.get<Topic[]>('/topics/open')
}

// 获取已开放的选题列表（学生端，附带匹配度）
export const getOpenTopicsWithScore = () => {
  return request.get<Topic[]>('/topics/open-with-score')
}

// 获取选题列表（可按状态筛选）
export const getTopics = (params?: { status?: string; teacherId?: number }) => {
  return request.get<Topic[]>('/topics', { params })
}

// 获取选题详情
export const getTopic = (id: number) => {
  return request.get<Topic>(`/topics/${id}`)
}

// 创建选题
export const createTopic = (data: TopicRequest) => {
  return request.post<Topic>('/topics', data)
}

// 更新选题
export const updateTopic = (id: number, data: TopicRequest) => {
  return request.put<Topic>(`/topics/${id}`, data)
}

// 去重检测
export const checkTopicDuplicate = (data: TopicDuplicateCheckRequest) => {
  return request.post<TopicDuplicateCheckResponse>('/topics/check-duplicate', data)
}

// 提交选题审核
export const submitTopicForReview = (id: number) => {
  return request.post(`/topics/${id}/submit-review`)
}

// 删除选题（仅草稿/已驳回）
export const deleteTopic = (id: number) => {
  return request.delete(`/topics/${id}`)
}

/** 导师 AI 选题生成请求：多维度选题需求均为可选，信息越完整生成越贴近需求 */
export interface AiGenerateTopicsParams {
  count?: number
  tagNames?: string[]
  /** 未填写下方五类时作为整体选题需求说明 */
  preferenceHint?: string
  backgroundHint?: string
  contentHint?: string
  abilityHint?: string
  dataHint?: string
  innovationHint?: string
}

// 基于导师标签的 AI 选题生成（可能调用外部大模型，延迟较高，单独拉长超时时间）
export const generateAiTopics = (data: AiGenerateTopicsParams) => {
  return request.post<AiGeneratedTopic[]>('/topics/ai-generate', data, { timeout: 300000 })
}

