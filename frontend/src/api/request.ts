import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200 || res.code === 0) {
      return res
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  (error) => {
    if (error.response) {
      if (error.response.status === 401 || error.response.status === 403) {
        const authStore = useAuthStore()
        const currentPath = router.currentRoute.value.path
        
        // 如果已经在登录页，不显示错误消息，避免重复提示
        if (currentPath !== '/login') {
          authStore.clearAuth()
          if (error.response.status === 401) {
            ElMessage.error('登录已过期，请重新登录')
          } else {
            ElMessage.error('无权限访问，请重新登录')
          }
          router.push('/login')
        } else {
          // 在登录页时，只清除认证信息，不显示错误消息
          authStore.clearAuth()
        }
      } else {
        ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request

