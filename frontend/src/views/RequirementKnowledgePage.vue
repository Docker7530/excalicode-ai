<template>
  <div class="knowledge-page">
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
          </ElCard>

          <ElCard class="card entries-card">
            <template #header>
              <div class="entries-header">
                <div>
                  <h3 class="entries-title">已保存条目</h3>
                  <p class="entries-subtitle">
                    这里保存的是数据库草稿；需要参与检索请手动点「向量化」或「一键向量」
                  </p>
                </div>
                <div class="entries-actions">
                  <ElUpload
                    :auto-upload="false"
                    :show-file-list="false"
                    accept=".xlsx,.xls"
                    :on-change="handleImportChange"
                  >
                    <ElButton
                      type="primary"
                      :loading="importingEntries"
                      :disabled="entriesLoading || vectorizingAll"
                    >
                      一键导入
                    </ElButton>
                  </ElUpload>
                  <ElButton
                    type="success"
                    :loading="vectorizingAll"
                    :disabled="entriesLoading || importingEntries"
                    @click="handleVectorizeAll"
                  >
                    一键向量
                  </ElButton>
                  <ElButton
                    :icon="RefreshRight"
                    :loading="entriesLoading"
                    @click="fetchEntries"
                  >
                    刷新
                  </ElButton>
                </div>
              </div>
            </template>

            <ElTable
              v-loading="entriesLoading"
              :data="knowledgeEntries"
              row-key="documentId"
              class="entries-table"
              empty-text="暂无数据"
            >
              <ElTableColumn prop="documentId" label="文档ID" width="260" />
              <ElTableColumn label="标题" min-width="240" show-overflow-tooltip>
                <template #default="{ row }">
                  <ElButton link type="primary" @click="openEdit(row)">
                    {{ row.title }}
                  </ElButton>
                </template>
              </ElTableColumn>
              <ElTableColumn label="标签" min-width="180">
                <template #default="{ row }">
                  <div v-if="row.tags?.length" class="table-tags">
                    <ElTag
                      v-for="tag in row.tags"
                      :key="tag"
                      size="small"
                      effect="light"
                    >
                      {{ tag }}
                    </ElTag>
                  </div>
                  <span v-else class="table-empty">-</span>
                </template>
              </ElTableColumn>
              <ElTableColumn label="向量" width="90">
                <template #default="{ row }">
                  <ElTag
                    size="small"
                    :type="row.vectorized === 1 ? 'success' : 'info'"
                  >
                    {{ row.vectorized === 1 ? '已向量' : '未向量' }}
                  </ElTag>
                </template>
              </ElTableColumn>
              <ElTableColumn prop="updatedTime" label="更新时间" width="170" />
              <ElTableColumn label="操作" width="260" fixed="right">
                <template #default="{ row }">
                  <ElButton
                    size="small"
                    type="primary"
                    plain
                    :loading="vectorizingId === row.documentId"
                    @click="handleVectorize(row)"
                  >
                    向量化
                  </ElButton>
                  <ElButton
                    size="small"
                    type="warning"
                    plain
                    :loading="deletingVectorId === row.documentId"
                    @click="handleDeleteVector(row)"
                  >
                    删除向量
                  </ElButton>
                  <ElButton
                    size="small"
                    type="danger"
                    plain
                    :loading="deletingEntryId === row.documentId"
                    @click="handleDeleteEntry(row)"
                  >
                    删除数据
                  </ElButton>
                </template>
              </ElTableColumn>
            </ElTable>
          </ElCard>

          <ElDialog
            v-model="editDialogVisible"
            title="编辑知识条目"
            width="760px"
          >
            <ElAlert
              type="warning"
              :closable="false"
              class="edit-alert"
              title="只修改数据库内容，不会自动更新向量；如需检索请重新向量化"
            />
            <ElForm
              ref="editFormRef"
              :model="editForm"
              :rules="editRules"
              label-width="96px"
            >
              <ElFormItem label="文档 ID">
                <ElInput v-model="editForm.documentId" disabled />
              </ElFormItem>
              <ElFormItem label="标题" prop="title">
                <ElInput
                  v-model.trim="editForm.title"
                  maxlength="128"
                  show-word-limit
                />
              </ElFormItem>
              <ElFormItem label="标签">
                <ElSelect
                  v-model="editForm.tags"
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
                  v-model="editForm.content"
                  type="textarea"
                  :rows="10"
                />
              </ElFormItem>
            </ElForm>
            <template #footer>
              <ElButton @click="editDialogVisible = false">取消</ElButton>
              <ElButton
                type="primary"
                :loading="savingEdit"
                @click="handleSaveEdit"
              >
                保存
              </ElButton>
            </template>
          </ElDialog>
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
import {
  deleteKnowledgeEntry,
  deleteKnowledgeVector,
  importKnowledgeEntries,
  listKnowledgeEntries,
  searchKnowledgeDocuments,
  updateKnowledgeEntry,
  upsertKnowledgeDocument,
  vectorizeAllKnowledgeEntries,
  vectorizeKnowledgeEntry,
} from '@/api/requirementKnowledge.js';
import {
  DocumentAdd,
  DocumentCopy,
  RefreshRight,
  Search,
} from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

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
    ElMessage.success('已保存到数据库（未向量化）');
    resetUpsertForm();
    await fetchEntries();
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

const entriesLoading = ref(false);
const knowledgeEntries = ref([]);
const importingEntries = ref(false);
const vectorizingAll = ref(false);

const fetchEntries = async () => {
  entriesLoading.value = true;
  try {
    const data = await listKnowledgeEntries();
    knowledgeEntries.value = Array.isArray(data) ? data : [];
  } catch (error) {
    console.error('获取知识条目失败', error);
    ElMessage.error(error?.message || '获取知识条目失败');
  } finally {
    entriesLoading.value = false;
  }
};

