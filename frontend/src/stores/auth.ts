import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, type LoginResponse } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const role = ref<string | null>(localStorage.getItem('role'))
  const realName = ref<string | null>(localStorage.getItem('realName'))
  const userId = ref<number | null>(localStorage.getItem('userId') ? Number(localStorage.getItem('userId')) : null)
  const backgroundUrl = ref<string | null>(localStorage.getItem('backgroundUrl'))
  const backgroundScale = ref<number>(Number(localStorage.getItem('backgroundScale') || 100))
  const backgroundPosX = ref<number>(Number(localStorage.getItem('backgroundPosX') || 50))
  const backgroundPosY = ref<number>(Number(localStorage.getItem('backgroundPosY') || 50))
  const bgOverlayAlpha = ref<number>(Number(localStorage.getItem('bgOverlayAlpha') || 0.78))
  const contentAlpha = ref<number>(Number(localStorage.getItem('contentAlpha') || 1.0))
  const contentBlur = ref<number>(Number(localStorage.getItem('contentBlur') || 0))

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
    backgroundUrl.value = null
    backgroundScale.value = 100
    backgroundPosX.value = 50
    backgroundPosY.value = 50
    bgOverlayAlpha.value = 0.78
    contentAlpha.value = 1.0
    contentBlur.value = 0
    
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('realName')
    localStorage.removeItem('userId')
    localStorage.removeItem('backgroundUrl')
    localStorage.removeItem('backgroundScale')
    localStorage.removeItem('backgroundPosX')
    localStorage.removeItem('backgroundPosY')
    localStorage.removeItem('bgOverlayAlpha')
    localStorage.removeItem('contentAlpha')
    localStorage.removeItem('contentBlur')
  }

  const setRealName = (newRealName: string) => {
    realName.value = newRealName
    localStorage.setItem('realName', newRealName)
  }

  const setBackgroundUrl = (url: string | null) => {
    backgroundUrl.value = url
    if (url) {
      localStorage.setItem('backgroundUrl', url)
    } else {
      localStorage.removeItem('backgroundUrl')
    }
  }

  const setAppearance = (payload: {
    backgroundScale?: number
    backgroundPosX?: number
    backgroundPosY?: number
    bgOverlayAlpha?: number
    contentAlpha?: number
    contentBlur?: number
  }) => {
    if (typeof payload.backgroundScale === 'number') {
      backgroundScale.value = payload.backgroundScale
      localStorage.setItem('backgroundScale', String(payload.backgroundScale))
    }
    if (typeof payload.backgroundPosX === 'number') {
      backgroundPosX.value = payload.backgroundPosX
      localStorage.setItem('backgroundPosX', String(payload.backgroundPosX))
    }
    if (typeof payload.backgroundPosY === 'number') {
      backgroundPosY.value = payload.backgroundPosY
      localStorage.setItem('backgroundPosY', String(payload.backgroundPosY))
    }
    if (typeof payload.bgOverlayAlpha === 'number') {
      bgOverlayAlpha.value = payload.bgOverlayAlpha
      localStorage.setItem('bgOverlayAlpha', String(payload.bgOverlayAlpha))
    }
    if (typeof payload.contentAlpha === 'number') {
      contentAlpha.value = payload.contentAlpha
      localStorage.setItem('contentAlpha', String(payload.contentAlpha))
    }
    if (typeof payload.contentBlur === 'number') {
      contentBlur.value = payload.contentBlur
      localStorage.setItem('contentBlur', String(payload.contentBlur))
    }
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
    backgroundUrl,
    backgroundScale,
    backgroundPosX,
    backgroundPosY,
    bgOverlayAlpha,
    contentAlpha,
    contentBlur,
    setAuth,
    setRealName,
    setBackgroundUrl,
    setAppearance,
    clearAuth,
    isAuthenticated,
    isStudent,
    isTeacher,
    isAdmin
  }
})

