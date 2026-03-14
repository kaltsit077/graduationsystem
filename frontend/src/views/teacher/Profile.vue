<template>
  <div class="profile-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
        </div>
      </template>

      <el-form :model="form" label-width="120px" style="max-width: 600px">
        <el-form-item label="工号">
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
        <el-form-item label="职称">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="研究方向">
          <el-input
            v-model="form.researchDirection"
            type="textarea"
            :rows="4"
            placeholder="请填写您的研究方向（用于自动生成标签）"
          />
        </el-form-item>
        <el-form-item label="最大可带学生数">
          <el-input-number v-model="form.maxStudentCount" :min="1" :max="20" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveProfile" :loading="saving">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>我的标签</span>
          <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap; justify-content: flex-end">
            <el-button
              type="primary"
              size="small"
              @click="generateTags"
              :loading="generating"
              :disabled="generating || rerolling || savingTags || selectMode"
            >
              生成我的标签
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
        暂无标签，请先完善研究方向后点击“生成我的标签”
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import {
  getTeacherProfile,
  updateTeacherProfile,
  changeTeacherPassword,
  updateTeacherTags,
  regenerateTeacherTags,
  type UserTag
} from '@/api/teacher'

const authStore = useAuthStore()

const form = ref({
  username: '',
  realName: authStore.realName || '',
  title: '',
  researchDirection: '',
  maxStudentCount: 10
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
  'AI 正在为你阅读研究方向…',
  '正在匹配与你最相近的课题方向…',
  '快好了，马上生成专属标签～'
]
let progressMessageTimer: any = null

const selectedMap = ref<Record<string, boolean>>({})
const selectedNames = ref<string[]>([])

const showEditDialog = ref(false)
const activeTag = ref<UserTag | null>(null)
const editingValue = ref('')

const selectMode = ref(false)
const selectAction = ref<'delete' | 'reroll'>('delete')

const editInputRef = ref()
const editHint = ref('点击保存后会更新到“我的标签”。想批量删除/重生成，请使用右上角“选择删除 / 选择重新生成”。')
const editHints = [
  '可以给标签起一个更贴近你研究方向的名字。',
  '尽量用简短的名词短语，比如“运筹优化”“项目管理”。',
  '修改这里只会影响你自己的标签，不会影响推荐模型。',
  '想一次处理多个标签，可以用右上角的“选择删除 / 选择重新生成”。'
]

onMounted(() => {
  loadProfile()
})

const loadProfile = async () => {
  try {
    const res = await getTeacherProfile()
    if (res.data) {
      form.value.username = res.data.username || form.value.username
      form.value.realName = res.data.realName || form.value.realName
      form.value.title = res.data.title || ''
      form.value.researchDirection = res.data.researchDirection || ''
      form.value.maxStudentCount = res.data.maxStudentCount || 10
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
  return Number(tag.weight) >= 0.85
}

const tagColorType = (tag: UserTag) => {
  return isInterestTag(tag) ? 'warning' : 'primary'
}

const saveProfile = async () => {
  saving.value = true
  try {
    await updateTeacherProfile({
      realName: form.value.realName,
      title: form.value.title,
      researchDirection: form.value.researchDirection,
      maxStudentCount: form.value.maxStudentCount
    })
    ElMessage.success('保存成功')
    if (form.value.realName) {
      authStore.setRealName(form.value.realName)
    }
    await loadProfile()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
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

const generateTags = async () => {
  if (generating.value || rerolling.value) return
  if (!form.value.researchDirection) {
    ElMessage.warning('请先填写研究方向，再生成标签')
    return
  }
  generating.value = true
  startProgress()
  try {
    await saveProfile()

    const excludeAll = tags.value.map((t) => t.tagName)
    const res = await regenerateTeacherTags({
      interestDesc: form.value.researchDirection,
      pinnedTags: [],
      excludeTagNames: excludeAll,
      desiredTotal: 5
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

const rerollWithPinned = async (pinned: UserTag[], exclude: string[], desiredTotal: number) => {
  rerolling.value = true
  startProgress()
  try {
    const res = await regenerateTeacherTags({
      interestDesc: form.value.researchDirection,
      pinnedTags: pinned,
      excludeTagNames: exclude,
      desiredTotal
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
    const pinned = tags.value.filter((t) => t.tagName !== tag.tagName)
    const exclude = tags.value.map((t) => t.tagName)
    await rerollWithPinned(pinned, exclude, tags.value.length || 5)
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
  tags.value = tags.value.map((t) => (t.tagName === tag.tagName ? { ...t, tagName: newName } : t))
  showEditDialog.value = false
  activeTag.value = null
  editingValue.value = ''
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
  await rerollWithPinned(pinned, exclude, pinned.length + selectedNames.value.length)
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
  showEditDialog.value = true
  editHint.value = editHints[Math.floor(Math.random() * editHints.length)] || editHint.value
  nextTick(() => {
    editInputRef.value?.focus?.()
    editInputRef.value?.select?.()
  })
}

const saveTags = async () => {
  savingTags.value = true
  try {
    const cleaned: UserTag[] = []
    const seen = new Set<string>()
    for (const t of tags.value) {
      const name = (t.tagName || '').trim()
      if (!name) continue
      const key = name.toLowerCase()
      if (seen.has(key)) continue
      seen.add(key)
      cleaned.push({ tagName: name, weight: t.weight ?? 0.9 })
    }
    const res = await updateTeacherTags(cleaned)
    tags.value = res.data || cleaned
    rebuildSelectedMap()
    ElMessage.success('标签已保存')
  } catch (e: any) {
    ElMessage.error(e.message || '保存标签失败')
  } finally {
    savingTags.value = false
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
    await changeTeacherPassword(oldPwd, newPwd)
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
</script>

<style scoped>
.profile-container {
  max-width: 1000px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

