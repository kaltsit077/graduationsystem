import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, type LoginResponse } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const role = ref<string | null>(localStorage.getItem('role'))
  const realName = ref<string | null>(localStorage.getItem('realName'))
  const userId = ref<number | null>(localStorage.getItem('userId') ? Number(localStorage.getItem('userId')) : null)

  const setAuth = (data: LoginResponse) => {
    token.value = data.token
    role.value = data.role
    realName.value = data.realName
    userId.value = data.userId
    
    localStorage.setItem('token', data.token)
    localStorage.setItem('role', data.role)
    localStorage.setItem('realName', data.realName)
    localStorage.setItem('userId', String(data.userId))
  }

  const clearAuth = () => {
    token.value = null
    role.value = null
    realName.value = null
    userId.value = null
    
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('realName')
    localStorage.removeItem('userId')
  }

  const isAuthenticated = () => {
    return token.value !== null
  }

  const isStudent = () => {
    return role.value === 'STUDENT'
  }

  const isTeacher = () => {
    return role.value === 'TEACHER'
  }

  const isAdmin = () => {
    return role.value === 'ADMIN'
  }

  return {
    token,
    role,
    realName,
    userId,
    setAuth,
    clearAuth,
    isAuthenticated,
    isStudent,
    isTeacher,
    isAdmin
  }
})

