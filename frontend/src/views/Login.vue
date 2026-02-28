<template>
  <div class="login-container">
    <div class="login-wrapper">
      <!-- 左侧：Logo 和图形 -->
      <div class="login-left">
        <div class="logo-section">
          <div class="logo-circle">
            <el-icon class="logo-icon"><Document /></el-icon>
          </div>
          <h1 class="system-title">毕业论文选题与反馈系统</h1>
          <p class="system-subtitle">Graduation Thesis Topic Selection System</p>
          <div class="decorative-elements">
            <div class="circle circle-1"></div>
            <div class="circle circle-2"></div>
            <div class="circle circle-3"></div>
          </div>
        </div>
      </div>
      
      <!-- 右侧：登录/注册表单 -->
      <div class="login-right">
        <div class="login-form-box">
          <div class="form-header">
            <h2 class="form-title">{{ isRegister ? '用户注册' : '欢迎登录' }}</h2>
            <p class="form-subtitle">{{ isRegister ? '创建您的账号' : '请输入您的账号信息' }}</p>
          </div>
          
          <!-- 登录表单 -->
          <el-form v-if="!isRegister" :model="form" :rules="loginRules" ref="loginFormRef" class="login-form">
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入学号或教工号"
                size="large"
                :prefix-icon="User"
                autocomplete="off"
                name="username"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                autocomplete="current-password"
                name="password"
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                @click="handleLogin"
                class="login-button"
              >
                {{ loading ? '登录中...' : '登录' }}
              </el-button>
            </el-form-item>
          </el-form>
          
          <!-- 注册表单 -->
          <el-form v-else :model="registerForm" :rules="registerRules" ref="registerFormRef" class="login-form">
            <el-form-item prop="username">
              <el-input
                v-model="registerForm.username"
                :placeholder="registerForm.role === 'STUDENT' ? '学号：仅字母和数字，3-50位' : '教工号：仅字母和数字，3-50位'"
                size="large"
                :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item prop="realName">
              <el-input
                v-model="registerForm.realName"
                placeholder="真实姓名：中文、字母、空格或·"
                size="large"
                :prefix-icon="UserFilled"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="密码（6-20个字符）"
                size="large"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="确认密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="handleRegister"
              />
            </el-form-item>
            <el-form-item prop="role">
              <el-select
                v-model="registerForm.role"
                placeholder="选择角色"
                size="large"
                style="width: 100%"
                @change="onRoleChange"
              >
                <el-option label="学生" value="STUDENT" />
                <el-option label="导师" value="TEACHER" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                @click="handleRegister"
                class="login-button"
              >
                {{ loading ? '注册中...' : '注册' }}
              </el-button>
            </el-form-item>
          </el-form>
          
          <!-- 切换登录/注册 -->
          <div class="form-footer">
            <el-button
              type="text"
              @click="toggleMode"
              class="toggle-button"
            >
              {{ isRegister ? '已有账号？去登录' : '没有账号？立即注册' }}
            </el-button>
          </div>
          
          <div class="login-tip">
            <el-icon><InfoFilled /></el-icon>
            <span v-if="!isRegister">请使用账号登录，或点击上方注册新账号</span>
            <span v-else>注册后自动登录，学生和导师可以自行注册</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Document, InfoFilled, UserFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { login, register } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()

const isRegister = ref(false)
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  realName: '',
  password: '',
  confirmPassword: '',
  role: 'STUDENT' as 'STUDENT' | 'TEACHER'
})

const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入学号或教工号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const validateConfirmPassword = (_rule: any, value: any, callback: any) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 学号/教工号：仅字母、数字
const validateUsername = (_rule: any, value: string, callback: (err?: Error) => void) => {
  if (!/^[A-Za-z0-9]+$/.test(value || '')) {
    callback(new Error('学号或教工号只能包含字母和数字'))
  } else {
    callback()
  }
}
// 真实姓名：中文、字母、空格、·
const validateRealName = (_rule: any, value: string, callback: (err?: Error) => void) => {
  if (!/^[\u4e00-\u9fa5A-Za-z\s·]+$/.test(value || '')) {
    callback(new Error('真实姓名只能包含中文、字母、空格或·'))
  } else {
    callback()
  }
}

// 使用计算属性动态生成验证规则（根据角色）
const registerRules = computed(() => {
  const isStudent = registerForm.role === 'STUDENT'
  return {
    username: [
      { required: true, message: isStudent ? '请输入学号' : '请输入教工号', trigger: 'blur' },
      { min: 3, max: 50, message: isStudent ? '学号长度必须在3-50个字符之间' : '教工号长度必须在3-50个字符之间', trigger: 'blur' },
      { validator: validateUsername, trigger: 'blur' }
    ],
    realName: [
      { required: true, message: '请输入真实姓名', trigger: 'blur' },
      { min: 1, max: 50, message: '真实姓名长度必须在1-50个字符之间', trigger: 'blur' },
      { validator: validateRealName, trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 20, message: '密码长度必须在6-20个字符之间', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, message: '请确认密码', trigger: 'blur' },
      { validator: validateConfirmPassword, trigger: 'blur' }
    ],
    role: [
      { required: true, message: '请选择角色', trigger: 'change' }
    ]
  }
})

