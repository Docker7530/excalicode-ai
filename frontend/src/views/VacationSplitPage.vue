<!--
  员工休假记录拆分页面
  上传Excel文件，解析并展示备注不为空的员工休假记录
-->

<template>
  <div class="vacation-split-page">
    <AppHeader />
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">员工休假记录拆分</h1>
        <p class="page-description">
          上传考勤Excel后，系统会自动筛选备注、AI修正并生成休假数据表Excel直接供下载
        </p>
      </div>

      <div class="upload-section">
        <ElCard shadow="hover">
          <template #header>
            <div class="card-header-content">
              <ElIcon :size="20">
                <Upload />
              </ElIcon>
              <span>上传Excel文件</span>
            </div>
          </template>

          <ElAlert
            class="process-hint"
            type="info"
            :closable="false"
            title="单击“开始解析并生成”后，将一次性完成数据筛选、备注修正、Excel生成并自动触发下载。"
          />

          <ElUpload
            ref="uploadRef"
            class="upload-area"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
          >
            <ElIcon class="upload-icon" :size="80">
              <UploadFilled />
            </ElIcon>
            <div class="upload-text">
              将Excel文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="upload-tip">
                支持.xlsx和.xls格式，文件大小不超过10MB<br />
                需包含：身份证号码、姓名、一级部门、备注 四个列
              </div>
            </template>
          </ElUpload>

          <div v-if="selectedFile" class="file-info">
            <ElIcon :size="16">
              <Document />
            </ElIcon>
            <span class="file-name">{{ selectedFile.name }}</span>
            <span class="file-size"
              >({{ formatFileSize(selectedFile.size) }})</span
            >
          </div>

          <div class="upload-actions">
            <ElButton
              type="primary"
              size="large"
              :loading="loading"
              :disabled="!selectedFile"
              @click="handleUpload"
            >
              <ElIcon v-if="!loading">
                <Upload />
              </ElIcon>
              {{ loading ? '生成休假数据表中...' : '开始解析并生成休假数据表' }}
            </ElButton>
            <ElButton v-if="selectedFile" size="large" @click="handleClear">
              清除
            </ElButton>
          </div>
        </ElCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import AppHeader from '@/components/AppHeader.vue';
import { api } from '@/api';
import { Document, Upload, UploadFilled } from '@element-plus/icons-vue';
import { ElMessage, genFileId } from 'element-plus';

const uploadRef = ref(null);
const selectedFile = ref(null);
const loading = ref(false);

const handleFileChange = (file) => {
  selectedFile.value = file?.raw ?? null;
};

const handleExceed = (files) => {
  const upload = uploadRef.value;
  if (!upload) return;

  const file = files?.[0];
  if (!file) return;

  upload.clearFiles();
  file.uid = genFileId();
  upload.handleStart(file);
};

const handleClear = () => {
  uploadRef.value?.clearFiles();
  selectedFile.value = null;
};

const formatFileSize = (bytes) => {
  if (!bytes) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
};

const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件');
    return;
  }

  loading.value = true;

  try {
    const formData = new FormData();
    formData.append('file', selectedFile.value);

    const response = await api.post('/api/vacation/process', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      responseType: 'blob',
    });

    downloadExcelResponse(response);
    ElMessage.success('休假数据表生成完成，已开始下载');
  } catch (error) {
    console.error('生成休假数据表失败:', error);
    ElMessage.error(error.message || '生成失败，请检查文件格式或稍后再试');
  } finally {
    loading.value = false;
  }
};

const downloadExcelResponse = (response) => {
  const blob = response?.data;
  if (!(blob instanceof Blob)) {
    throw new TypeError('未收到可下载的Excel文件');
  }

  const filename =
    extractFilename(response) || `休假数据表_${Date.now().toString()}.xlsx`;

  const url = globalThis.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.rel = 'noopener noreferrer';
  document.body.appendChild(link);
  link.click();
  link.remove();
  globalThis.URL.revokeObjectURL(url);
};

const extractFilename = (response) => {
  const disposition = response?.headers?.['content-disposition'];
  if (!disposition) return null;

  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match && utf8Match[1]) {
    try {
      return decodeURIComponent(utf8Match[1]);
    } catch {
      return utf8Match[1];
    }
  }

  const fallbackMatch = disposition.match(/filename="?([^";]+)"?/i);
  if (fallbackMatch && fallbackMatch[1]) {
    try {
      return decodeURIComponent(fallbackMatch[1]);
    } catch {
      return fallbackMatch[1];
    }
  }

  return null;
};
</script>

<style lang="scss" scoped>
.vacation-split-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #eef2f7 100%);
  padding: 96px 0 64px;

  @media (max-width: 768px) {
    padding: 84px 0 48px;
  }
}

.page-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 24px;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 16px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-description {
  font-size: 1.125rem;
  color: #64748b;
  margin: 0;
}

.upload-section {
  margin-bottom: 24px;
}

.card-header-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1.125rem;
  font-weight: 600;
}

.process-hint {
  margin-bottom: 16px;
}

.upload-area {
  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    padding: 60px 20px;
    border: 2px dashed #d0d7de;
    border-radius: 12px;
    transition: all 0.3s ease;

    &:hover {
      border-color: #409eff;
      background-color: #f5f8ff;
    }
  }
}

.upload-icon {
  color: #409eff;
  margin-bottom: 16px;
}

.upload-text {
  color: #606266;
  font-size: 1rem;
  margin-bottom: 8px;

  em {
    color: #409eff;
    font-style: normal;
    font-weight: 600;
  }
}

.upload-tip {
  color: #909399;
  font-size: 0.875rem;
  line-height: 1.6;
  margin-top: 12px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-top: 16px;
  color: #606266;
}

.file-name {
  font-weight: 600;
}

.file-size {
  color: #909399;
  font-size: 0.875rem;
}

.upload-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
}
</style>
