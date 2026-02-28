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
