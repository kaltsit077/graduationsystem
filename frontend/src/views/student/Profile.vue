<template>
  <div class="profile-container">
    <el-card class="bg-card">
      <template #header>
        <div class="card-header">
          <span>个性化背景</span>
          <div class="bg-actions">
            <el-button size="small" @click="resetBackground" :disabled="!authStore.backgroundUrl" :loading="bgLoading">
              恢复默认
            </el-button>
            <el-upload
              :show-file-list="false"
              :before-upload="beforeUpload"
              :http-request="doUpload"
              accept="image/*"
            >
              <el-button size="small" type="primary" :loading="bgLoading">上传背景</el-button>
            </el-upload>
          </div>
        </div>
      </template>
      <div class="bg-preview" :class="{ empty: !authStore.backgroundUrl }">
        <div
          v-if="authStore.backgroundUrl"
          class="bg-preview-img"
          :style="bgPreviewStyle"
          @mousedown="onPreviewMouseDown"
        />
        <div v-else class="bg-preview-empty">当前使用默认背景。你可以上传一张图片作为系统背景。</div>
      </div>
      <div class="bg-controls" v-if="authStore.backgroundUrl">
        <div class="bg-control">
          <div class="bg-control-label">
            <span>背景缩放</span>
            <span class="bg-control-value">{{ authStore.backgroundScale }}%</span>
          </div>
          <el-slider
            v-model="authStore.backgroundScale"
            :min="50"
            :max="200"
            :step="1"
            :disabled="appearanceSaving"
            @change="persistAppearance({ backgroundScale: authStore.backgroundScale })"
          />
        </div>

        <div class="bg-control">
          <div class="bg-control-label">
            <span>背景遮罩</span>
            <span class="bg-control-value">{{ Math.round(authStore.bgOverlayAlpha * 100) }}%</span>
          </div>
          <el-slider
            v-model="authStore.bgOverlayAlpha"
            :min="0"
            :max="1"
            :step="0.01"
            :disabled="appearanceSaving"
            @change="persistAppearance({ bgOverlayAlpha: authStore.bgOverlayAlpha })"
          />
        </div>

        <div class="bg-control">
          <div class="bg-control-label">
            <span>内容透明度</span>
            <span class="bg-control-value">{{ Math.round(authStore.contentAlpha * 100) }}%</span>
          </div>
          <el-slider
            v-model="authStore.contentAlpha"
            :min="0.4"
            :max="1"
            :step="0.01"
            :disabled="appearanceSaving"
            @change="persistAppearance({ contentAlpha: authStore.contentAlpha })"
          />
        </div>

        <div class="bg-control">
          <div class="bg-control-label">
            <span>毛玻璃</span>
            <span class="bg-control-value">{{ authStore.contentBlur }}px</span>
          </div>
          <el-slider
            v-model="authStore.contentBlur"
            :min="0"
            :max="24"
            :step="1"
            :disabled="appearanceSaving"
            @change="persistAppearance({ contentBlur: authStore.contentBlur })"
          />
        </div>

        <div class="bg-drag-hint">提示：在上方预览图里按住拖拽可移动背景位置</div>
      </div>
      <div class="bg-hint">
        支持 jpg/jpeg/png/webp/gif，建议 1920×1080 以上，大小不超过 5MB。
      </div>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
        </div>
      </template>

      <el-form :model="form" label-width="120px" style="max-width: 600px">
        <el-form-item label="学号">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="当前密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onChangePasswordClick" :loading="changingPassword">
            修改密码
          </el-button>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="form.major" />
        </el-form-item>
        <el-form-item label="主修科目">
          <el-input
            v-model="form.majorCourses"
            type="textarea"
            :rows="3"
            placeholder="请填写已修课程/主修课程（用逗号分隔），如：数据结构，数据库，Python，机器学习"
          />
        </el-form-item>
        <el-form-item label="年级">
          <el-input v-model="form.grade" />
        </el-form-item>
        <el-form-item label="兴趣描述">
          <el-input
            v-model="form.interestDesc"
            type="textarea"
            :rows="4"
            placeholder="请描述您的兴趣爱好和研究方向"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSaveProfileClick" :loading="saving">
            {{ saveSuccess ? '已保存 ✓' : '保存' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header tags-header">
          <span>我的标签</span>
          <div class="tags-header-controls">
            <div class="tags-header-left">
              <el-radio-group v-model="form.tagMode" size="small">
                <el-radio-button label="BOTH">综合</el-radio-button>
                <el-radio-button label="MAJOR">仅专业</el-radio-button>
                <el-radio-button label="INTEREST">仅兴趣</el-radio-button>
              </el-radio-group>
              <div class="tags-count-wrap">
                <span class="tags-count-label">本次新增数量</span>
                <el-input-number
                  v-model="desiredTagCount"
                  size="small"
                  :min="1"
                  :max="8"
                  :step="1"
                  controls-position="right"
                />
              </div>
            </div>
            <div class="tags-header-right">
            <el-button
              type="primary"
              size="small"
              @click="generateTags"
              :loading="generating"
              :disabled="generating || rerolling || savingTags || selectMode"
            >
              生成我的标签
            </el-button>
            <el-button
              size="small"
              @click="createTag"
              :disabled="generating || rerolling || savingTags"
            >
              新增标签
            </el-button>
            <el-button size="small" type="success" @click="saveTags" :loading="savingTags" :disabled="generating || rerolling">
              保存标签
            </el-button>

            <el-button
              size="small"
              @click="enterSelectMode('reroll')"
              :disabled="generating || rerolling || savingTags"
              v-if="!selectMode"
            >
              选择重新生成
            </el-button>
            <el-button
              size="small"
              type="danger"
              @click="enterSelectMode('delete')"
              :disabled="generating || rerolling || savingTags"
              v-if="!selectMode"
            >
              选择删除
            </el-button>

            <el-button
              size="small"
              type="primary"
              @click="confirmSelectionAction"
              :disabled="selectedNames.length === 0 || generating || rerolling"
              v-if="selectMode"
            >
              确认{{ selectAction === 'delete' ? '删除' : '重新生成' }}（{{ selectedNames.length }}）
            </el-button>
            <el-button size="small" @click="exitSelectMode" :disabled="generating || rerolling" v-if="selectMode">
              取消选择
            </el-button>
            </div>
          </div>
        </div>
      </template>
      <div v-if="generating || rerolling" style="margin-bottom: 12px">
        <div style="display: flex; justify-content: space-between; color: #666; margin-bottom: 6px">
          <span>您的专属标签正在生成中…</span>
          <span>{{ progress }}%</span>
        </div>
        <el-progress :percentage="progress" :stroke-width="10" status="success" />
        <div v-if="progressText" class="progress-text">
          {{ progressText }}
        </div>
      </div>

      <div v-if="tags.length === 0" style="text-align: center; color: #999; padding: 20px">
        暂无标签，请点击“生成我的标签”
      </div>

      <div v-else>
        <div v-if="selectMode" style="color: #666; margin-bottom: 10px">
          {{
            selectAction === 'delete'
              ? '请选择要删除的标签，然后点击右上角确认删除。'
              : '请选择要重新生成的标签，然后点击右上角确认重新生成。'
          }}
        </div>

        <TransitionGroup name="tag-card-fade" tag="div" class="tag-grid">
          <div
            v-for="tag in tags"
            :key="tag.tagName"
            class="tag-card"
            :class="{ selected: !!selectedMap[tag.tagName], selectable: selectMode }"
            @click="onTagClick(tag)"
          >
            <div class="tag-card-top">
              <el-tag :type="tagColorType(tag)" effect="light">{{ tag.tagName }}</el-tag>
              <span class="tag-weight">权重: {{ tag.weight }}</span>
            </div>
            <div class="tag-card-bottom" v-if="selectMode">
              <span class="tag-select-hint">{{ selectedMap[tag.tagName] ? '已选择' : '点击选择' }}</span>
            </div>
          </div>
        </TransitionGroup>
      </div>
    </el-card>

    <AppDialog
      v-model="showEditDialog"
      title="编辑标签"
      width="520px"
      :close-on-click-modal="false"
    >
      <div v-if="activeTag">
        <div class="edit-hint">{{ editHint }}</div>
        <el-form label-width="90px">
          <el-form-item label="标签名称">
            <el-input
              v-model="editingValue"
              ref="editInputRef"
              class="tag-edit-input"
              :prefix-icon="EditPen"
              placeholder="请输入标签名称"
              @keyup.enter="activeTag && commitEdit(activeTag)"
            />
          </el-form-item>
          <el-form-item label="权重">
            <div style="width: 100%">
              <el-slider v-model="editingWeight" :min="0" :max="1" :step="0.05" show-input />
              <div style="margin-top: 6px; color: #888; font-size: 12px">
                权重越高，表示该标签在匹配排序中越重要。若“喜欢但不熟练”，可适当降低权重。
              </div>
            </div>
          </el-form-item>
          <el-form-item label="标签类型">
            <el-radio-group v-model="editingTagType">
              <el-radio-button label="MAJOR">专业</el-radio-button>
              <el-radio-button label="INTEREST">兴趣</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showEditDialog = false">关闭</el-button>
        <el-button type="danger" @click="deleteOne(activeTag!)" :disabled="generating || rerolling || !activeTag">删除</el-button>
        <el-button
          @click="rerollOne(activeTag!)"
          :disabled="generating || rerolling || !activeTag"
          :loading="rerollingOne === activeTag?.tagName"
        >
          重新生成此标签
        </el-button>
        <el-button type="primary" @click="commitEdit(activeTag!)" :disabled="!activeTag">保存修改</el-button>
      </template>
    </AppDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { EditPen } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import AppDialog from '@/components/AppDialog.vue'
import { useAuthStore } from '@/stores/auth'
import { resetMyBackground, uploadMyBackground, updateMyAppearance } from '@/api/userPreference'
import {
  getStudentProfile,
  updateStudentProfile,
  regenerateStudentTags,
  updateStudentTags,
  changeStudentPassword,
  type UserTag,
  type StudentProfileResponse
} from '@/api/student'

const authStore = useAuthStore()

const bgLoading = ref(false)
const appearanceSaving = ref(false)
let appearanceDebounceTimer: any = null

const bgPreviewStyle = computed(() => {
  if (!authStore.backgroundUrl) return {}
  const overlay = Number.isFinite(authStore.bgOverlayAlpha) ? authStore.bgOverlayAlpha : 0.78
  const scale = Number.isFinite(authStore.backgroundScale) ? authStore.backgroundScale : 100
  const posX = Number.isFinite(authStore.backgroundPosX) ? authStore.backgroundPosX : 50
  const posY = Number.isFinite(authStore.backgroundPosY) ? authStore.backgroundPosY : 50
  return {
    backgroundImage: `linear-gradient(135deg, rgba(245, 247, 250, ${overlay}) 0%, rgba(238, 243, 251, ${overlay}) 50%, rgba(245, 247, 250, ${overlay}) 100%), url(${authStore.backgroundUrl})`,
    backgroundSize: `${scale}%`,
    backgroundPosition: `${posX}% ${posY}%`,
    backgroundRepeat: 'no-repeat'
  } as Record<string, string>
})

const persistAppearance = (patch: Record<string, any>, debounceMs = 0) => {
  authStore.setAppearance(patch)
  if (appearanceDebounceTimer) {
    clearTimeout(appearanceDebounceTimer)
    appearanceDebounceTimer = null
  }
  const run = async () => {
    appearanceSaving.value = true
    try {
      const res = await updateMyAppearance(patch)
      authStore.setBackgroundUrl(res.data?.backgroundUrl || null)
      authStore.setAppearance({
        backgroundScale: res.data?.backgroundScale,
        backgroundPosX: res.data?.backgroundPosX,
        backgroundPosY: res.data?.backgroundPosY,
        bgOverlayAlpha: res.data?.bgOverlayAlpha,
        contentAlpha: res.data?.contentAlpha,
        contentBlur: res.data?.contentBlur
      })
    } catch {
      // ignore
    } finally {
      appearanceSaving.value = false
    }
  }
  if (debounceMs > 0) {
    appearanceDebounceTimer = setTimeout(run, debounceMs)
  } else {
    void run()
  }
}

const beforeUpload = (file: File) => {
  const isImg = file.type.startsWith('image/')
  if (!isImg) {
    ElMessage.error('请选择图片文件')
    return false
  }
  const ok = file.size <= 5 * 1024 * 1024
  if (!ok) {
    ElMessage.error('图片过大，请上传 5MB 以内的图片')
    return false
  }
  return true
}

const doUpload = async (opts: any) => {
  const file = opts?.file as File
  if (!file) return
  bgLoading.value = true
  try {
    const res = await uploadMyBackground(file)
    authStore.setBackgroundUrl(res.data?.backgroundUrl || null)
    authStore.setAppearance({
      backgroundScale: res.data?.backgroundScale ?? 100,
      backgroundPosX: res.data?.backgroundPosX ?? 50,
      backgroundPosY: res.data?.backgroundPosY ?? 50,
      bgOverlayAlpha: res.data?.bgOverlayAlpha ?? 0.78,
      contentAlpha: res.data?.contentAlpha ?? 1.0,
      contentBlur: res.data?.contentBlur ?? 0
    })
    ElMessage.success('背景已更新')
  } catch {
    // message 已由拦截器处理
  } finally {
    bgLoading.value = false
  }
}

const resetBackground = async () => {
  bgLoading.value = true
  try {
    const res = await resetMyBackground()
    authStore.setBackgroundUrl(null)
    authStore.setAppearance({
      backgroundScale: res.data?.backgroundScale ?? 100,
      backgroundPosX: res.data?.backgroundPosX ?? 50,
      backgroundPosY: res.data?.backgroundPosY ?? 50,
      bgOverlayAlpha: res.data?.bgOverlayAlpha ?? 0.78,
      contentAlpha: res.data?.contentAlpha ?? 1.0,
      contentBlur: res.data?.contentBlur ?? 0
    })
    ElMessage.success('已恢复默认背景')
  } catch {
    // ignore
  } finally {
    bgLoading.value = false
  }
}

// ---------- Preview drag to adjust background position ----------
const dragState = ref<{
  dragging: boolean
  startX: number
  startY: number
  startPosX: number
  startPosY: number
  boxW: number
  boxH: number
} | null>(null)

const onPreviewMouseDown = (e: MouseEvent) => {
  if (!authStore.backgroundUrl) return
  const el = e.currentTarget as HTMLElement | null
  if (!el) return
  const rect = el.getBoundingClientRect()
  dragState.value = {
    dragging: true,
    startX: e.clientX,
    startY: e.clientY,
    startPosX: authStore.backgroundPosX,
    startPosY: authStore.backgroundPosY,
    boxW: rect.width,
    boxH: rect.height
  }
  window.addEventListener('mousemove', onPreviewMouseMove, { passive: true })
  window.addEventListener('mouseup', onPreviewMouseUp, { passive: true })
}

const onPreviewMouseMove = (e: MouseEvent) => {
  const s = dragState.value
  if (!s?.dragging) return
  const dx = e.clientX - s.startX
  const dy = e.clientY - s.startY
  // 经验映射：拖动 1 个预览宽度 -> 位置变化约 100%
  const nextX = Math.max(0, Math.min(100, Math.round(s.startPosX - (dx / Math.max(1, s.boxW)) * 100)))
  const nextY = Math.max(0, Math.min(100, Math.round(s.startPosY - (dy / Math.max(1, s.boxH)) * 100)))
  authStore.setAppearance({ backgroundPosX: nextX, backgroundPosY: nextY })
}

const onPreviewMouseUp = () => {
  const s = dragState.value
  if (!s) return
  dragState.value = { ...s, dragging: false }
  window.removeEventListener('mousemove', onPreviewMouseMove as any)
  window.removeEventListener('mouseup', onPreviewMouseUp as any)
  persistAppearance({ backgroundPosX: authStore.backgroundPosX, backgroundPosY: authStore.backgroundPosY }, 150)
}

const form = ref({
  username: '',
  realName: authStore.realName || '',
  major: '',
  majorCourses: '',
  grade: '',
  interestDesc: '',
  tagMode: 'BOTH' as 'MAJOR' | 'INTEREST' | 'BOTH'
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const changingPassword = ref(false)

const tags = ref<UserTag[]>([])
const saving = ref(false)
const generating = ref(false)
const rerolling = ref(false)
const rerollingOne = ref<string | null>(null)
const savingTags = ref(false)

const progress = ref(0)
let progressTimer: any = null
const progressText = ref('')
const progressMessages = [
  'AI 正在为你阅读兴趣描述…',
  '正在匹配与你最相近的方向…',
  '快好了，马上生成专属标签～'
]
let progressMessageTimer: any = null

const selectedMap = ref<Record<string, boolean>>({})
const selectedNames = ref<string[]>([])

const showEditDialog = ref(false)
const activeTag = ref<UserTag | null>(null)
const editingValue = ref('')
const editingWeight = ref<number>(0.9)
const editingTagType = ref<'MAJOR' | 'INTEREST'>('INTEREST')

const selectMode = ref(false)
const selectAction = ref<'delete' | 'reroll'>('delete')

const saveSuccess = ref(false)

// 期望“本次新增”的标签数量（1-8），默认 3
const desiredTagCount = ref(3)

const maxTotalTags = 9

const editInputRef = ref()
const editHint = ref('点击保存后会更新到“我的标签”。想批量删除/重生成，请使用右上角“选择删除 / 选择重新生成”。')
const editHints = [
  '可以给标签起一个更贴近你专业/兴趣的名字。',
  '尽量用简短的名词短语，比如“数据治理”“计算机视觉”。',
  '修改这里只会影响你自己的标签，不会影响推荐模型。',
  '想一次处理多个标签，可以用右上角的“选择删除 / 选择重新生成”。'
]

onMounted(() => {
  loadProfile()
})

const loadProfile = async () => {
  try {
    const res = await getStudentProfile()
    if (res.data) {
      form.value.username = res.data.username || form.value.username
      form.value.realName = res.data.realName || form.value.realName
      form.value.major = res.data.major || ''
      form.value.majorCourses = (res.data as any).majorCourses || ''
      form.value.grade = res.data.grade || ''
      form.value.interestDesc = res.data.interestDesc || ''
      form.value.tagMode = (res.data.tagMode as any) || 'BOTH'
      tags.value = res.data.tags || []
      rebuildSelectedMap()
    }
  } catch (error) {
    ElMessage.error('加载信息失败')
  }
}

const rebuildSelectedMap = () => {
  const map: Record<string, boolean> = {}
  for (const t of tags.value) {
    map[t.tagName] = selectedMap.value[t.tagName] || false
  }
  selectedMap.value = map
  selectedNames.value = Object.keys(map).filter((k) => map[k])
}

const isInterestTag = (tag: UserTag) => {
  // 优先使用后端返回/用户选择的 tagType；缺失时回退到权重推断，兼容旧数据
  const t = (tag as any)?.tagType
  if (t === 'INTEREST') return true
  if (t === 'MAJOR') return false
  return Number(tag.weight) >= 0.85
}

const tagColorType = (tag: UserTag) => {
  // 专业：蓝色 primary；兴趣：黄色 warning
  return isInterestTag(tag) ? 'warning' : 'primary'
}

const onSaveProfileClick = async () => {
  const res = await saveProfile()
  if (res) {
    saveSuccess.value = true
    setTimeout(() => {
      saveSuccess.value = false
    }, 1600)
  }
}

const saveProfile = async (): Promise<StudentProfileResponse | undefined> => {
  saving.value = true
  try {
    const res = await updateStudentProfile({
      realName: form.value.realName,
      major: form.value.major,
      majorCourses: (form.value as any).majorCourses,
      grade: form.value.grade,
      interestDesc: form.value.interestDesc,
      tagMode: form.value.tagMode
    })
    ElMessage.success('保存成功')
    if (form.value.realName) {
      authStore.setRealName(form.value.realName)
    }
    // 保存仅更新基本信息，不再自动刷新标签
    return res.data
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
    throw error
  } finally {
    saving.value = false
  }
}

const onChangePasswordClick = async () => {
  const oldPwd = passwordForm.value.oldPassword.trim()
  const newPwd = passwordForm.value.newPassword.trim()
  const confirmPwd = passwordForm.value.confirmPassword.trim()

  if (!oldPwd || !newPwd || !confirmPwd) {
    ElMessage.warning('请完整填写当前密码和新密码')
    return
  }
  if (newPwd.length < 6) {
    ElMessage.warning('新密码长度至少为 6 位')
    return
  }
  if (newPwd !== confirmPwd) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  changingPassword.value = true
  try {
    await changeStudentPassword(oldPwd, newPwd)
    ElMessage.success('密码修改成功，请使用新密码登录')
    passwordForm.value.oldPassword = ''
    passwordForm.value.newPassword = ''
    passwordForm.value.confirmPassword = ''
  } catch (error) {
    // 具体错误提示已在拦截器里处理
  } finally {
    changingPassword.value = false
  }
}

const startProgress = () => {
  progress.value = 5
  if (progressTimer) clearInterval(progressTimer)
  if (progressMessageTimer) clearInterval(progressMessageTimer)
  let idx = 0
  progressText.value = progressMessages[idx]
  progressMessageTimer = setInterval(() => {
    idx = (idx + 1) % progressMessages.length
    progressText.value = progressMessages[idx]
  }, 1600)
  progressTimer = setInterval(() => {
    // 缓慢逼近 90%，等待接口返回后再拉到 100%
    if (progress.value < 90) progress.value += Math.max(1, Math.floor((90 - progress.value) / 12))
  }, 500)
}

const stopProgress = (success: boolean) => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
  if (progressMessageTimer) {
    clearInterval(progressMessageTimer)
    progressMessageTimer = null
  }
  if (success) {
    progress.value = 100
    setTimeout(() => (progress.value = 0), 600)
  } else {
    progress.value = 0
  }
  progressText.value = ''
}

const validateTagInputsBeforeGenerate = () => {
  const mode = (form.value.tagMode || 'BOTH') as 'MAJOR' | 'INTEREST' | 'BOTH'
  const interest = (form.value.interestDesc || '').trim()
  const major = (form.value.major || '').trim()
  const courses = ((form.value as any).majorCourses || '').trim()

  if (mode === 'MAJOR') {
    if (!major && !courses) {
      ElMessage.warning('请先填写专业或主修科目，再按“按专业生成”')
      return false
    }
    return true
  }
  if (mode === 'INTEREST') {
    if (!interest) {
      ElMessage.warning('请先填写兴趣描述，再按“按兴趣生成”')
      return false
    }
    return true
  }
  // BOTH
  if (!interest && !major && !courses) {
    ElMessage.warning('请先至少填写兴趣描述、专业或主修科目，再进行综合生成')
    return false
  }
  return true
}

const generateTags = async () => {
  if (generating.value || rerolling.value) return
  if (!validateTagInputsBeforeGenerate()) return

  const currentCount = tags.value.length
  if (currentCount >= maxTotalTags) {
    ElMessage.warning(`标签已达到上限 ${maxTotalTags} 个，如需调整请删除部分标签后再生成`)
    return
  }
  const targetTotal = Math.min(maxTotalTags, currentCount + Math.max(1, desiredTagCount.value || 1))

  generating.value = true
  startProgress()
  try {
    await saveProfile()

    const excludeAll = tags.value.map((t) => t.tagName)
    const pinned = tags.value.map((t) => ({ ...t }))
    const res = await regenerateStudentTags({
      interestDesc: form.value.interestDesc,
      major: form.value.major,
      majorCourses: (form.value as any).majorCourses,
      tagMode: form.value.tagMode,
      pinnedTags: pinned,
      excludeTagNames: excludeAll,
      desiredTotal: targetTotal
    })

    const newTags = res.data || []
    tags.value = newTags
    rebuildSelectedMap()

    ElMessage.success('标签生成成功')
    stopProgress(true)
  } catch (error: any) {
    ElMessage.error(error.message || '标签生成失败')
    stopProgress(false)
  } finally {
    generating.value = false
  }
}

const rerollWithPinned = async (pinned: UserTag[], exclude: string[], regeneratedCount: number) => {
  if (!validateTagInputsBeforeGenerate()) return

  const baseCount = pinned.length
  const targetTotal = Math.min(maxTotalTags, Math.max(baseCount, baseCount + regeneratedCount))

  rerolling.value = true
  startProgress()
  try {
    const res = await regenerateStudentTags({
      interestDesc: form.value.interestDesc,
      major: form.value.major,
      majorCourses: (form.value as any).majorCourses,
      tagMode: form.value.tagMode,
      pinnedTags: pinned,
      excludeTagNames: exclude,
      desiredTotal: targetTotal
    })

    const newTags = res.data || []
    tags.value = newTags
    rebuildSelectedMap()
    ElMessage.success('重新生成成功')
    stopProgress(true)
  } catch (error: any) {
    ElMessage.error(error.message || '重新生成失败')
    stopProgress(false)
  } finally {
    rerolling.value = false
  }
}

const rerollOne = async (tag: UserTag) => {
  if (generating.value || rerolling.value) return
  rerollingOne.value = tag.tagName
  try {
    // 固定除当前标签外的其它标签
    const pinned = tags.value.filter((t) => t.tagName !== tag.tagName)
    // 排除当前轮所有标签名，避免重复（固定标签除外后端会自动允许）
    const exclude = tags.value.map((t) => t.tagName)
    await rerollWithPinned(pinned, exclude, 1)
  } finally {
    rerollingOne.value = null
  }
}

const deleteOne = (tag: UserTag) => {
  tags.value = tags.value.filter((t) => t.tagName !== tag.tagName)
  if (activeTag.value?.tagName === tag.tagName) {
    showEditDialog.value = false
    activeTag.value = null
    editingValue.value = ''
  }
  rebuildSelectedMap()
}

const commitEdit = (tag: UserTag) => {
  const newName = editingValue.value.trim()
  if (!newName) {
    ElMessage.error('标签名不能为空')
    return
  }
  if (newName !== tag.tagName && tags.value.some((t) => t.tagName === newName)) {
    ElMessage.error('标签名重复')
    return
  }
  const w = Math.max(0, Math.min(1, Number(editingWeight.value)))
  const tp = (editingTagType.value || (isInterestTag(tag) ? 'INTEREST' : 'MAJOR')) as 'MAJOR' | 'INTEREST'
  tags.value = tags.value.map((t) =>
    t.tagName === tag.tagName ? { ...t, tagName: newName, weight: w, tagType: tp } : t
  )
  showEditDialog.value = false
  activeTag.value = null
  editingValue.value = ''
  editingWeight.value = 0.9
  editingTagType.value = 'INTEREST'
  rebuildSelectedMap()
}

const enterSelectMode = (action: 'delete' | 'reroll') => {
  if (generating.value || rerolling.value) return
  selectMode.value = true
  selectAction.value = action
  selectedMap.value = {}
  rebuildSelectedMap()
}

const exitSelectMode = () => {
  selectMode.value = false
  selectedMap.value = {}
  rebuildSelectedMap()
}

const confirmSelectionAction = async () => {
  if (selectedNames.value.length === 0) return
  if (selectAction.value === 'delete') {
    tags.value = tags.value.filter((t) => !selectedNames.value.includes(t.tagName))
    rebuildSelectedMap()
    exitSelectMode()
    ElMessage.success('已删除选中标签')
    return
  }
  const pinned = tags.value.filter((t) => !selectedNames.value.includes(t.tagName))
  const exclude = tags.value.map((t) => t.tagName)
  // 选中几个就只重生成几个：总数 ≈ 固定数 + 选中数，但不超过上限
  await rerollWithPinned(pinned, exclude, selectedNames.value.length)
  exitSelectMode()
}

const onTagClick = (tag: UserTag) => {
  if (selectMode.value) {
    selectedMap.value = { ...selectedMap.value, [tag.tagName]: !selectedMap.value[tag.tagName] }
    selectedNames.value = Object.keys(selectedMap.value).filter((k) => selectedMap.value[k])
    return
  }
  activeTag.value = tag
  editingValue.value = tag.tagName
  editingWeight.value = Number(tag.weight ?? 0.9)
  editingTagType.value = ((tag as any)?.tagType || (isInterestTag(tag) ? 'INTEREST' : 'MAJOR')) as 'MAJOR' | 'INTEREST'
  showEditDialog.value = true
  // 随机给一点编辑提示，并自动聚焦输入框
  editHint.value = editHints[Math.floor(Math.random() * editHints.length)] || editHint.value
  nextTick(() => {
    editInputRef.value?.focus?.()
    editInputRef.value?.select?.()
  })
}

const saveTags = async () => {
  savingTags.value = true
  try {
    // 去重/清洗
    const cleaned: UserTag[] = []
    const seen = new Set<string>()
    for (const t of tags.value) {
      const name = (t.tagName || '').trim()
      if (!name) continue
      const key = name.toLowerCase()
      if (seen.has(key)) continue
      seen.add(key)
      const tagType = (t as any)?.tagType as 'MAJOR' | 'INTEREST' | undefined
      cleaned.push({ tagName: name, weight: t.weight ?? 0.9, tagType })
    }
    const res = await updateStudentTags(cleaned)
    tags.value = res.data || cleaned
    rebuildSelectedMap()
    ElMessage.success('标签已保存')
  } catch (e: any) {
    ElMessage.error(e.message || '保存标签失败')
  } finally {
    savingTags.value = false
  }
}

const createTag = () => {
  // 默认作为“兴趣类”标签，权重 0.9，方便学生先写名字再微调
  const blank: UserTag = { tagName: '新标签', weight: 0.9, tagType: 'INTEREST' }
  tags.value = [...tags.value, blank]
  rebuildSelectedMap()
  onTagClick(blank)
}
</script>

<style scoped>
.profile-container {
  max-width: 1000px;
  margin: 0 auto;
}

.bg-card {
  margin-bottom: 16px;
}

.bg-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.bg-preview {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  background: #f5f7fa;
}

.bg-preview-img {
  height: 160px;
  cursor: grab;
}

.bg-preview-img:active {
  cursor: grabbing;
}

.bg-controls {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.bg-control-label {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  color: #606266;
  font-size: 13px;
  margin-bottom: 6px;
}

.bg-control-value {
  color: #909399;
  font-variant-numeric: tabular-nums;
}

.bg-drag-hint {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}

.bg-preview-empty {
  height: 160px;
  display: grid;
  place-items: center;
  color: #909399;
  font-size: 13px;
  padding: 0 16px;
  text-align: center;
}

.bg-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tags-header {
  align-items: flex-start;
}

.tags-header-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
}

.tags-header-left {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.tags-count-wrap {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;
}

.tags-count-label {
  white-space: nowrap;
}

.edit-hint {
  margin-bottom: 12px;
  color: #666;
  font-size: 13px;
  line-height: 1.6;
}

.tag-edit-input :deep(.el-input__wrapper) {
  border-radius: 999px;
  box-shadow: 0 2px 10px rgba(15, 35, 95, 0.08);
  background: linear-gradient(135deg, #ffffff, #f8fbff);
  border: 1px solid rgba(220, 223, 230, 0.9);
  transition:
    box-shadow 0.22s ease,
    border-color 0.22s ease,
    background-color 0.22s ease,
    transform 0.12s ease;
}

.tag-edit-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.18);
  border-color: rgba(64, 158, 255, 0.75);
}

.tag-edit-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 6px 20px rgba(64, 158, 255, 0.32);
  border-color: transparent;
  background-image:
    linear-gradient(135deg, #ffffff, #f8fbff),
    linear-gradient(120deg, #67c23a, #409eff);
  background-origin: border-box;
  background-clip: padding-box, border-box;
  transform: translateY(-1px);
}

.tag-edit-input :deep(.el-input__inner) {
  font-weight: 500;
}

.progress-text {
  margin-top: 8px;
  text-align: center;
  color: #666;
  font-size: 13px;
}

.tag-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.tag-card {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 12px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
  user-select: none;
}

.tag-card:hover {
  border-color: #dcdfe6;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.05);
  transform: translateY(-1px);
}

.tag-card.selectable {
  border-style: dashed;
}

.tag-card.selected {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12);
   transform: translateY(-2px) scale(1.02);
}

.tag-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.tag-weight {
  font-size: 12px;
  color: #888;
  white-space: nowrap;
}

.tag-card-bottom {
  margin-top: 10px;
  color: #666;
  font-size: 12px;
}

.tag-select-hint {
  color: #409eff;
}

.tag-card-fade-enter-active,
.tag-card-fade-leave-active {
  transition: all 0.22s ease-out;
}

.tag-card-fade-enter-from,
.tag-card-fade-leave-to {
  opacity: 0;
  transform: translateY(4px) scale(0.98);
}
</style>

