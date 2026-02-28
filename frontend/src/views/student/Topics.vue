<template>
  <div class="topics-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>选题中心</span>
          <el-button type="primary" @click="refreshTopics">刷新</el-button>
        </div>
      </template>

      <el-table :data="topics" style="width: 100%" v-loading="loading">
        <el-table-column prop="title" label="选题标题" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
        <el-table-column prop="teacherName" label="导师" width="120" />
        <el-table-column prop="currentApplicants" label="已申请人数" width="120" />
        <el-table-column prop="maxApplicants" label="最大人数" width="100" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewTopic(row)">查看</el-button>
            <el-button type="success" size="small" @click="applyTopic(row)" :disabled="row.currentApplicants >= row.maxApplicants">
              申请
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 选题详情对话框 -->
    <el-dialog v-model="dialogVisible" title="选题详情" width="600px">
      <div v-if="selectedTopic">
        <p><strong>标题：</strong>{{ selectedTopic.title }}</p>
        <p><strong>描述：</strong>{{ selectedTopic.description }}</p>
        <p><strong>导师：</strong>{{ selectedTopic.teacherName }}</p>
        <p><strong>申请人数：</strong>{{ selectedTopic.currentApplicants }} / {{ selectedTopic.maxApplicants }}</p>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 申请对话框 -->
    <el-dialog v-model="applyDialogVisible" title="提交申请" width="500px">
      <el-form :model="applyForm" label-width="100px">
        <el-form-item label="申请备注">
          <el-input
            v-model="applyForm.remark"
            type="textarea"
            :rows="4"
            placeholder="请输入申请理由或备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOpenTopics, type Topic } from '@/api/topic'
import { submitApplication } from '@/api/application'

const loading = ref(false)
const topics = ref<Topic[]>([])
const dialogVisible = ref(false)
const selectedTopic = ref<Topic | null>(null)
const applyDialogVisible = ref(false)
const submitting = ref(false)

const applyForm = ref({
  remark: ''
})

onMounted(() => {
  loadTopics()
})

const loadTopics = async () => {
  loading.value = true
  try {
    const res = await getOpenTopics()
    topics.value = res.data || []
  } catch (error) {
    ElMessage.error('加载选题失败')
  } finally {
    loading.value = false
  }
}

const refreshTopics = () => {
  loadTopics()
}

const viewTopic = (topic: Topic) => {
  selectedTopic.value = topic
  dialogVisible.value = true
}

const applyTopic = (topic: Topic) => {
  selectedTopic.value = topic
  applyForm.value.remark = ''
  applyDialogVisible.value = true
}

const handleApply = () => {
  dialogVisible.value = false
  applyTopic(selectedTopic.value!)
}

const submitApply = async () => {
  if (!selectedTopic.value) return
  
  submitting.value = true
  try {
    await submitApplication({
      topicId: selectedTopic.value.id,
      remark: applyForm.value.remark
    })
    ElMessage.success('申请提交成功')
    applyDialogVisible.value = false
    loadTopics()
  } catch (error: any) {
    ElMessage.error(error.message || '申请提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.topics-container {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

