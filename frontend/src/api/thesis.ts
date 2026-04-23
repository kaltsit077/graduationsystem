import request from './request'

export interface Thesis {
  id: number
  topicId: number
  topicTitle?: string
  studentId: number
  studentName?: string
  fileUrl: string
  fileName: string
  fileSize: number
  stage?: string
  status: string
  createdAt?: string
  updatedAt?: string
}

export interface ThesisUploadRequest {
  topicId: number
  fileUrl: string
  fileName: string
  fileSize: number
}

// 上传论文
export const uploadThesis = (data: ThesisUploadRequest) => {
  return request.post<Thesis>('/thesis/upload', data)
}

// 上传论文文件（服务器存储）
export const uploadThesisFile = (topicId: number, file: File, stage?: string) => {
  const form = new FormData()
  form.append('topicId', String(topicId))
  if (stage) {
    form.append('stage', stage)
  }
  form.append('file', file)
  return request.post<Thesis>('/thesis/upload-file', form, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取我的论文列表
export const getMyTheses = () => {
  return request.get<Thesis[]>('/thesis/my')
}

// 获取导师的论文列表
export const getTeacherTheses = () => {
  return request.get<Thesis[]>('/thesis/teacher')
}

// 获取论文详情
export const getThesis = (id: number) => {
  return request.get<Thesis>(`/thesis/${id}`)
}

export const reviewThesisWorkflow = (thesisId: number, decision: 'APPROVE' | 'NEED_REVISION') => {
  return request.post<void>(`/thesis/${thesisId}/workflow`, { decision })
}

