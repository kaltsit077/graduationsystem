import request from './request'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  realName: string
  role: 'STUDENT' | 'TEACHER'
}

export interface LoginResponse {
  token: string
  role: string
  realName: string
  userId: number
}

export const login = (data: LoginRequest) => {
  return request.post<LoginResponse>('/auth/login', data)
}

export const register = (data: RegisterRequest) => {
  return request.post<LoginResponse>('/auth/register', data)
}

