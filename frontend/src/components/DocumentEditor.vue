<!--
  需求文档编辑器
  提供 AI 生成内容的预览、微调与导出能力
-->
<template>
  <div class="document-editor">
    <!-- 头部操作区 -->
    <div class="editor-header">
      <div class="header-main">
        <h2>需求文档预览</h2>
        <div class="header-stats">
          <span class="stat-item">
            <ElIcon><DocumentCopy /></ElIcon>
            当前字数：{{ contentLength }}
          </span>
        </div>
      </div>
      <div class="header-actions">
        <ElButton
          type="primary"
          :icon="RefreshRight"
          :loading="generating"
          :disabled="generating"
          @click="$emit('regenerate')"
        >
          重新生成
        </ElButton>
      </div>
    </div>

    <!-- 主编辑区域 -->
    <div class="editor-body">
      <div class="content-section">
        <ElInput
          v-model="editableContent"
          type="textarea"
          rows="24"
          resize="none"
          :disabled="generating"
          placeholder="AI 正在生成需求文档..."
        />
      </div>
    </div>

    <!-- 底部操作区 -->
    <div class="editor-footer">
      <div class="footer-note">
        <ElIcon><InfoFilled /></ElIcon>
        <span>确认内容后可导出最终文档</span>
      </div>
      <div class="footer-actions">
        <ElButton
          type="success"
          size="large"
          :icon="DocumentChecked"
          :loading="finalizing"
          :disabled="!editableContent.trim() || finalizing"
          @click="handleFinalize"
        >
          导出最终文档
        </ElButton>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 需求文档编辑预览器
 * 提供 AI 文稿的展示、微调与导出入口
 */

import {
  DocumentChecked,
  DocumentCopy,
  InfoFilled,
  RefreshRight,
} from '@element-plus/icons-vue';

const props = defineProps({
  initialContent: {
    type: String,
    default: '',
  },
  generating: {
    type: Boolean,
    default: false,
  },
  finalizing: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['regenerate', 'finalize', 'content-change']);

// ==================== 状态与派生数据 ====================
const editableContent = ref(props.initialContent || '');

// ==================== 同步外部数据 ====================
watch(
  () => props.initialContent,
  (value) => {
    const nextValue = value || '';
    if (nextValue !== editableContent.value) {
      editableContent.value = nextValue;
    }
  },
  { immediate: true },
);

watch(editableContent, (value) => {
  emit('content-change', value);
});

const contentLength = computed(() => editableContent.value.trim().length);

// ==================== 事件处理 ====================
/**
 * 通知父组件导出终稿
 */
const handleFinalize = () => {
  emit('finalize', editableContent.value);
};
</script>

<style lang="scss" scoped>
.document-editor {
  background: $background-primary;
  border-radius: $border-radius-2xl;
  box-shadow: $shadow-lg;
  border: 1px solid $border-light;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 600px;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: $spacing-xl;
  padding: $spacing-2xl $spacing-3xl;
  background: linear-gradient(
    135deg,
    rgba($background-secondary, 0.9) 0%,
    rgba($background-tertiary, 0.9) 100%
  );
  border-bottom: 1px solid $border-light;

  .header-main {
    flex: 1;

    h2 {
      margin: 0 0 $spacing-md 0;
      font-size: $font-size-2xl;
      font-weight: $font-weight-semibold;
      background: linear-gradient(135deg, #4a90e2 0%, #50c878 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }

  .header-stats {
    display: flex;
    gap: $spacing-lg;
    flex-wrap: wrap;

    .stat-item {
      display: flex;
      align-items: center;
      gap: $spacing-xs;
      color: $secondary-color;
      font-size: $font-size-sm;
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: $spacing-md;
    flex-wrap: wrap;
  }

  @media (max-width: $breakpoint-lg) {
    flex-direction: column;
    align-items: stretch;

    .header-actions {
      justify-content: flex-end;
    }
  }
}

.editor-body {
  flex: 1;
  padding: $spacing-2xl $spacing-3xl;
  overflow: auto;
  display: flex;
  flex-direction: column;

  .content-section {
    flex: 1;

    :deep(.el-textarea__inner) {
      min-height: 480px;
      font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
      font-size: $font-size-base;
      line-height: $line-height-relaxed;
      border: 1px solid $border-medium;
      border-radius: $border-radius-lg;

      &:focus {
        border-color: $accent-color;
        box-shadow: 0 0 0 3px rgba($accent-color, 0.1);
      }
    }
  }
}

.editor-footer {
  padding: $spacing-xl $spacing-3xl;
  border-top: 1px solid $border-light;
  background: rgba($background-secondary, 0.6);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: $spacing-lg;
  flex-wrap: wrap;

  .footer-note {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    color: $secondary-color;
    font-size: $font-size-sm;
  }

  .footer-actions {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }

  @media (max-width: $breakpoint-md) {
    flex-direction: column;
    align-items: stretch;

    .footer-actions {
      justify-content: center;
    }
  }
}

@media (max-width: 640px) {
  .editor-header {
    padding: $spacing-xl;
  }

  .editor-body {
    padding: $spacing-lg;
  }
}
</style>
