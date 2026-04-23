<template>
  <div class="collab-container" v-if="application">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>选题与导师信息</span>
          <div class="card-header-extra">
            <slot name="headerExtra" />
          </div>
        </div>
      </template>
      <el-descriptions border :column="2" size="small">
        <el-descriptions-item label="选题标题">{{ application.topicTitle }}</el-descriptions-item>
        <el-descriptions-item v-if="identity === 'student'" label="导师姓名">{{
          application.teacherName || peerName || '—'
        }}</el-descriptions-item>
        <el-descriptions-item v-else label="学生姓名">{{ peerName }}</el-descriptions-item>
        <el-descriptions-item label="申请状态">
          <el-tag v-if="application.status === 'APPROVED'" type="success">已通过</el-tag>
          <el-tag v-else-if="application.status === 'PENDING'" type="warning">待审核</el-tag>
          <el-tag v-else type="danger">已拒绝</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ application.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <div class="collab-view-toggle">
      <el-radio-group v-model="viewMode" size="small">
        <el-radio-button label="split">双栏模式</el-radio-button>
        <el-radio-button label="focus">专注作业</el-radio-button>
      </el-radio-group>
    </div>

    <el-row :gutter="16" class="collab-main-row">
      <el-col :span="viewMode === 'split' ? 12 : 24">
        <el-card :class="['thesis-card', { 'thesis-card-pulse': thesisHighlight }]">
          <template #header>
            <div class="card-header">
              <span>作业 / 论文</span>
              <el-button
                v-if="identity === 'student' && showThesisUploadButton"
                type="primary"
                size="small"
                @click="handleUploadClick"
              >
                上传作业
              </el-button>
            </div>
          </template>
          <slot name="thesis"></slot>
        </el-card>
      </el-col>
      <el-col v-if="viewMode === 'split'" :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>消息对话</span>
              <div class="chat-header-extra">
                <el-tag v-if="unreadCount > 0" type="danger" size="small" effect="plain">
                  未读 {{ unreadCount }}
                </el-tag>
                <el-button link size="small" @click="emit('refreshMessages')">刷新</el-button>
              </div>
            </div>
          </template>
          <div class="chat-box">
            <el-scrollbar class="chat-list" ref="chatScrollRef">
              <transition-group name="chat-fade" tag="div">
                <div
                  v-for="msg in messages"
                  :key="msg.id"
                  class="chat-item"
                  :class="{ 'chat-item-unread': msg.isRead === 0 }"
                >
                  <div class="chat-meta">
                    <span class="chat-title">
                      {{ msg.title }}
                      <el-tag v-if="msg.isRead === 0" size="small" type="warning" effect="plain" class="chat-tag">
                        未读
                      </el-tag>
                    </span>
                    <span class="chat-time">{{ msg.createdAt }}</span>
                  </div>
                  <div class="chat-content">
                    {{ msg.content }}
                  </div>
                </div>
              </transition-group>
              <div v-if="!messages.length" class="chat-empty">暂无消息，去和对方打个招呼吧～</div>
            </el-scrollbar>
            <div class="chat-input">
              <div v-if="!chatStage" class="chat-pick-stage">请先在左侧进度表中点击一行，选择要对话的环节</div>
              <div class="chat-templates">
                <span class="chat-templates-label">快捷短语：</span>
                <el-tag
                  v-for="tpl in templates"
                  :key="tpl"
                  size="small"
                  effect="plain"
                  class="chat-template-tag"
                  @click="useTemplate(tpl)"
                >
                  {{ tpl }}
                </el-tag>
              </div>
              <el-input
                v-model="draft"
                type="textarea"
                :rows="3"
                maxlength="200"
                show-word-limit
                placeholder="输入要发送的消息..."
              />
              <div class="chat-actions">
                <span class="chat-length-hint">已输入 {{ draftLength }} / 200 字</span>
                <el-button
                  type="primary"
                  size="small"
                  @click="send"
                  :loading="sending"
                  :disabled="sendDisabled"
                >
                  发送
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 悬浮可拖拽消息面板：仅在“专注作业”模式下显示 -->
    <div
      v-if="viewMode === 'focus'"
      ref="floatingChatRef"
      class="floating-chat"
      :class="{ 'floating-chat-collapsed': chatCollapsed }"
      :style="{ transform: `translate(${chatPosition.x}px, ${chatPosition.y}px)` }"
    >
      <div
        class="floating-chat-header"
        @mousedown="startDrag"
        @dblclick="toggleCollapsed"
        @click="onFloatingHeaderClick"
      >
        <template v-if="chatCollapsed">
          <div class="bubble">
            <el-icon class="bubble-icon"><ChatDotRound /></el-icon>
            <span v-if="unreadCount > 0" class="bubble-badge">{{ unreadCount }}</span>
          </div>
        </template>
        <template v-else>
          <span class="floating-chat-title">消息对话</span>
          <div class="floating-chat-tools">
            <el-tag v-if="unreadCount > 0" type="danger" size="small" effect="plain">
              未读 {{ unreadCount }}
            </el-tag>
            <el-button link size="small" @click.stop="emit('refreshMessages')">刷新</el-button>
            <el-button link size="small" @click.stop="toggleCollapsed">收起</el-button>
          </div>
        </template>
      </div>
      <transition name="floating-body">
        <div v-show="!chatCollapsed" class="floating-chat-body">
          <div class="chat-box chat-box-floating">
            <el-scrollbar class="chat-list" ref="chatScrollRef">
              <transition-group name="chat-fade" tag="div">
                <div
                  v-for="msg in messages"
                  :key="msg.id"
                  class="chat-item"
                  :class="{ 'chat-item-unread': msg.isRead === 0 }"
                >
                  <div class="chat-meta">
                    <span class="chat-title">
                      {{ msg.title }}
                      <el-tag v-if="msg.isRead === 0" size="small" type="warning" effect="plain" class="chat-tag">
                        未读
                      </el-tag>
                    </span>
                    <span class="chat-time">{{ msg.createdAt }}</span>
                  </div>
                  <div class="chat-content">
                    {{ msg.content }}
                  </div>
                </div>
              </transition-group>
              <div v-if="!messages.length" class="chat-empty">暂无消息，去和对方打个招呼吧～</div>
            </el-scrollbar>
            <div class="chat-input">
              <div v-if="!chatStage" class="chat-pick-stage">请先在进度表中点击一行选择环节</div>
              <div class="chat-templates">
                <span class="chat-templates-label">快捷短语：</span>
                <el-tag
                  v-for="tpl in templates"
                  :key="tpl"
                  size="small"
                  effect="plain"
                  class="chat-template-tag"
                  @click="useTemplate(tpl)"
                >
                  {{ tpl }}
                </el-tag>
              </div>
              <el-input
                v-model="draft"
                type="textarea"
                :rows="3"
                maxlength="200"
                show-word-limit
                placeholder="输入要发送的消息..."
              />
              <div class="chat-actions">
                <span class="chat-length-hint">已输入 {{ draftLength }} / 200 字</span>
                <el-button
                  type="primary"
                  size="small"
                  @click="send"
                  :loading="sending"
                  :disabled="sendDisabled"
                >
                  发送
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </div>
  </div>
  <div v-else class="collab-empty">
    暂无已确认的选题，确认选题后可在此与导师/学生协作。
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import type { Application } from '@/api/application'
import type { Notification } from '@/api/notification'
import { ChatDotRound } from '@element-plus/icons-vue'

