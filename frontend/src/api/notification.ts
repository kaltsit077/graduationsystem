import request from './request'

export interface Notification {
  id: number
  type: string
  title: string
  content: string
  isRead: number
  relatedId?: number
  collabStage?: string | null
  createdAt: string
}

// 获取通知列表
export const getNotifications = (params?: {
  isRead?: boolean
  limit?: number
  type?: string
  relatedId?: number
  collabStage?: string
}) => {
  return request.get<Notification[]>('/notifications', { params })
}

// 获取未读通知数量
export const getUnreadCount = () => {
  return request.get<number>('/notifications/unread-count')
}

// 标记通知为已读
export const markNotificationAsRead = (id: number) => {
  return request.post(`/notifications/${id}/read`)
}

// 标记所有通知为已读
export const markAllNotificationsAsRead = () => {
  return request.post('/notifications/read-all')
}

// 发送聊天消息
export const sendChatMessage = (
  targetUserId: number,
  content: string,
  relatedId?: number,
  collabStage?: string
) => {
  return request.post('/notifications/chat', null, {
    params: { targetUserId, content, relatedId, collabStage }
  })
}

