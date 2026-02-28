<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/api/request'

const authStore = useAuthStore()

// 应用启动时验证 token 有效性
onMounted(async () => {
  if (authStore.isAuthenticated()) {
    try {
      // 调用需要认证的接口来验证 token 有效性
      const res = await request.get('/auth/me')
      // 验证响应数据是否有效
      if (res && res.data && res.data.userId) {
        // token 有效，可以继续使用
        console.log('Token 验证成功')
      } else {
        // 响应数据无效，清除认证信息
        console.warn('Token 验证失败：响应数据无效')
        authStore.clearAuth()
        // 如果当前不在登录页，跳转到登录页
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }
    } catch (error: any) {
      // 如果 token 无效（401/403），响应拦截器已经处理了清除 token 和跳转
      // 这里只需要处理网络错误等其他情况
      if (error.response?.status === 401 || error.response?.status === 403) {
        // 401/403 错误，响应拦截器已处理，这里只记录日志
        console.log('Token 无效，已清除认证信息')
      } else {
        // 网络错误或其他错误，也清除 token 以确保安全
        console.warn('Token 验证失败:', error)
        authStore.clearAuth()
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }
    }
  }
})
</script>