const props = withDefaults(
  defineProps<{
    identity: 'student' | 'teacher'
    application: Application | null
    messages: Notification[]
    /** 与某毕设环节绑定的会话；未选环节时不允许发送 */
    chatStage?: string | null
    /** 为 false 时隐藏卡片标题栏的「上传作业」（由流程表逐行上传） */
    showThesisUploadButton?: boolean
  }>(),
  {
    chatStage: undefined,
    showThesisUploadButton: true
  }
)

const emit = defineEmits<{
  (e: 'refreshMessages'): void
  (e: 'sendMessage', content: string, collabStage: string | null | undefined): void
  (e: 'uploadThesis', application: Application): void
}>()

const viewMode = ref<'split' | 'focus'>('split')
const draft = ref('')
const sending = ref(false)
const chatScrollRef = ref()
const chatCollapsed = ref(false)
const chatPosition = ref({ x: 0, y: 0 })
const floatingChatRef = ref<HTMLDivElement | null>(null)
const thesisHighlight = ref(false)
const templates = [
  '老师您好，本周的进度如下：',
  '我对当前选题有一些疑问，想请教：',
  '已经按要求完成了最新一次修改，请查收。'
]

let dragging = false
let suppressHeaderClickUntil = 0
let didDrag = false
let dragStartX = 0
let dragStartY = 0
let pointerStartX = 0
let pointerStartY = 0

const peerName = computed(() => {
  if (!props.application) return ''
  return props.identity === 'student'
    ? props.application.teacherName || ''
    : props.application.studentName || ''
})

const sendDisabled = computed(() => !draft.value.trim() || !props.chatStage)

watch(
  () => props.application,
  () => {
    draft.value = ''
  }
)

