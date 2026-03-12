<template>
  <el-container class="layout-container">
    <el-header class="layout-header">
      <div class="header-left">
        <h2>毕业论文选题系统</h2>
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
      <el-aside width="220px" class="layout-aside">
        <el-menu
          :default-active="activeMenu"
          router
          class="sidebar-menu"
        >
          <el-menu-item index="/student">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/student/topics">
            <el-icon><Document /></el-icon>
            <span>选题中心</span>
          </el-menu-item>
          <el-menu-item index="/student/teachers">
            <el-icon><User /></el-icon>
            <span>导师列表</span>
          </el-menu-item>
          <el-menu-item index="/student/applications">
            <el-icon><List /></el-icon>
            <span>我的申请</span>
          </el-menu-item>
          <el-menu-item index="/student/collab">
            <el-icon><Document /></el-icon>
            <span>协作中心</span>
          </el-menu-item>
          <el-menu-item index="/student/profile">
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <div class="page-wrapper">
          <router-view />
        </div>
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
    router.push('/student/profile')
  } else if (command === 'settings') {
    ElMessage.info('账号设置功能开发中')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #eef3fb 50%, #f5f7fa 100%);
}

.layout-header {
  background: #409eff;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
  letter-spacing: 0.5px;
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
  box-shadow: 4px 0 16px rgba(0, 0, 0, 0.03);
}

.sidebar-menu {
  border-right: none;
  height: 100%;
  padding: 16px 8px;
}

.layout-main {
  background: transparent;
  padding: 24px 32px;
  display: flex;
  align-items: stretch;
  justify-content: center;
  box-sizing: border-box;
}

.page-wrapper {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 28px;
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 18px 45px rgba(15, 35, 95, 0.08);
  box-sizing: border-box;
  overflow: hidden;
  animation: fade-slide-in 0.4s ease-out;
}

:deep(.sidebar-menu .el-menu-item) {
  border-radius: 10px;
  margin-bottom: 6px;
  height: 44px;
  line-height: 44px;
  transition: all 0.25s ease-out;
}

:deep(.sidebar-menu .el-menu-item.is-active) {
  background: linear-gradient(90deg, #409eff 0%, #66b1ff 100%);
  color: #fff;
  box-shadow: 0 10px 24px rgba(64, 158, 255, 0.35);
}

:deep(.sidebar-menu .el-menu-item:hover) {
  background-color: rgba(64, 158, 255, 0.08);
  transform: translateY(-1px);
}

:deep(.sidebar-menu .el-menu-item .el-icon) {
  margin-right: 8px;
}

@keyframes fade-slide-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

