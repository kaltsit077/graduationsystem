<template>
  <el-container class="layout-container" :style="layoutBgStyle">
    <el-header class="layout-header">
      <div class="header-left">
        <button
          class="aside-toggle"
          type="button"
          @click="toggleAside"
          :class="{ collapsed: isAsideCollapsed }"
          :aria-label="isAsideCollapsed ? '展开侧边栏' : '折叠侧边栏'"
        >
          <span
            class="win-tiles"
            :class="[{ diamond: isAsideCollapsed, 'square-back': squareBackAnimating }]"
            aria-hidden="true"
          >
            <i class="tile t1"></i>
            <i class="tile t2"></i>
            <i class="tile t3"></i>
            <i class="tile t4"></i>
          </span>
        </button>
        <h2>毕业论文选题系统 - 管理员端</h2>
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
      <el-aside :width="asideWidth" class="layout-aside" :class="{ collapsed: isAsideCollapsed }">
        <el-menu :default-active="activeMenu" router class="sidebar-menu" :collapse="isAsideCollapsed" :collapse-transition="false">
          <el-menu-item index="/admin">
            <el-icon><HomeFilled /></el-icon>
            <template #title>首页</template>
          </el-menu-item>
          <el-menu-item index="/admin/reviews">
            <el-icon><Document /></el-icon>
            <template #title>选题审核</template>
          </el-menu-item>
          <el-menu-item index="/admin/accounts">
            <el-icon><User /></el-icon>
            <template #title>账号管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/teacher-load">
            <el-icon><Document /></el-icon>
            <template #title>导师负荷与变更</template>
          </el-menu-item>
          <el-menu-item index="/admin/evaluation">
            <el-icon><Document /></el-icon>
            <template #title>选题质量分析</template>
          </el-menu-item>
          <el-menu-item index="/admin/monitor">
            <el-icon><Document /></el-icon>
            <template #title>系统监控</template>
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
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { HomeFilled, Document, User, ArrowDown, Setting, SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getMyBackground } from '@/api/userPreference'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => route.path)

const isAsideCollapsed = ref(false)
const squareBackAnimating = ref(false)
const asideWidth = computed(() => (isAsideCollapsed.value ? '64px' : '220px'))
const isMobile = () => window.matchMedia('(max-width: 768px)').matches

const layoutBgStyle = computed(() => {
  const url = authStore.backgroundUrl
  const overlay = Number.isFinite(authStore.bgOverlayAlpha) ? authStore.bgOverlayAlpha : 0.78
  const scale = Number.isFinite(authStore.backgroundScale) ? authStore.backgroundScale : 100
  const posX = Number.isFinite(authStore.backgroundPosX) ? authStore.backgroundPosX : 50
  const posY = Number.isFinite(authStore.backgroundPosY) ? authStore.backgroundPosY : 50
  const contentAlpha = Number.isFinite(authStore.contentAlpha) ? authStore.contentAlpha : 1
  const contentBlur = Number.isFinite(authStore.contentBlur) ? authStore.contentBlur : 0

  const vars: Record<string, string> = {
    '--app-content-alpha': String(contentAlpha),
    '--app-content-blur': `${contentBlur}px`
  }

  if (!url) return vars
  return {
    ...vars,
    backgroundImage: `linear-gradient(135deg, rgba(245, 247, 250, ${overlay}) 0%, rgba(238, 243, 251, ${overlay}) 50%, rgba(245, 247, 250, ${overlay}) 100%), url(${url})`,
    backgroundSize: `${scale}%`,
    backgroundPosition: `${posX}% ${posY}%`,
    backgroundRepeat: 'no-repeat',
    backgroundAttachment: 'fixed'
  } as Record<string, string>
})

onMounted(async () => {
  // 小屏默认折叠侧边栏，避免内容被挤压
  isAsideCollapsed.value = isMobile()

  const onResize = () => {
    if (isMobile()) isAsideCollapsed.value = true
  }
  window.addEventListener('resize', onResize, { passive: true })
  onUnmounted(() => window.removeEventListener('resize', onResize))

  if (authStore.isAuthenticated() && !authStore.backgroundUrl) {
    try {
      const res = await getMyBackground()
      authStore.setBackgroundUrl(res.data?.backgroundUrl || null)
      authStore.setAppearance({
        backgroundScale: res.data?.backgroundScale ?? 100,
        backgroundPosX: res.data?.backgroundPosX ?? 50,
        backgroundPosY: res.data?.backgroundPosY ?? 50,
        bgOverlayAlpha: res.data?.bgOverlayAlpha ?? 0.78,
        contentAlpha: res.data?.contentAlpha ?? 1.0,
        contentBlur: res.data?.contentBlur ?? 0
      })
    } catch {
      // ignore
    }
  }
})

const toggleAside = () => {
  if (isAsideCollapsed.value) {
    isAsideCollapsed.value = false
    squareBackAnimating.value = true
    setTimeout(() => {
      squareBackAnimating.value = false
    }, 260)
  } else {
    isAsideCollapsed.value = true
    squareBackAnimating.value = false
  }
}

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
    // 跳转到个人资料页面（如果有的话）
    ElMessage.info('个人资料功能开发中')
  } else if (command === 'settings') {
    // 跳转到账号设置页面（如果有的话）
    ElMessage.info('账号设置功能开发中')
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
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

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
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
  font-size: 17px;
  font-weight: 600;
  color: #fff;
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
  position: relative;
  transition: width 0.25s ease-out;
  overflow: hidden;
}

