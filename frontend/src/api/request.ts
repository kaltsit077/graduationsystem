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
      const msg = error.response.data?.message || error.response.data?.msg || ''
      if (error.response.status === 401) {
        const authStore = useAuthStore()
        const currentPath = router.currentRoute.value.path
        if (currentPath !== '/login') {
          authStore.clearAuth()
          ElMessage.error('登录已过期，请重新登录')
          router.push('/login')
        } else {
          authStore.clearAuth()
        }
      } else if (error.response.status === 403) {
        // 403 一律视为“当前账号无权限”，但不自动登出，避免误伤正常登录用户
        // 具体业务（如禁止删除管理员）在各自页面上自行处理文案
        ElMessage.error(msg || '当前账号无权限执行该操作')
      } else {
        ElMessage.error(msg || '请求失败')
      }
      // 将后端返回的 message 挂到 error 上，便于登录页等直接使用 error.message
      if (msg) {
        error.message = msg
      }
    } else {
      if (error.code === 'ECONNABORTED' || (typeof error.message === 'string' && error.message.includes('timeout'))) {
        ElMessage.error('请求超时，请稍后重试')
      } else {
        ElMessage.error('网络错误，请稍后重试')
      }
    }
    return Promise.reject(error)
  }
)

export default request

