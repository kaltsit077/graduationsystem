<template>
  <div class="accounts-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>账号管理</span>
          <el-button
            type="primary"
            :disabled="currentSelection.length === 0"
            @click="batchResetPassword"
          >
            批量重置为 123456（已选 {{ currentSelection.length }} 人）
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="教师账号" name="teachers">
          <el-table
            ref="teacherTableRef"
            :data="teachers"
            style="width: 100%"
            v-loading="loadingTeachers"
            @selection-change="(val: AdminUser[]) => (selectedTeachers = val)"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" width="140" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="passwordDisplay" label="密码" width="100" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="注册时间" width="180" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openChangePassword(row)">修改密码</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loadingTeachers && teachers.length === 0" description="暂无教师账号" />
        </el-tab-pane>
        <el-tab-pane label="学生账号" name="students">
          <el-table
            ref="studentTableRef"
            :data="students"
            style="width: 100%"
            v-loading="loadingStudents"
            @selection-change="(val: AdminUser[]) => (selectedStudents = val)"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" width="140" />
            <el-table-column prop="realName" label="姓名" width="120" />
            <el-table-column prop="passwordDisplay" label="密码" width="100" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="注册时间" width="180" />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openChangePassword(row)">修改密码</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loadingStudents && students.length === 0" description="暂无学生账号" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px" @close="closePasswordDialog">
      <el-form :model="passwordForm" label-width="80px">
        <el-form-item label="账号">
          <span>{{ passwordForm.username }}（{{ passwordForm.realName }}）</span>
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChangePassword" :loading="passwordSubmitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import {
  getAdminTeachers,
  getAdminStudents,
  updateUserPassword,
  resetPasswordsToDefault,
  type AdminUser
} from '@/api/admin'

const activeTab = ref<'teachers' | 'students'>('teachers')
const teachers = ref<AdminUser[]>([])
const students = ref<AdminUser[]>([])
const loadingTeachers = ref(false)
const loadingStudents = ref(false)
const teacherTableRef = ref<TableInstance>()
const studentTableRef = ref<TableInstance>()
const selectedTeachers = ref<AdminUser[]>([])
const selectedStudents = ref<AdminUser[]>([])

const currentSelection = computed(() =>
  activeTab.value === 'teachers' ? selectedTeachers.value : selectedStudents.value
)

const passwordDialogVisible = ref(false)
const passwordSubmitting = ref(false)
const passwordForm = ref({
  userId: 0,
  username: '',
  realName: '',
  newPassword: ''
})

onMounted(() => {
  loadTeachers()
  loadStudents()
})

const loadTeachers = async () => {
  loadingTeachers.value = true
  try {
    const res = await getAdminTeachers()
    teachers.value = res.data || []
  } catch {
    ElMessage.error('加载教师列表失败')
  } finally {
    loadingTeachers.value = false
  }
}

const loadStudents = async () => {
  loadingStudents.value = true
  try {
    const res = await getAdminStudents()
    students.value = res.data || []
  } catch {
    ElMessage.error('加载学生列表失败')
  } finally {
    loadingStudents.value = false
  }
}

function openChangePassword(row: AdminUser) {
  passwordForm.value = {
    userId: row.id,
    username: row.username,
    realName: row.realName || '',
    newPassword: ''
  }
  passwordDialogVisible.value = true
}

function closePasswordDialog() {
  passwordForm.value = { userId: 0, username: '', realName: '', newPassword: '' }
}

const submitChangePassword = async () => {
  const pwd = passwordForm.value.newPassword?.trim()
  if (!pwd) {
    ElMessage.warning('请输入新密码')
    return
  }
  passwordSubmitting.value = true
  try {
    await updateUserPassword(passwordForm.value.userId, pwd)
    ElMessage.success('密码已修改')
    passwordDialogVisible.value = false
    closePasswordDialog()
    if (activeTab.value === 'teachers') loadTeachers()
    else loadStudents()
  } catch {
    ElMessage.error('修改失败')
  } finally {
    passwordSubmitting.value = false
  }
}

const batchResetPassword = async () => {
  const list = currentSelection.value
  if (list.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定将选中的 ${list.length} 个账号的密码重置为 123456 吗？`,
      '批量重置密码',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  const ids = list.map((u) => u.id)
  try {
    const res = await resetPasswordsToDefault(ids)
    ElMessage.success(`已重置 ${res.data ?? 0} 个账号密码为 123456`)
    if (activeTab.value === 'teachers') {
      loadTeachers()
      teacherTableRef.value?.clearSelection()
    } else {
      loadStudents()
      studentTableRef.value?.clearSelection()
    }
  } catch {
    ElMessage.error('批量重置失败')
  }
}
</script>

<style scoped>
.accounts-container {
  max-width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
