import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import request from '@/api/request'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/student',
      component: () => import('@/layouts/StudentLayout.vue'),
      meta: { requiresAuth: true, role: 'STUDENT' },
      children: [
        {
          path: '',
          name: 'StudentHome',
          component: () => import('@/views/student/Home.vue')
        },
        {
          path: 'topics',
          name: 'StudentTopics',
          component: () => import('@/views/student/Topics.vue')
        },
        {
          path: 'applications',
          name: 'StudentApplications',
          component: () => import('@/views/student/Applications.vue')
        },
        {
          path: 'profile',
          name: 'StudentProfile',
          component: () => import('@/views/student/Profile.vue')
        }
      ]
    },
    {
      path: '/teacher',
      component: () => import('@/layouts/TeacherLayout.vue'),
      meta: { requiresAuth: true, role: 'TEACHER' },
      children: [
        {
          path: '',
          name: 'TeacherHome',
          component: () => import('@/views/teacher/Home.vue')
        },
        {
          path: 'topics',
          name: 'TeacherTopics',
          component: () => import('@/views/teacher/Topics.vue')
        },
        {
          path: 'applications',
          name: 'TeacherApplications',
          component: () => import('@/views/teacher/Applications.vue')
        },
        {
          path: 'profile',
          name: 'TeacherProfile',
          component: () => import('@/views/teacher/Profile.vue')
        }
      ]
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true, role: 'ADMIN' },
      children: [
        {
          path: '',
          name: 'AdminHome',
          component: () => import('@/views/admin/Home.vue')
        },
        {
          path: 'reviews',
          name: 'AdminReviews',
          component: () => import('@/views/admin/Reviews.vue')
        },
        {
          path: 'accounts',
          name: 'AdminAccounts',
          component: () => import('@/views/admin/Accounts.vue')
        }
      ]
    }
  ]
})

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()
  
  // 登录页面不需要验证
  if (to.meta.requiresAuth === false) {
    // 如果已登录，先验证 token 有效性
    if (authStore.isAuthenticated() && to.path === '/login') {
      try {
        await request.get('/auth/me')
        // Token 有效，重定向到对应角色首页
        if (authStore.isAdmin()) {
          next('/admin')
        } else if (authStore.isTeacher()) {
          next('/teacher')
        } else if (authStore.isStudent()) {
          next('/student')
        } else {
          next()
        }
      } catch (error) {
        // Token 无效，清除认证信息，允许访问登录页
        authStore.clearAuth()
        next()
      }
      return
    }
    next()
    return
  }

  // 需要认证的页面
  if (!authStore.isAuthenticated()) {
    next('/login')
    return
  }

  // 验证 token 有效性 - 必须验证成功才能继续
  try {
    const response = await request.get('/auth/me')
    // 验证响应数据是否有效
    if (!response || !response.data || !response.data.userId) {
      throw new Error('Token 验证失败：响应数据无效')
    }
  } catch (error: any) {
    // Token 无效或验证失败，清除认证信息并跳转到登录页
    console.log('Token 验证失败，清除认证信息:', error)
    authStore.clearAuth()
    next('/login')
    return
  }

  // 检查角色匹配
  if (to.meta.role && authStore.role !== to.meta.role) {
    // 角色不匹配，重定向到对应角色的首页
    if (authStore.isStudent()) {
      next('/student')
    } else if (authStore.isTeacher()) {
      next('/teacher')
    } else if (authStore.isAdmin()) {
      next('/admin')
    } else {
      // 角色无效，清除认证信息并跳转到登录页
      authStore.clearAuth()
      next('/login')
    }
    return
  }

  next()
})

export default router

