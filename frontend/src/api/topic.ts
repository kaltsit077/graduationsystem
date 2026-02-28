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

// 获取已开放的选题列表（学生端）
export const getOpenTopics = () => {
  return request.get<Topic[]>('/topics/open')
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

