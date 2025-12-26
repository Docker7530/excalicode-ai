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
  searchKnowledgeDocuments,
  upsertKnowledgeDocument,
} from '@/api/requirementKnowledge.js';
import {
  DocumentAdd,
  DocumentCopy,
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
</style>