.sidebar-menu {
  border-right: none;
  height: 100%;
  padding: 16px 8px;
}

.layout-aside.collapsed .sidebar-menu {
  padding: 56px 6px 10px;
}

.aside-toggle {
  position: relative;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 10px;
  background: transparent;
  box-shadow: none;
  cursor: pointer;
  display: grid;
  place-items: center;
  padding: 0;
  transition: transform 0.2s ease;
}

.aside-toggle:hover {
  transform: translateY(-1px);
}

.aside-toggle:active {
  transform: translateY(0);
}

.win-tiles {
  width: 18px;
  height: 18px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 3px;
  transition: transform 0.25s ease;
}

.tile {
  width: 100%;
  height: 100%;
  border-radius: 3px;
  background: linear-gradient(135deg, #ffffff 0%, #e6f4ff 100%);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.92);
}

.win-tiles.diamond {
  transform: rotate(45deg) scale(1.02);
  animation: win-diamond 320ms cubic-bezier(0.2, 1.1, 0.2, 1) both;
}

/* 正方形 -> 菱形：四个小方块先散开/回弹，再合拢旋转 */
.win-tiles.diamond .tile {
  animation: tile-burst 320ms cubic-bezier(0.2, 1.1, 0.2, 1) both;
}

.win-tiles.diamond .t1 {
  --dx: -5px;
  --dy: -5px;
}
.win-tiles.diamond .t2 {
  --dx: 5px;
  --dy: -5px;
}
.win-tiles.diamond .t3 {
  --dx: -5px;
  --dy: 5px;
}
.win-tiles.diamond .t4 {
  --dx: 5px;
  --dy: 5px;
}

@keyframes tile-burst {
  0% {
    transform: translate(0, 0) scale(1);
  }
  35% {
    transform: translate(var(--dx), var(--dy)) scale(0.94);
  }
  70% {
    transform: translate(calc(var(--dx) * 0.45), calc(var(--dy) * 0.45)) scale(1.08);
  }
  100% {
    transform: translate(0, 0) scale(1);
  }
}

@keyframes win-diamond {
  0% {
    transform: rotate(0deg) scale(1);
  }
  55% {
    transform: rotate(0deg) scale(1.06);
  }
  100% {
    transform: rotate(45deg) scale(1.02);
  }
}

.win-tiles.square-back {
  animation: win-square 320ms cubic-bezier(0.2, 1.1, 0.2, 1) both;
}

.win-tiles.square-back .tile {
  animation: tile-burst-back 320ms cubic-bezier(0.2, 1.1, 0.2, 1) both;
}

@keyframes tile-burst-back {
  0% {
    transform: translate(0, 0) scale(1);
  }
  35% {
    transform: translate(calc(var(--dx) * -0.2), calc(var(--dy) * -0.2)) scale(0.94);
  }
  70% {
    transform: translate(calc(var(--dx) * 0.25), calc(var(--dy) * 0.25)) scale(1.08);
  }
  100% {
    transform: translate(0, 0) scale(1);
  }
}

@keyframes win-square {
  0% {
    transform: rotate(45deg) scale(1.02);
  }
  55% {
    transform: rotate(45deg) scale(1.06);
  }
  100% {
    transform: rotate(0deg) scale(1);
  }
}

.layout-aside.collapsed :deep(.el-menu-item .el-icon) {
  animation: icon-pop 420ms cubic-bezier(0.2, 1.1, 0.2, 1) both;
}

.layout-aside.collapsed :deep(.el-menu-item:nth-child(1) .el-icon) {
  animation-delay: 40ms;
}
.layout-aside.collapsed :deep(.el-menu-item:nth-child(2) .el-icon) {
  animation-delay: 110ms;
}
.layout-aside.collapsed :deep(.el-menu-item:nth-child(3) .el-icon) {
  animation-delay: 180ms;
}
.layout-aside.collapsed :deep(.el-menu-item:nth-child(4) .el-icon) {
  animation-delay: 250ms;
}
.layout-aside.collapsed :deep(.el-menu-item:nth-child(5) .el-icon) {
  animation-delay: 320ms;
}
.layout-aside.collapsed :deep(.el-menu-item:nth-child(6) .el-icon) {
  animation-delay: 390ms;
}

@keyframes icon-pop {
  0% {
    opacity: 0;
    transform: translateX(-8px) scale(0.6);
    filter: blur(1px);
  }
  60% {
    opacity: 1;
    transform: translateX(0) scale(1.08);
    filter: blur(0);
  }
  100% {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
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
  background-color: rgba(255, 255, 255, var(--app-content-alpha, 1));
  border-radius: 16px;
  box-shadow: 0 18px 45px rgba(15, 35, 95, 0.08);
  box-sizing: border-box;
  overflow: hidden;
  animation: fade-slide-in 0.4s ease-out;
  backdrop-filter: blur(var(--app-content-blur, 0px));
  -webkit-backdrop-filter: blur(var(--app-content-blur, 0px));
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

@media (max-width: 768px) {
  .layout-header {
    padding: 0 12px;
  }

  .header-left h2 {
    font-size: 14px;
    letter-spacing: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 62vw;
  }

  .user-name {
    display: none;
  }

  .layout-main {
    padding: 10px 10px;
  }

  .page-wrapper {
    max-width: 100%;
    padding: 14px 12px;
    border-radius: 14px;
  }
}
</style>

