<template>
  <el-dialog
    :model-value="modelValue"
    :width="width"
    class="app-dialog"
    :close-on-click-modal="closeOnClickModal"
    :show-close="false"
    :destroy-on-close="destroyOnClose"
    @update:model-value="(v: boolean) => $emit('update:modelValue', v)"
  >
    <template #header>
      <div class="app-dialog-header">
        <span class="app-dialog-title">{{ title }}</span>
        <button
          type="button"
          class="app-dialog-close"
          aria-label="关闭"
          @click="$emit('update:modelValue', false)"
        >
          <el-icon><Close /></el-icon>
        </button>
      </div>
    </template>
    <div class="app-dialog-body" :style="bodyStyle">
      <slot />
    </div>
    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { Close } from '@element-plus/icons-vue'
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    width?: string | number
    /** 是否点击遮罩关闭，默认 false，仅能点 X 关闭 */
    closeOnClickModal?: boolean
    /** 内容区最大高度，如 "600px" 或 "80vh"，不传则由内容撑开 */
    bodyMaxHeight?: string
    destroyOnClose?: boolean
  }>(),
  {
    width: '560px',
    closeOnClickModal: false,
    bodyMaxHeight: '',
    destroyOnClose: false
  }
)

defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const bodyStyle = computed(() => {
  if (!props.bodyMaxHeight) return {}
  return {
    maxHeight: props.bodyMaxHeight,
    overflow: 'auto'
  }
})
</script>

<style scoped>
.app-dialog :deep(.el-dialog__header) {
  margin-right: 0;
  padding: 0;
}

.app-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 56px;
  padding: 0 20px 0 24px;
  background: #409eff;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  border-radius: 4px 4px 0 0;
}

.app-dialog-title {
  letter-spacing: 0.5px;
}

.app-dialog-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  color: #f56c6c;
  transition: background-color 0.2s, color 0.2s;
}

.app-dialog-close:hover {
  background: rgba(245, 108, 108, 0.2);
  color: #f56c6c;
}

.app-dialog-close .el-icon {
  font-size: 22px;
}

.app-dialog :deep(.el-dialog__body) {
  padding: 16px 20px;
}

.app-dialog-body {
  box-sizing: border-box;
}
</style>