const handleImportChange = async (uploadFile) => {
  const raw = uploadFile?.raw ?? uploadFile;
  if (!raw) return;
  try {
    importingEntries.value = true;
    const result = await importKnowledgeEntries(raw);
    const successCount = result?.successCount ?? 0;
    const failedCount = result?.failedCount ?? 0;
    const skippedCount = result?.skippedCount ?? 0;

    if (failedCount > 0) {
      const messages = (result?.errors ?? [])
        .slice(0, 3)
        .map((item) => `第${item.rowIndex}行：${item.message}`)
        .join('；');
      ElMessage.warning(
        `导入完成：成功${successCount}条，失败${failedCount}条，跳过${skippedCount}行。${messages}`,
      );
    } else {
      ElMessage.success(
        `导入完成：成功${successCount}条，跳过${skippedCount}行`,
      );
    }
    await fetchEntries();
  } catch (error) {
    console.error('导入失败', error);
    ElMessage.error(error?.message || '导入失败');
  } finally {
    importingEntries.value = false;
  }
};

const handleVectorizeAll = async () => {
  try {
    await ElMessageBox.confirm(
      '确认对数据库中所有未向量化的条目执行向量化？该操作会在后台异步执行。',
      '一键向量确认',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' },
    );
    vectorizingAll.value = true;
    await vectorizeAllKnowledgeEntries();
    ElMessage.success('已提交后台向量化任务，请稍后点击刷新查看最新状态');
  } catch (error) {
    if (error === 'cancel' || error === 'close') return;
    console.error('一键向量失败', error);
    ElMessage.error(error?.message || '一键向量失败');
  } finally {
    vectorizingAll.value = false;
  }
};

const vectorizingId = ref('');
const deletingVectorId = ref('');
const deletingEntryId = ref('');

const handleVectorize = async (row) => {
  if (!row?.documentId) return;
  try {
    await ElMessageBox.confirm(
      `确认对文档 ${row.documentId} 执行向量化？`,
      '向量化确认',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' },
    );
    vectorizingId.value = row.documentId;
    await vectorizeKnowledgeEntry(row.documentId);
    ElMessage.success('向量化完成');
    await fetchEntries();
  } catch (error) {
    if (error === 'cancel' || error === 'close') return;
    console.error('向量化失败', error);
    ElMessage.error(error?.message || '向量化失败');
  } finally {
    vectorizingId.value = '';
  }
};

const handleDeleteVector = async (row) => {
  if (!row?.documentId) return;
  try {
    await ElMessageBox.confirm(
      `确认删除文档 ${row.documentId} 的向量？数据库条目会保留。`,
      '删除向量确认',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' },
    );
    deletingVectorId.value = row.documentId;
    await deleteKnowledgeVector(row.documentId);
    ElMessage.success('向量已删除');
    await fetchEntries();
  } catch (error) {
    if (error === 'cancel' || error === 'close') return;
    console.error('删除向量失败', error);
    ElMessage.error(error?.message || '删除向量失败');
  } finally {
    deletingVectorId.value = '';
  }
};

const handleDeleteEntry = async (row) => {
  if (!row?.documentId) return;
  try {
    await ElMessageBox.confirm(
      `确认删除文档 ${row.documentId}？该操作会同时尝试删除向量。`,
      '删除数据确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' },
    );
    deletingEntryId.value = row.documentId;
    await deleteKnowledgeEntry(row.documentId);
    ElMessage.success('数据已删除');
    await fetchEntries();
  } catch (error) {
    if (error === 'cancel' || error === 'close') return;
    console.error('删除数据失败', error);
    ElMessage.error(error?.message || '删除数据失败');
  } finally {
    deletingEntryId.value = '';
  }
};

const editDialogVisible = ref(false);
const editFormRef = ref(null);
const editForm = reactive({
  documentId: '',
  title: '',
  tags: [],
  content: '',
});
const editRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文内容', trigger: 'blur' }],
};
const savingEdit = ref(false);

const openEdit = (row) => {
  if (!row) return;
  editForm.documentId = row.documentId;
  editForm.title = row.title;
  editForm.tags = Array.isArray(row.tags) ? [...row.tags] : [];
  editForm.content = row.content;
  editDialogVisible.value = true;
};

const handleSaveEdit = async () => {
  if (!editFormRef.value) return;
  try {
    await editFormRef.value.validate();
    savingEdit.value = true;
    await updateKnowledgeEntry(editForm.documentId, {
      title: editForm.title,
      content: editForm.content,
      tags: editForm.tags,
    });
    ElMessage.success('已保存（未向量化）');
    editDialogVisible.value = false;
    await fetchEntries();
  } catch (error) {
    if (error?.name === 'ElFormError') {
      return;
    }
    console.error('保存失败', error);
    ElMessage.error(error?.message || '保存失败');
  } finally {
    savingEdit.value = false;
  }
};

onMounted(() => {
  fetchEntries();
});

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
</script>

<style scoped lang="scss">
.knowledge-page {
  min-height: 100vh;
  background: #ffffff;
  padding: 96px 24px 40px;
}

@media (max-width: 768px) {
  .knowledge-page {
    padding: 84px 16px 32px;
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

.entries-card {
  margin-top: 20px;
}

.entries-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.entries-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.entries-subtitle {
  margin: 4px 0 0;
  font-size: 0.85rem;
  color: #94a3b8;
}

.entries-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.entries-table {
  width: 100%;
}

.table-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.table-empty {
  color: #94a3b8;
}

.edit-alert {
  margin-bottom: 12px;
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
</style>
