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
        </div>
      </template>
      <div v-if="tags.length === 0" style="text-align: center; color: #999; padding: 20px">
        暂无标签，请先完善研究方向后自动生成
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
import { getTeacherProfile, updateTeacherProfile, type UserTag } from '@/api/teacher'

const authStore = useAuthStore()

const form = ref({
  username: '',
  realName: authStore.realName || '',
  title: '',
  researchDirection: '',
  maxStudentCount: 10
})

const tags = ref<UserTag[]>([])
const saving = ref(false)

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
    }
  } catch (error) {
    ElMessage.error('加载信息失败')
  }
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
    ElMessage.success('保存成功，标签已自动生成')
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

