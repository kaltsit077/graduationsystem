import request from './request'

export interface ThesisEvaluationRequest {
  thesisId: number
  score?: number
  defenseScore?: number
  reviewScore?: number
  gradeLevel?: string
  comment?: string
}

export interface ThesisEvaluation {
  thesisId: number
  score?: number
  defenseScore?: number
  reviewScore?: number
  gradeLevel?: string
  comment?: string
  studentScore?: number
  studentComment?: string
}

export interface TopicMetrics {
  topicId: number
  topicTitle: string
  avgScore?: number
  totalStudents: number
  excellentRatio?: number
  failRatio?: number
  avgStudentScore?: number
}

export interface TeacherMetrics {
  teacherId: number
  teacherName: string
  totalStudents: number
  avgScore?: number
  excellentRatio?: number
  failRatio?: number
  avgStudentScore?: number
}

// 保存/更新论文评价
export const saveThesisEvaluation = (data: ThesisEvaluationRequest) => {
  return request.post<void>('/eval/thesis', data)
}

// 获取导师自己的选题质量统计
export const getTeacherTopicMetrics = () => {
  return request.get<TopicMetrics[]>('/eval/teacher/topics')
}

// 管理员查看全局选题质量统计
export const getAdminTopicMetrics = () => {
  return request.get<TopicMetrics[]>('/eval/admin/topics')
}

// 管理员查看导师维度质量统计
export const getAdminTeacherMetrics = () => {
  return request.get<TeacherMetrics[]>('/eval/admin/teachers')
}

// 导师查看自己总体质量指标
export const getTeacherSummary = () => {
  return request.get<TeacherMetrics>('/eval/teacher/summary')
}

// 学生提交论文与导师评价
export const saveStudentThesisFeedback = (data: ThesisEvaluationRequest) => {
  return request.post<void>('/eval/thesis/student', data)
}

// 获取某篇论文的评价详情
export const getThesisEvaluation = (thesisId: number) => {
  return request.get<ThesisEvaluation>(`/eval/thesis/${thesisId}`)
}

