<template>
  <div class="topics-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>选题管理</span>
          <el-button type="primary" @click="showCreateDialog">创建选题</el-button>
        </div>
      </template>

      <el-table :data="topics" style="width: 100%" v-loading="loading">
        <el-table-column prop="title" label="选题标题" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'DRAFT'" type="info">草稿</el-tag>
            <el-tag v-else-if="row.status === 'PENDING_REVIEW'" type="warning">待审核</el-tag>
            <el-tag v-else-if="row.status === 'REJECTED'" type="danger">已驳回</el-tag>
            <el-tag v-else-if="row.status === 'OPEN'" type="success">已开放</el-tag>
            <el-tag v-else-if="row.status === 'CLOSED'" type="">已关闭</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentApplicants" label="已申请人数" width="120" />
        <el-table-column prop="maxApplicants" label="最大人数" width="100" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="editTopic(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'REJECTED'"
              type="success"
              size="small"
              @click="submitReview(row)"
            >
              提交审核
            </el-button>
            <el-button type="info" size="small" @click="checkDuplicate(row)">去重检测</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑选题对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="选题标题" required>
          <el-input v-model="form.title" placeholder="请输入选题标题" />
        </el-form-item>
        <el-form-item label="选题描述" required>
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入选题描述"
          />
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="form.maxApplicants" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input
            v-model="form.tags"
            placeholder="多个标签用逗号分隔"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTopic" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTopics, createTopic, updateTopic, submitTopicForReview, checkTopicDuplicate, type Topic } from '@/api/topic'

const loading = ref(false)
const topics = ref<Topic[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const form = ref({
  title: '',
  description: '',
  maxApplicants: 1,
  tags: ''
})

const dialogTitle = computed(() => {
  return editingId.value ? '编辑选题' : '创建选题'
})

onMounted(() => {
  loadTopics()
})

const loadTopics = async () => {
  loading.value = true
  try {
    const res = await getTopics()
    topics.value = res.data || []
  } catch (error) {
    ElMessage.error('加载选题失败')
  } finally {
    loading.value = false
  }
}

const showCreateDialog = () => {
  editingId.value = null
  form.value = {
    title: '',
    description: '',
    maxApplicants: 1,
    tags: ''
  }
  dialogVisible.value = true
}

const editTopic = (topic: Topic) => {
  editingId.value = topic.id
  form.value = {
    title: topic.title,
    description: topic.description || '',
    maxApplicants: topic.maxApplicants,
    tags: topic.tags?.join(',') || ''
  }
  dialogVisible.value = true
}

const saveTopic = async () => {
  if (!form.value.title) {
    ElMessage.warning('请输入选题标题')
    return
  }
  
  saving.value = true
  try {
    const tags = form.value.tags ? form.value.tags.split(',').map(t => t.trim()).filter(t => t) : []
    const data = {
      title: form.value.title,
      description: form.value.description,
      maxApplicants: form.value.maxApplicants,
      tags: tags
    }
    
    if (editingId.value) {
      await updateTopic(editingId.value, data)
    } else {
      await createTopic(data)
    }
    
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadTopics()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const submitReview = async (topic: Topic) => {
  try {
    await ElMessageBox.confirm('确定要提交审核吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await submitTopicForReview(topic.id)
    ElMessage.success('提交成功')
    loadTopics()
  } catch (error: any) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  }
}

const checkDuplicate = async (topic: Topic) => {
  try {
    const res = await checkTopicDuplicate({
      topicId: topic.id,
      title: topic.title,
      description: topic.description
    })
    if (res.data.passed) {
      ElMessage.success(`去重检测通过，相似度：${(res.data.maxSimilarity * 100).toFixed(2)}%`)
    } else {
      ElMessage.warning(`检测到相似选题：${res.data.similarTopicTitle}，相似度：${(res.data.maxSimilarity * 100).toFixed(2)}%`)
    }
  } catch (error: any) {
    ElMessage.warning(error.message || '检测到相似选题')
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

