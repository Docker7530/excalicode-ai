<!--
  锐评大师
  支持用户上传 COSMIC 子过程表格，并流式展示 AI 锐评结果
-->
<template>
  <div class="estimate-master">
    <input
      ref="fileInputRef"
      class="file-input"
      type="file"
      accept=".xlsx,.xls"
      @change="handleFileChange"
    />

    <div class="editor-header">
      <div class="header-main">
        <h2>锐评大师</h2>
        <div class="header-stats">
          <span class="stat-item">
            <ElIcon><DocumentCopy /></ElIcon>
            当前字数：{{ textLength }}
          </span>
          <span v-if="fileName" class="stat-item">
            <ElIcon><Upload /></ElIcon>
            文件：{{ fileName }}
          </span>
        </div>
      </div>

      <div class="header-actions">
        <ElButton
          type="primary"
          plain
          :loading="loading"
          :disabled="loading"
          :icon="Upload"
          @click="handleUploadClick"
        >
          上传拆分
        </ElButton>
        <ElButton
          type="warning"
          plain
          :disabled="!loading"
          :icon="Close"
          @click="$emit('cancel')"
        >
          停止
        </ElButton>
        <ElButton
          type="info"
          plain
          :disabled="!text"
          :icon="DocumentCopy"
          @click="copyText"
        >
          复制
        </ElButton>
        <ElButton type="danger" plain :icon="Delete" @click="$emit('clear')">
          清空
        </ElButton>
      </div>
    </div>

    <div class="editor-body">
      <ElAlert
        type="info"
        :closable="false"
        show-icon
        class="estimate-alert"
        title="上传 COSMIC 子过程表（与系统导入模板一致），系统会把解析出的 AnalysisResponse(JSON) 交给 AI 做锐评。"
      />

      <div class="content-section">
        <ElTabs v-model="activeTab" class="estimate-tabs">
          <ElTabPane label="预览" name="preview">
            <div class="preview-panel">
              <MarkdownPreview :content="normalizedText" />
            </div>
          </ElTabPane>
          <ElTabPane label="原文" name="raw">
            <ElInput
              :model-value="text"
              type="textarea"
              rows="22"
              resize="none"
              readonly
              placeholder="上传 Excel 后，锐评结果会在这里流式输出..."
            />
          </ElTabPane>
        </ElTabs>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 锐评大师组件
 * 负责文件选择与结果展示，上传与流式逻辑由父组件处理
 */

import MarkdownPreview from '@/components/MarkdownPreview.vue';
import { Close, Delete, DocumentCopy, Upload } from '@element-plus/icons-vue';

const props = defineProps({
  text: {
    type: String,
    default: '',
  },
  loading: {
    type: Boolean,
    default: false,
  },
  fileName: {
    type: String,
    default: '',
  },
});

const emit = defineEmits(['upload', 'cancel', 'clear']);

const fileInputRef = ref(null);
const activeTab = ref('preview');

const EXCEL_FILE_PATTERN = /\.(xlsx|xls)$/i;

const normalizedText = computed(() =>
  (props.text || '').replace(/\r\n/g, '\n'),
);
const textLength = computed(() => normalizedText.value.trim().length);

const resetInput = () => {
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
};

const handleUploadClick = () => {
  if (props.loading) {
    return;
  }
  fileInputRef.value?.click();
};

const handleFileChange = (event) => {
  const inputEl = event?.target ?? fileInputRef.value;
  const [file] = inputEl?.files ?? [];
  if (!file) {
    resetInput();
    return;
  }

  if (!EXCEL_FILE_PATTERN.test(file.name)) {
    ElMessage.error('请上传 Excel 文件（.xlsx/.xls）');
    resetInput();
    return;
  }

  emit('upload', file);
  resetInput();
};

const copyText = async () => {
  const source = normalizedText.value.trim();
  if (!source) {
    ElMessage.warning('暂无可复制的锐评内容');
    return;
  }

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(source);
      ElMessage.success('锐评内容已复制');
      return;
    }

    const textarea = document.createElement('textarea');
    textarea.value = source;
    textarea.setAttribute('readonly', 'readonly');
    textarea.style.position = 'fixed';
    textarea.style.left = '-9999px';
    textarea.style.top = '-9999px';
    document.body.appendChild(textarea);
    textarea.select();
    const ok = document.execCommand('copy');
    document.body.removeChild(textarea);
    if (!ok) {
      throw new Error('复制失败，请手动复制');
    }
    ElMessage.success('锐评内容已复制');
  } catch (error) {
    ElMessage.error(error?.message || '复制失败，请稍后重试');
  }
};
</script>

<style lang="scss" scoped>
.estimate-master {
  background: $background-primary;
  border-radius: $border-radius-2xl;
  box-shadow: $shadow-lg;
  border: 1px solid $border-light;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 600px;
}

.file-input {
  display: none;
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
      background: linear-gradient(135deg, #ff4d4f 0%, #faad14 100%);
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
  gap: $spacing-lg;

  .estimate-alert {
    border-radius: $border-radius-lg;
  }

  .content-section {
    flex: 1;

    .estimate-tabs {
      height: 100%;

      :deep(.el-tabs__content) {
        height: 100%;
      }

      :deep(.el-tab-pane) {
        height: 100%;
      }
    }

    .preview-panel {
      min-height: 460px;
      border: 1px solid $border-medium;
      border-radius: $border-radius-lg;
      background: #ffffff;
      padding: $spacing-lg;
      overflow: auto;
    }

    :deep(.markdown-preview) {
      font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
      font-size: $font-size-base;
      line-height: $line-height-relaxed;
    }

    :deep(.el-textarea__inner) {
      min-height: 460px;
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

@media (max-width: 640px) {
  .editor-header {
    padding: $spacing-xl;
  }

  .editor-body {
    padding: $spacing-xl;
  }
}
</style>
