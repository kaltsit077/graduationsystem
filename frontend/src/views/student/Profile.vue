<template>
  <div class="profile-container">
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
        <el-form-item label="姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="form.major" />
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
      <el-form-item label="标签生成模式">
        <el-radio-group v-model="form.tagMode">
          <el-radio label="BOTH">综合（专业 + 兴趣）</el-radio>
          <el-radio label="MAJOR">仅根据专业生成</el-radio>
          <el-radio label="INTEREST">仅根据兴趣生成</el-radio>
        </el-radio-group>
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
          <el-button type="primary" size="small" @click="generateTags">自动生成标签</el-button>
        </div>
      </template>
      <div v-if="tags.length === 0" style="text-align: center; color: #999; padding: 20px">
        暂无标签，请先完善个人信息后自动生成
      </div>
      <el-tag
        v-for="tag in tags"
        :key="tag.id"
        style="margin: 5px"
        :type="tag.weight > 0.8 ? 'success' : 'info'"
      >
        {{ tag.tagName }} (权重: {{ tag.weight }})
      </el-tag>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getStudentProfile, updateStudentProfile, type UserTag } from '@/api/student'

const authStore = useAuthStore()

const form = ref({
  username: '',
  realName: authStore.realName || '',
  major: '',
  grade: '',
  interestDesc: '',
  tagMode: 'BOTH' as 'MAJOR' | 'INTEREST' | 'BOTH'
})

const tags = ref<UserTag[]>([])
const saving = ref(false)

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
      form.value.grade = res.data.grade || ''
      form.value.interestDesc = res.data.interestDesc || ''
      form.value.tagMode = (res.data.tagMode as any) || 'BOTH'
      tags.value = res.data.tags || []
    }
  } catch (error) {
    ElMessage.error('加载信息失败')
  }
}

const saveProfile = async () => {
  saving.value = true
  try {
    await updateStudentProfile({
      realName: form.value.realName,
      major: form.value.major,
      grade: form.value.grade,
      interestDesc: form.value.interestDesc,
      tagMode: form.value.tagMode
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

const generateTags = async () => {
  try {
    // 保存后会自动生成标签，直接重新加载
    await saveProfile()
    ElMessage.success('标签生成成功')
  } catch (error: any) {
    ElMessage.error(error.message || '标签生成失败')
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
</style>

