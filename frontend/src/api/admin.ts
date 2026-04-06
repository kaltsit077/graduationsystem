import request from './request'

export interface AdminUser {
  id: number
  username: string
  realName: string
  /** 密码显示（掩码，非明文） */
  passwordDisplay?: string
  role: string
  status: number
  createdAt: string
  updatedAt?: string
}

export const getAdminTeachers = () => {
  return request.get<AdminUser[]>('/admin/users/teachers')
}

export const getAdminStudents = () => {
  return request.get<AdminUser[]>('/admin/users/students')
}

/** 管理员修改指定用户密码（POST，与批量重置同一方式） */
export const updateUserPassword = (userId: number, newPassword: string) => {
  return request.post<void>('/admin/users/change-password', { userId, newPassword })
}

/** 管理员批量重置密码为 123456 */
export const resetPasswordsToDefault = (userIds: number[]) => {
  return request.post<number>('/admin/users/reset-password', { userIds })
}

/** 管理员删除账号（单个/批量） */
export const deleteUsers = (userIds: number[]) => {
  return request.post<number>('/admin/users/delete', { userIds })
}

export interface MonitorStatus {
  uptimeMillis: number
  uptime: string
  heapUsedMb: number
  heapMaxMb: number
  threadCount: number
  systemLoadAverage: number
  activeDbConnections: number
  idleDbConnections: number
  totalDbConnections: number
  maxDbConnections: number
  dbMetricsAvailable: boolean
}

export const getMonitorStatus = () => {
  return request.get<MonitorStatus>('/admin/monitor/status')
}

export const getMonitorLogs = (params: { level?: string; lines?: number }) => {
  return request.get<string[]>('/admin/monitor/logs', { params })
}

/** 手动清除监控日志文件内容 */
export const clearMonitorLogs = () => {
  return request.post<void>('/admin/monitor/logs/clear')
}

// 选题系统开放设置
export interface SelectionSetting {
  enabled: boolean
  startTime?: string | null
  endTime?: string | null
  openNow: boolean
}

export const getSelectionSetting = () => {
  return request.get<SelectionSetting>('/admin/selection-setting')
}

export const updateSelectionSetting = (data: {
  enabled: boolean
  startTime?: string | null
  endTime?: string | null
}) => {
  return request.post<SelectionSetting>('/admin/selection-setting', data)
}