const draftLength = computed(() => draft.value.trim().length)

const scrollToBottom = () => {
  nextTick(() => {
    const wrap = (chatScrollRef.value as any)?.wrapRef as HTMLElement | undefined
    if (wrap) {
      wrap.scrollTop = wrap.scrollHeight
    }
  })
}

watch(
  () => props.messages,
  () => {
    scrollToBottom()
  },
  { deep: true }
)

const unreadCount = computed(() => props.messages.filter((m) => m.isRead === 0).length)

const handleUploadClick = () => {
  if (props.application) {
    emit('uploadThesis', props.application)
    thesisHighlight.value = true
    setTimeout(() => {
      thesisHighlight.value = false
    }, 600)
  }
}

const startDrag = (event: MouseEvent) => {
  if (viewMode.value !== 'focus') return
  event.preventDefault()
  event.stopPropagation()
  dragging = true
  didDrag = false
  // 防止拖动结束后触发 header 的 click 事件，导致误触发收起/展开
  suppressHeaderClickUntil = Date.now() + 800
  dragStartX = chatPosition.value.x
  dragStartY = chatPosition.value.y
  pointerStartX = event.clientX
  pointerStartY = event.clientY
  window.addEventListener('mousemove', onDragging)
  window.addEventListener('mouseup', stopDrag)
}

const getFloatingBounds = () => {
  const el = floatingChatRef.value

  const w = chatCollapsed.value ? 56 : (el?.offsetWidth ?? 360)
  const h = chatCollapsed.value ? 56 : (el?.offsetHeight ?? 260)
  const baseLeft = 24
  const baseTop = 120
  const pad = 8
  const maxX = Math.max(0, window.innerWidth - w - baseLeft - pad)
  const maxY = Math.max(0, window.innerHeight - h - baseTop - pad)
  return { w, h, maxX, maxY }
}

const clampPosition = (pos: { x: number; y: number }) => {
  const { maxX, maxY } = getFloatingBounds()
  return {
    x: Math.min(Math.max(0, pos.x), maxX),
    y: Math.min(Math.max(0, pos.y), maxY)
  }
}

/**
 * 贴边“磁吸”：只有靠近边缘才吸附
 * - 避免“松手必回左边”的强制行为
 * - 当视窗较窄导致 maxX=0 时，也不会产生奇怪的跳动
 */
const snapIfNearEdge = (thresholdPx = 32) => {
  const { w, maxX } = getFloatingBounds()
  const cur = clampPosition(chatPosition.value)

  // “露头”吸附：仅在收起态生效，贴边后把大部分隐藏到屏幕外
  const peekPx = 18
  const hidden = Math.max(0, w - peekPx)

  if (cur.x <= thresholdPx) {
    chatPosition.value = chatCollapsed.value ? { x: -hidden, y: cur.y } : { x: 0, y: cur.y }
    return
  }
  if (maxX - cur.x <= thresholdPx) {
    chatPosition.value = chatCollapsed.value ? { x: maxX + hidden, y: cur.y } : { x: maxX, y: cur.y }
  }
}

const onDragging = (event: MouseEvent) => {
  if (!dragging) return
  const dx = event.clientX - pointerStartX
  const dy = event.clientY - pointerStartY
  const { maxX, maxY } = getFloatingBounds()

  const nextX = dragStartX + dx
  const nextY = dragStartY + dy
  if (!didDrag && Math.abs(dx) + Math.abs(dy) > 3) {
    didDrag = true
    suppressHeaderClickUntil = Date.now() + 1200
  }

  chatPosition.value = {
    x: Math.min(Math.max(0, nextX), maxX),
    y: Math.min(Math.max(0, nextY), maxY)
  }
}

const stopDrag = () => {
  if (!dragging) return
  dragging = false
  window.removeEventListener('mousemove', onDragging)
  window.removeEventListener('mouseup', stopDrag)
  // mouseup 后浏览器仍可能派发 click，额外延长抑制窗口
  suppressHeaderClickUntil = Date.now() + 1200
  snapIfNearEdge()
}

onBeforeUnmount(() => {
  stopDrag()
})

const toggleCollapsed = () => {
  chatCollapsed.value = !chatCollapsed.value
  // 收起/展开会改变宽高，重新夹紧并贴边，避免越界或半遮挡
  requestAnimationFrame(() => {
    // 展开态必须完全可见；收起态允许“露头”（x 可为负或超出 maxX）
    if (!chatCollapsed.value) {
      chatPosition.value = clampPosition(chatPosition.value)
    }
    snapIfNearEdge()
  })
}

