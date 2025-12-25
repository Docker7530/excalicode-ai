<template>
  <div class="knowledge-page">
    <AppHeader />
    <div class="page-shell">
      <div class="view-switch">
        <button
          class="switch-card"
          :class="{ active: activePanel === 'upsert' }"
          type="button"
          @click="switchPanel('upsert')"
        >
          <ElIcon><DocumentAdd /></ElIcon>
          <div class="switch-text">
            <h3>知识入库</h3>
          </div>
        </button>
        <button
          class="switch-card"
          :class="{ active: activePanel === 'search' }"
          type="button"
          @click="switchPanel('search')"
        >
          <ElIcon><Search /></ElIcon>
          <div class="switch-text">
            <h3>知识检索</h3>
          </div>
        </button>
      </div>

      <Transition name="fade-slide" mode="out-in">
        <section
          v-if="activePanel === 'upsert'"
          key="upsert"
          class="panel-wrapper"
        >
          <ElCard class="card upsert-card">
            <ElForm
              ref="upsertFormRef"
              :model="upsertForm"
              :rules="upsertRules"
              label-width="96px"
              class="upsert-form"
            >
              <ElFormItem label="文档 ID" prop="documentId">
                <ElInput
                  v-model.trim="upsertForm.documentId"
                  placeholder="留空自动生成"
                  clearable
                />
              </ElFormItem>
              <ElFormItem label="标题" prop="title">
                <ElInput
                  v-model.trim="upsertForm.title"
                  placeholder="示例：支付通道-风控策略"
                  maxlength="128"
                  show-word-limit
                />
              </ElFormItem>
              <ElFormItem label="标签" prop="tags">
                <ElSelect
                  v-model="upsertForm.tags"
                  placeholder="输入后回车新增标签"
                  clearable
                  class="tag-select"
                  multiple
                  filterable
                  allow-create
                  default-first-option
                />
              </ElFormItem>
              <ElFormItem label="正文" prop="content">
                <ElInput
                  v-model="upsertForm.content"
                  type="textarea"
                  :rows="10"
                  placeholder="输入场景描述、历史案例或上下文"
                />
              </ElFormItem>
            </ElForm>

            <div class="form-actions">
              <ElButton
                type="primary"
                :loading="upserting"
                :icon="DocumentAdd"
                @click="handleUpsert"
              >
                提交入库
              </ElButton>
              <ElButton
                :disabled="upserting"
                :icon="RefreshRight"
                @click="resetUpsertForm"
              >
                重置
              </ElButton>
            </div>

            <ElDivider content-position="center" class="folder-import-divider">
              <span>或批量导入</span>
            </ElDivider>

            <section v-loading="folderImporting" class="folder-import">
              <div class="folder-import-header">
                <div>
                  <h4>一键导入知识文件夹</h4>
                  <p>自动扫描子目录中的 {{ folderAllowedHint }} 文件并向量化</p>
                </div>
                <div class="folder-import-actions">
                  <input
                    ref="folderInputRef"
                    class="folder-input"
                    type="file"
                    multiple
                    webkitdirectory
                    accept=".md,.MD,.txt,.TXT,.json,.JSON"
                    @change="handleFolderChange"
                  />
                  <ElButton
                    type="primary"
                    plain
                    :loading="folderImporting"
                    :icon="FolderOpened"
                    @click="triggerFolderPicker"
                  >
                    选择文件夹
                  </ElButton>
                </div>
              </div>

              <ElAlert
                v-if="folderImportResult"
                :closable="false"
                :type="folderImportAlertType"
                class="folder-import-alert"
              >
                {{ folderImportBanner }}
              </ElAlert>

              <ElDescriptions
                v-if="folderImportResult"
                :column="2"
                border
                size="small"
                class="folder-import-stats"
              >
                <ElDescriptionsItem label="文件夹">
                  {{ folderImportResult.folderName }}
                </ElDescriptionsItem>
                <ElDescriptionsItem label="扫描数">
                  {{ folderImportResult.totalFiles }} 个
                </ElDescriptionsItem>
                <ElDescriptionsItem label="可处理">
                  {{ folderImportResult.eligibleFiles }} 个
                </ElDescriptionsItem>
                <ElDescriptionsItem label="已入库">
                  {{ folderImportResult.ingestedFiles }} 个
                </ElDescriptionsItem>
              </ElDescriptions>

              <div
                v-if="folderFailedFiles.length"
                class="folder-import-results"
              >
                <p class="folder-result-title">以下文件未能入库</p>
                <div class="folder-import-files">
                  <div
                    v-for="item in folderFailedFiles"
                    :key="item.path"
                    class="folder-file-row error"
                  >
                    <span class="folder-file-path">{{ item.path }}</span>
                    <span class="folder-file-reason">{{
                      item.reason || '未知错误'
                    }}</span>
                  </div>
                </div>
              </div>

              <p class="folder-tip">
                小提示：不符合条件的文件会被自动跳过，建议文件名保持语义化便于检索
              </p>
            </section>
          </ElCard>
        </section>
        <section v-else key="search" class="panel-wrapper">
          <ElCard class="card search-card">
            <ElForm :model="searchForm" label-width="92px" class="search-form">
              <ElFormItem label="查询" required>
                <ElInput
                  v-model.trim="searchForm.query"
                  placeholder="输入需求摘要、关键词..."
                  clearable
                  @keyup.enter="handleSearch"
                />
              </ElFormItem>
              <div class="inline-field-group">
                <div class="inline-item">
                  <ElFormItem label="TopK">
                    <ElInputNumber
                      v-model="searchForm.topK"
                      :min="1"
                      :max="10"
                      :step="1"
                      controls-position="right"
                    />
                  </ElFormItem>
                </div>
                <div class="inline-item">
                  <ElFormItem label="相似度">
                    <ElInputNumber
                      v-model="searchForm.minScore"
                      :min="0"
                      :max="1"
                      :step="0.05"
                      :precision="2"
                      controls-position="right"
                    />
                  </ElFormItem>
                </div>
              </div>
            </ElForm>

            <div class="form-actions">
              <ElButton
                type="primary"
                :loading="searching"
                :icon="Search"
                @click="handleSearch"
              >
                执行检索
              </ElButton>
            </div>

            <ElAlert
              v-if="hasSearched"
              type="info"
              :closable="false"
              class="result-tip"
            >
              {{ resultBanner }}
            </ElAlert>

            <div v-loading="searching" class="result-list">
              <template v-if="searchResults.length">
                <article
                  v-for="(item, index) in searchResults"
                  :key="`${item.documentId || 'doc'}-${item.chunkIndex}-${index}`"
                  class="result-item"
                >
                  <header class="result-meta">
                    <div>
                      <h4>{{ formatTitle(item, index) }}</h4>
                      <p class="result-subtitle">
                        ID：{{ item.documentId || '自动生成' }} · 片段 #{
                        item.chunkIndex + 1 }
                      </p>
                    </div>
                  </header>
                  <div v-if="item.tags?.length" class="tag-group">
                    <ElTag
                      v-for="tag in item.tags"
                      :key="tag"
                      size="small"
                      effect="light"
                    >
                      {{ tag }}
                    </ElTag>
                  </div>
                  <p class="result-content">{{ item.content }}</p>
                  <div class="result-actions">
                    <ElButton
                      type="primary"
                      text
                      size="small"
                      :icon="DocumentCopy"
                      @click="copyChunk(item.content)"
                    >
                      复制片段
                    </ElButton>
                  </div>
                </article>
              </template>
              <ElEmpty
                v-else-if="hasSearched && !searching"
                description="暂无检索结果"
                class="result-empty"
              />
            </div>
          </ElCard>
        </section>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue';
