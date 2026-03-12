<template>
  <div class="collab-container" v-if="application">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>选题与导师信息</span>
        </div>
      </template>
      <el-descriptions border :column="2" size="small">
        <el-descriptions-item label="选题标题">{{ application.topicTitle }}</el-descriptions-item>
        <el-descriptions-item v-if="identity === 'student'" label="导师姓名">{{ peerName }}</el-descriptions-item>
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
                v-if="identity === 'student'"
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
                  :disabled="!draft.trim()"
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
      class="floating-chat"
      :class="{ 'floating-chat-collapsed': chatCollapsed }"
      :style="{ transform: `translate(${chatPosition.x}px, ${chatPosition.y}px)` }"
    >
      <div class="floating-chat-header" @mousedown="startDrag" @dblclick="toggleCollapsed">
        <span class="floating-chat-title">消息对话</span>
        <div class="floating-chat-tools">
          <el-tag v-if="unreadCount > 0" type="danger" size="small" effect="plain">
            未读 {{ unreadCount }}
          </el-tag>
          <el-button link size="small" @click.stop="emit('refreshMessages')">刷新</el-button>
          <el-button link size="small" @click.stop="toggleCollapsed">
            {{ chatCollapsed ? '展开' : '收起' }}
          </el-button>
        </div>
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
                  :disabled="!draft.trim()"
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

const props = defineProps<{
  identity: 'student' | 'teacher'
  application: Application | null
  messages: Notification[]
}>()

const emit = defineEmits<{
  (e: 'refreshMessages'): void
  (e: 'sendMessage', content: string): void
  (e: 'uploadThesis', application: Application): void
}>()

const viewMode = ref<'split' | 'focus'>('split')
const draft = ref('')
const sending = ref(false)
const chatScrollRef = ref()
const chatCollapsed = ref(false)
const chatPosition = ref({ x: 0, y: 0 })
const thesisHighlight = ref(false)
const templates = [
  '老师您好，本周的进度如下：',
  '我对当前选题有一些疑问，想请教：',
  '已经按要求完成了最新一次修改，请查收。'
]

let dragging = false
let dragStartX = 0
let dragStartY = 0
let pointerStartX = 0
let pointerStartY = 0

const peerName = computed(() => {
  if (!props.application) return ''
  return props.identity === 'student' ? props.application.studentName || '' : props.application.studentName || ''
})

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
  dragging = true
  dragStartX = chatPosition.value.x
  dragStartY = chatPosition.value.y
  pointerStartX = event.clientX
  pointerStartY = event.clientY
  window.addEventListener('mousemove', onDragging)
  window.addEventListener('mouseup', stopDrag)
}

const onDragging = (event: MouseEvent) => {
  if (!dragging) return
  const dx = event.clientX - pointerStartX
  const dy = event.clientY - pointerStartY
  chatPosition.value = {
    x: dragStartX + dx,
    y: dragStartY + dy
  }
}

const stopDrag = () => {
  if (!dragging) return
  dragging = false
  window.removeEventListener('mousemove', onDragging)
  window.removeEventListener('mouseup', stopDrag)
}

onBeforeUnmount(() => {
  stopDrag()
})

const toggleCollapsed = () => {
  chatCollapsed.value = !chatCollapsed.value
}

const useTemplate = (text: string) => {
  draft.value = text
}

const send = async () => {
  if (!draft.value.trim()) return
  sending.value = true
  try {
    emit('sendMessage', draft.value.trim())
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
  position: absolute;
  right: 24px;
  bottom: 24px;
  width: 360px;
  max-width: 90%;
  background-color: #ffffff;
  box-shadow: 0 16px 40px rgba(15, 35, 95, 0.2);
  border-radius: 12px;
  overflow: hidden;
  cursor: default;
  user-select: none;
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