const onFloatingHeaderClick = () => {
  if (Date.now() < suppressHeaderClickUntil) return
  if (didDrag) return
  // 仅在「收起态」点击时才展开；展开态点击不做任何事（避免误触）
  if (chatCollapsed.value) {
    toggleCollapsed()
  }
}

const useTemplate = (text: string) => {
  draft.value = text
}

const send = async () => {
  if (!draft.value.trim() || !props.chatStage) return
  sending.value = true
  try {
    emit('sendMessage', draft.value.trim(), props.chatStage)
    draft.value = ''
  } finally {
    sending.value = false
    emit('refreshMessages')
    scrollToBottom()
  }
}
</script>

<style scoped>
.collab-container {
  max-width: 1400px;
  margin: 0 auto;
  position: relative;
}

.collab-main-row {
  margin-top: 16px;
}

.collab-view-toggle {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.collab-empty {
  text-align: center;
  color: #999;
  padding: 40px 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header-extra {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-header-extra {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-box {
  display: flex;
  flex-direction: column;
  height: 360px;
}

.chat-list {
  flex: 1;
  padding-right: 4px;
  padding-left: 12px;
  position: relative;
}

.chat-item {
  position: relative;
  padding: 6px 4px 6px 18px;
  border-left: 2px solid #e4e7ed;
  margin-left: 4px;
  margin-bottom: 4px;
  border-bottom: 1px solid #f5f5f5;
  transition: background-color 0.2s ease;
}

.chat-item::before {
  content: '';
  position: absolute;
  left: -6px;
  top: 10px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #c0c4cc;
}

.chat-item-unread {
  background-color: #fff7e6;
}

.chat-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.chat-title {
  font-weight: 500;
}

.chat-tag {
  margin-left: 4px;
}

.chat-content {
  margin-top: 2px;
  font-size: 13px;
}

.chat-empty {
  text-align: center;
  color: #c0c4cc;
  padding: 16px 0;
}

.chat-input {
  margin-top: 8px;
}

.chat-templates {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 4px;
}

.chat-templates-label {
  font-size: 12px;
  color: #909399;
}

.chat-pick-stage {
  font-size: 12px;
  color: #e6a23c;
  margin-bottom: 6px;
}

.chat-template-tag {
  cursor: pointer;
}

.chat-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 4px;
}

.chat-length-hint {
  font-size: 12px;
  color: #909399;
}

.thesis-card {
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.thesis-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 28px rgba(15, 35, 95, 0.08);
}

.thesis-card-pulse {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.4);
}

.floating-chat {
  position: fixed;
  left: 24px;
  top: 120px;
  width: 360px;
  max-width: 90%;
  background-color: #ffffff;
  box-shadow: 0 16px 40px rgba(15, 35, 95, 0.2);
  border-radius: 12px;
  overflow: hidden;
  cursor: default;
  user-select: none;
  /* 保证在各种弹窗/抽屉之上（ElementPlus 默认 dialog 在 2000 左右） */
  z-index: 3200;
  transition: width 0.18s ease, height 0.18s ease, border-radius 0.18s ease, box-shadow 0.18s ease;
}

.floating-chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  background: linear-gradient(90deg, #409eff, #66b1ff);
  color: #fff;
  cursor: move;
}

.floating-chat-title {
  font-size: 13px;
  font-weight: 500;
}

.floating-chat-tools {
  display: flex;
  align-items: center;
  gap: 4px;
}

.floating-chat-body {
  padding: 6px 8px 10px;
}

.chat-box-floating {
  height: 260px;
}

.floating-chat-collapsed {
  width: 56px;
  height: 56px;
  border-radius: 999px;
  box-shadow: 0 14px 32px rgba(15, 35, 95, 0.22);
}

.floating-chat-collapsed .floating-chat-header {
  padding: 0;
  width: 56px;
  height: 56px;
  justify-content: center;
  cursor: pointer;
}

.bubble {
  position: relative;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bubble-icon {
  font-size: 22px;
}

.bubble-badge {
  position: absolute;
  right: 6px;
  top: 6px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #f56c6c;
  color: #fff;
  font-size: 12px;
  line-height: 18px;
  text-align: center;
  box-shadow: 0 6px 14px rgba(245, 108, 108, 0.35);
}

.floating-chat-collapsed .floating-chat-body {
  display: none;
}

.chat-fade-enter-active,
.chat-fade-leave-active {
  transition: all 0.18s ease;
}

.chat-fade-enter-from,
.chat-fade-leave-to {
  opacity: 0;
  transform: translateY(4px);
}

.floating-body-enter-active,
.floating-body-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.floating-body-enter-from,
.floating-body-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>