const toggleMode = () => {
  isRegister.value = !isRegister.value
  // 清空表单
  if (isRegister.value) {
    Object.assign(form, { username: '', password: '' })
    loginFormRef.value?.clearValidate()
  } else {
    Object.assign(registerForm, {
      username: '',
      realName: '',
      password: '',
      confirmPassword: '',
      role: 'STUDENT'
    })
    registerFormRef.value?.clearValidate()
  }
}

// 监听角色变化，更新验证规则
const onRoleChange = () => {
  if (registerFormRef.value) {
    // 清除之前的验证状态
    registerFormRef.value.clearValidate('username')
    // 重新验证用户名字段（使用新的规则）
    setTimeout(() => {
      registerFormRef.value?.validateField('username')
    }, 0)
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await login(form)
        authStore.setAuth(res.data)
        
        // 清空登录表单
        Object.assign(form, { username: '', password: '' })
        loginFormRef.value?.clearValidate()
        
        ElMessage.success('登录成功，正在跳转...')
        
        // 延迟跳转，确保消息显示
        setTimeout(() => {
          // 根据角色跳转
          if (authStore.isStudent()) {
            router.push('/student')
          } else if (authStore.isTeacher()) {
            router.push('/teacher')
          } else if (authStore.isAdmin()) {
            router.push('/admin')
          }
        }, 500)
      } catch (error: any) {
        ElMessage.error(error.message || '登录失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const res = await register({
          username: registerForm.username.trim(),
          realName: registerForm.realName.trim(),
          password: registerForm.password,
          role: registerForm.role
        })
        authStore.setAuth(res.data)
        
        // 清空注册表单，避免数据残留
        Object.assign(registerForm, {
          username: '',
          realName: '',
          password: '',
          confirmPassword: '',
          role: 'STUDENT'
        })
        registerFormRef.value?.clearValidate()
        
        ElMessage.success('注册成功，正在跳转...')
        
        // 延迟跳转，确保消息显示
        setTimeout(() => {
          // 根据角色跳转
          if (authStore.isStudent()) {
            router.push('/student')
          } else if (authStore.isTeacher()) {
            router.push('/teacher')
          }
        }, 500)
      } catch (error: any) {
        ElMessage.error(error.message || '注册失败')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.login-wrapper {
  width: 90%;
  max-width: 1200px;
  height: 80vh;
  max-height: 700px;
  display: flex;
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

/* 左侧：Logo 区域 */
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.logo-section {
  text-align: center;
  color: white;
  z-index: 2;
  position: relative;
}

.logo-circle {
  width: 120px;
  height: 120px;
  margin: 0 auto 30px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  border: 2px solid rgba(255, 255, 255, 0.3);
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-20px);
  }
}

.logo-icon {
  font-size: 60px;
  color: white;
}

.system-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 10px 0;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

.system-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
  font-weight: 300;
  letter-spacing: 2px;
}

.decorative-elements {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 6s ease-in-out infinite;
}

.circle-1 {
  width: 200px;
  height: 200px;
  top: -50px;
  left: -50px;
  animation-delay: 0s;
}

.circle-2 {
  width: 150px;
  height: 150px;
  bottom: -30px;
  right: -30px;
  animation-delay: 2s;
}

.circle-3 {
  width: 100px;
  height: 100px;
  top: 50%;
  right: 20%;
  animation-delay: 4s;
}

/* 右侧：登录表单 */
.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: white;
}

.login-form-box {
  width: 100%;
  max-width: 400px;
}

.form-header {
  margin-bottom: 30px;
}

.form-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px 0;
  text-align: center;
}

.form-subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
  text-align: center;
}

.form-footer {
  margin-top: 20px;
  text-align: center;
}

.toggle-button {
  color: #667eea;
  font-size: 14px;
  padding: 0;
}

.toggle-button:hover {
  color: #764ba2;
}

.login-form {
  margin-top: 20px;
}

.login-form :deep(.el-input__wrapper) {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.login-button {
  width: 100%;
  margin-top: 10px;
  height: 45px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.login-button:hover {
  background: linear-gradient(135deg, #5568d3 0%, #6a3f8f 100%);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  transition: all 0.3s;
}

.login-tip {
  margin-top: 30px;
  text-align: center;
  color: #999;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.login-tip .el-icon {
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-wrapper {
    flex-direction: column;
    height: 100vh;
    max-height: none;
    border-radius: 0;
  }
  
  .login-left {
    flex: 0 0 40%;
    min-height: 200px;
  }
  
  .system-title {
    font-size: 24px;
  }
  
  .system-subtitle {
    font-size: 12px;
  }
  
  .logo-circle {
    width: 80px;
    height: 80px;
  }
  
  .logo-icon {
    font-size: 40px;
  }
}
</style>