import {
  importFolderKnowledge,
  searchKnowledgeDocuments,
  upsertKnowledgeDocument,
} from '@/api/requirementKnowledge.js';
import {
  DocumentAdd,
  DocumentCopy,
  FolderOpened,
  RefreshRight,
  Search,
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { computed, reactive, ref } from 'vue';

const activePanel = ref('upsert');
const switchPanel = (panel) => {
  activePanel.value = panel;
};

const upsertFormRef = ref(null);
const upsertForm = reactive({
  documentId: '',
  title: '',
  tags: [],
  content: '',
});
const upsertRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文内容', trigger: 'blur' }],
};
const upserting = ref(false);

const resetUpsertForm = () => {
  upsertForm.documentId = '';
  upsertForm.title = '';
  upsertForm.tags = [];
  upsertForm.content = '';
};

const handleUpsert = async () => {
  if (!upsertFormRef.value) return;
  try {
    await upsertFormRef.value.validate();
    upserting.value = true;
    await upsertKnowledgeDocument({
      documentId: upsertForm.documentId || undefined,
      title: upsertForm.title,
      tags: upsertForm.tags,
      content: upsertForm.content,
    });
    ElMessage.success('知识文档已入库');
    resetUpsertForm();
  } catch (error) {
    if (error?.name === 'ElFormError') {
      return;
    }
    if (error?.response?.data?.message) {
      ElMessage.error(error.response.data.message);
    } else if (error?.message) {
      ElMessage.error(error.message);
    }
  } finally {
    upserting.value = false;
  }
};

