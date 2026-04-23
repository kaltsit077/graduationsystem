<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import request from '@/api/request'

const authStore = useAuthStore()

onMounted(async () => {
  if (authStore.isAuthenticated()) {
    try {
      const res = await request.get('/auth/me')
      if (res && res.data && res.data.userId) {
        console.log('Token 验证成功')
      } else {
        console.warn('Token 验证失败：响应数据无效')
        authStore.clearAuth()
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }
    } catch (error: any) {
      if (error.response?.status === 401 || error.response?.status === 403) {
        console.log('Token 无效，已清除认证信息')
      } else {
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

