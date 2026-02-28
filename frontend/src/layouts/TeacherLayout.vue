<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-left">
        <h2>毕业论文选题系统 - 导师端</h2>
      </div>
      <div class="header-right">
        <el-dropdown @command="handleCommand" trigger="hover">
          <span class="user-info">
            <el-icon class="user-icon"><User /></el-icon>
            <span class="user-name">{{ authStore.realName }}</span>
            <el-icon class="arrow-icon"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">
                <el-icon><User /></el-icon>
                <span>个人资料</span>
              </el-dropdown-item>
              <el-dropdown-item command="settings" divided>
                <el-icon><Setting /></el-icon>
                <span>账号设置</span>
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon>
                <span>退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          router
          class="sidebar-menu"
        >
          <el-menu-item index="/teacher">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/teacher/topics">
            <el-icon><Document /></el-icon>
            <span>选题管理</span>
          </el-menu-item>
          <el-menu-item index="/teacher/applications">
            <el-icon><List /></el-icon>
            <span>申请处理</span>
          </el-menu-item>
          <el-menu-item index="/teacher/profile">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { HomeFilled, Document, List, User, ArrowDown, Setting, SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => route.path)

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      authStore.clearAuth()
      router.push('/login')
    } catch {
      // 用户取消
    }
  } else if (command === 'profile') {
    router.push('/teacher/profile')
  } else if (command === 'settings') {
    ElMessage.info('账号设置功能开发中')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.layout-header {
  background: #409eff;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: rgba(255, 255, 255, 0.1);
}

.user-icon {
  font-size: 18px;
}

.user-name {
  font-size: 14px;
}

.arrow-icon {
  font-size: 12px;
  transition: transform 0.3s;
}

.el-dropdown.is-active .arrow-icon {
  transform: rotate(180deg);
}

.layout-aside {
  background: #fff;
  border-right: 1px solid #e4e7ed;
}

.sidebar-menu {
  border-right: none;
  height: 100%;
}

.layout-main {
  background: #f5f7fa;
  padding: 20px;
}
</style>