const searchForm = reactive({
  query: '',
  topK: 4,
  minScore: 0.65,
});
const searching = ref(false);
const hasSearched = ref(false);
const searchResults = ref([]);
const lastSearchedAt = ref('');

const resultBanner = computed(() => {
  if (!hasSearched.value) return '';
  const ts = lastSearchedAt.value ? ` · ${lastSearchedAt.value}` : '';
  return `共返回 ${searchResults.value.length} 条片段${ts}`;
});

const formatTitle = (item, index) => {
  if (item?.title) return item.title;
  return `知识片段 ${index + 1}`;
};

const copyChunk = async (text) => {
  try {
    await navigator.clipboard.writeText(text || '');
    ElMessage.success('片段已复制到剪贴板');
  } catch (error) {
    console.error('复制失败', error);
    ElMessage.error('复制失败，请手动选择文本');
  }
};

const formatTimestamp = (date = new Date()) => {
  const pad = (val) => String(val).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
    date.getDate(),
  )} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(
    date.getSeconds(),
  )}`;
};

const handleSearch = async () => {
  if (!searchForm.query?.trim()) {
    ElMessage.warning('请输入查询内容');
    return;
  }
  searching.value = true;
  try {
    const payload = {
      query: searchForm.query.trim(),
      topK: searchForm.topK,
      minScore:
        typeof searchForm.minScore === 'number'
          ? Number(searchForm.minScore)
          : undefined,
    };
    const data = await searchKnowledgeDocuments(payload);
    searchResults.value = Array.isArray(data) ? data : [];
    hasSearched.value = true;
    lastSearchedAt.value = formatTimestamp();
  } catch (error) {
    console.error('知识检索失败', error);
    ElMessage.error(error?.message || '知识检索失败');
  } finally {
    searching.value = false;
  }
};

const folderInputRef = ref(null);
const folderImporting = ref(false);
const folderImportResult = ref(null);
const folderAllowedExtensions = ['md', 'txt', 'json'];
const folderAllowedHint = folderAllowedExtensions
  .map((ext) => `.${ext}`)
  .join(' / ');
const FOLDER_UPLOAD_BATCH_SIZE = 10;

const chunkFiles = (files, chunkSize = FOLDER_UPLOAD_BATCH_SIZE) => {
  const safeSize = Math.max(1, chunkSize);
  const chunks = [];
  for (let i = 0; i < files.length; i += safeSize) {
    chunks.push(files.slice(i, i + safeSize));
  }
  return chunks;
};

const mergeImportResponses = (current, incoming) => {
  if (!incoming) {
    return current;
  }
  if (!current) {
    return {
      ...incoming,
      fileResults: Array.isArray(incoming.fileResults)
        ? [...incoming.fileResults]
        : [],
    };
  }
  return {
    folderName: incoming.folderName || current.folderName,
    totalFiles: (current.totalFiles || 0) + (incoming.totalFiles || 0),
    eligibleFiles: (current.eligibleFiles || 0) + (incoming.eligibleFiles || 0),
    ingestedFiles: (current.ingestedFiles || 0) + (incoming.ingestedFiles || 0),
    skippedFiles: (current.skippedFiles || 0) + (incoming.skippedFiles || 0),
    fileResults: [
      ...(current.fileResults || []),
      ...(incoming.fileResults || []),
    ],
  };
};

const folderFailedFiles = computed(() => {
  const results = folderImportResult.value?.fileResults || [];
  return results.filter((item) => item && !item.ingested);
});

const folderImportBanner = computed(() => {
  if (!folderImportResult.value) {
    return '';
  }
  const {
    ingestedFiles = 0,
    eligibleFiles = 0,
    skippedFiles = 0,
  } = folderImportResult.value;
  return `成功写入 ${ingestedFiles}/${eligibleFiles} 个文件，跳过 ${skippedFiles} 个`;
});

const folderImportAlertType = computed(() =>
  folderFailedFiles.value.length ? 'warning' : 'success',
);

const resolveExtension = (filename = '') => {
  const lastDot = filename.lastIndexOf('.');
  if (lastDot < 0) {
    return '';
  }
  return filename.slice(lastDot + 1).toLowerCase();
};

const deriveFolderName = (file) => {
  if (!file) {
    return 'folder-upload';
  }
  const relative = file.webkitRelativePath || '';
  if (relative.includes('/')) {
    return relative.split('/')[0] || 'folder-upload';
  }
  const base = file.name || 'folder-upload';
  const dotIndex = base.lastIndexOf('.');
  return dotIndex > 0 ? base.slice(0, dotIndex) : base;
};

const resetFolderInput = (input) => {
  if (input) {
    input.value = '';
  }
};

const triggerFolderPicker = () => {
  folderImportResult.value = null;
  folderInputRef.value?.click();
};

const handleFolderChange = async (event) => {
  const target = event?.target;
  const files = Array.from(target?.files || []);
  folderImportResult.value = null;
  if (!files.length) {
    return;
  }
  const eligibleFiles = files.filter((file) =>
    folderAllowedExtensions.includes(resolveExtension(file.name)),
  );
  if (!eligibleFiles.length) {
    ElMessage.warning('该文件夹中没有 .md/.txt/.json 文档');
    resetFolderInput(target);
    return;
  }

  const folderName = deriveFolderName(eligibleFiles[0]);
  const fileChunks = chunkFiles(eligibleFiles, FOLDER_UPLOAD_BATCH_SIZE);
  folderImporting.value = true;
  let aggregatedResult = null;
  let currentBatchIndex = 0;

  try {
    for (const chunk of fileChunks) {
      currentBatchIndex += 1;
      const formData = new FormData();
      formData.append('folderName', folderName);
      chunk.forEach((file) => {
        const relativePath = file.webkitRelativePath || file.name;
        formData.append('files', file, relativePath);
      });
      const result = await importFolderKnowledge(formData);
      aggregatedResult = mergeImportResponses(aggregatedResult, result);
    }

    folderImportResult.value = aggregatedResult;
    const successCount = aggregatedResult?.ingestedFiles ?? 0;
    const eligibleCount =
      aggregatedResult?.eligibleFiles ?? eligibleFiles.length;
    ElMessage.success(
      `文件夹入库完成：${successCount}/${eligibleCount} 个文件`,
    );
  } catch (error) {
    console.error('文件夹批量导入失败', error);
    if (aggregatedResult) {
      folderImportResult.value = aggregatedResult;
    }
    const batchMessage =
      currentBatchIndex > 0 ? `第 ${currentBatchIndex} 批上传失败` : null;
    const detailMessage =
      error?.response?.data?.message ||
      error?.message ||
      batchMessage ||
      '文件夹导入失败';
    ElMessage.error(detailMessage);
  } finally {
    folderImporting.value = false;
    resetFolderInput(target);
  }
};
</script>

<style scoped lang="scss">
.knowledge-page {
  min-height: 100vh;
  background: #ffffff;
  padding: 80px 24px 40px;
}

@media (max-width: 768px) {
  .knowledge-page {
    padding: 64px 16px 32px;
  }
}

.page-shell {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.view-switch {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.switch-card {
  border: 1px solid rgba(79, 70, 229, 0.2);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  padding: 16px 20px;
  display: flex;
  gap: 16px;
  align-items: center;
  transition: all 0.25s ease;
  cursor: pointer;
  color: #0f172a;
}

.switch-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(79, 70, 229, 0.15);
}

.switch-card.active {
  border-color: transparent;
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  color: #ffffff;
  box-shadow: 0 14px 36px rgba(64, 158, 255, 0.25);
}

.switch-card.active .switch-text h3 {
  color: inherit;
}

.switch-text h3 {
  margin: 0;
  font-size: 18px;
  color: #0f172a;
}

.panel-wrapper {
  animation: scaleIn 0.3s ease;
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.card {
  border-radius: 18px;
  box-shadow: 0 20px 50px rgba(79, 70, 229, 0.12);
  border: none;
}

.upsert-form,
.search-form {
  margin-top: 12px;
}

.inline-field-group {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.inline-item {
  flex: 1;
  min-width: 220px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.tag-select {
  width: 100%;
}

.result-tip {
  margin-top: 16px;
}

.result-list {
  margin-top: 16px;
  min-height: 200px;
}

.result-item {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 16px 18px;
  background: #fff;
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.result-meta {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;

  h4 {
    margin: 0;
    font-size: 1.1rem;
    font-weight: 600;
    color: #1f2937;
  }

  .result-subtitle {
    margin: 4px 0 0;
    font-size: 0.85rem;
    color: #94a3b8;
  }
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 10px 0;
}

.result-content {
  margin: 6px 0 0;
  color: #374151;
  line-height: 1.6;
  white-space: pre-wrap;
}

.result-actions {
  margin-top: 8px;
  text-align: right;
}

.result-empty {
  padding: 32px 0;
}

@media (max-width: 768px) {
  .form-actions {
    flex-direction: column;
    align-items: stretch;
  }
}

.folder-import-divider {
  margin: 32px 0 16px;
  color: #94a3b8;
}

.folder-import {
  border: 1px dashed #c7d2fe;
  border-radius: 16px;
  padding: 20px;
  background: rgba(79, 70, 229, 0.02);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.folder-import-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;

  h4 {
    margin: 0;
    font-size: 1.05rem;
    color: #1f2937;
  }

  p {
    margin: 4px 0 0;
    color: #6b7280;
    font-size: 0.9rem;
  }
}

.folder-import-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.folder-input {
  display: none;
}

.folder-import-alert {
  margin-top: 4px;
}

.folder-import-stats {
  margin-top: 4px;
}

.folder-import-results {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px 14px;
  background: #fff;
}

.folder-result-title {
  margin: 0 0 8px;
  font-size: 0.9rem;
  color: #b45309;
}

.folder-import-files {
  max-height: 220px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.folder-file-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 0.85rem;
  color: #16a34a;
}

.folder-file-row.error {
  color: #dc2626;
}

.folder-file-path {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.folder-file-reason {
  flex-shrink: 0;
}

.folder-tip {
  margin: 0;
  font-size: 0.85rem;
  color: #6b7280;
}
</style>
