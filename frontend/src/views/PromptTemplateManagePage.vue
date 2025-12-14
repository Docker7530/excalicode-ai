<template>
  <div class="prompt-template-manage-page">
    <AppHeader />

    <ElContainer class="main-container">
      <!-- 左侧：提示词列表 -->
      <ElAside width="350px" class="prompt-list-aside">
        <div class="list-header">
          <h3>模板列表</h3>
          <ElButton
            type="primary"
            :icon="Plus"
            size="small"
            round
            @click="handleCreate"
          >
            新建
          </ElButton>
        </div>

        <ElInput
          v-model="searchKeyword"
          placeholder="搜索提示词..."
          clearable
          :prefix-icon="Search"
          class="search-input"
          @input="handleSearch"
        />

        <ElScrollbar class="prompt-list-scrollbar">
          <div
            v-for="template in filteredTemplates"
            :key="template.id"
            class="prompt-item"
            :class="{ active: selectedTemplate?.id === template.id }"
            @click="handleSelectTemplate(template)"
          >
            <div class="prompt-item-header">
              <span class="prompt-name">{{ template.name }}</span>
              <ElTag size="small" type="info">{{ template.code }}</ElTag>
            </div>
            <div class="prompt-meta">
              <span class="meta-item"
                >更新于 {{ formatDate(template.updatedTime) }}</span
              >
            </div>
          </div>

          <ElEmpty
            v-if="filteredTemplates.length === 0"
            description="暂无提示词"
          />
        </ElScrollbar>
      </ElAside>

      <!-- 右侧：编辑器 -->
      <ElMain class="editor-main">
        <div v-show="selectedTemplate" class="editor-container">
          <div class="editor-header">
            <h2>{{ isCreating ? '新建提示词' : '编辑提示词' }}</h2>
            <div class="editor-actions">
              <ElButton
                v-if="!isCreating"
                type="danger"
                :icon="Delete"
                size="small"
                @click="handleDelete"
              >
                删除
              </ElButton>
              <ElButton size="small" @click="handleCancel">取消</ElButton>
              <ElButton
                type="primary"
                :loading="saving"
                size="small"
                @click="handleSave"
              >
                保存
              </ElButton>
            </div>
          </div>

          <div class="editor-form">
            <ElForm
              :model="formData"
              label-width="100px"
              class="basic-info-form"
            >
              <ElRow :gutter="20">
                <ElCol :span="12">
                  <ElFormItem label="提示词代码" required>
                    <ElInput
                      v-model="formData.code"
                      placeholder="唯一标识，如: REQUIREMENT_DOC_GENERATOR"
                      :disabled="!isCreating && selectedTemplate?.id"
                    />
                  </ElFormItem>
                </ElCol>
                <ElCol :span="12">
                  <ElFormItem label="提示词名称" required>
                    <ElInput v-model="formData.name" placeholder="显示名称" />
                  </ElFormItem>
                </ElCol>
              </ElRow>
            </ElForm>

            <!-- Vditor 编辑器 - 占据剩余全部空间 -->
            <div class="editor-content-wrapper">
              <div class="editor-content-header">
                <span class="required-mark">*</span>
                <span>模板内容</span>
                <ElText type="info" size="small">支持 Markdown 格式</ElText>
              </div>
              <div id="vditor" class="vditor-wrapper"></div>
            </div>
          </div>
        </div>

        <ElEmpty
          v-show="!selectedTemplate"
          description="请选择或新建一个提示词模板"
          :image-size="200"
        />
      </ElMain>
    </ElContainer>
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue';
import { Delete, Plus, Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import Vditor from 'vditor';
import 'vditor/dist/index.css';
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue';
import {
  createPromptTemplate,
  deletePromptTemplate,
  listPromptTemplates,
  searchPromptTemplates,
  updatePromptTemplate,
} from '../api/promptTemplate.js';

// Vditor 实例
let vditorInstance = null;

// 数据
const templates = ref([]);
const selectedTemplate = ref(null);
const searchKeyword = ref('');
const isCreating = ref(false);
const saving = ref(false);

const formData = reactive({
  id: null,
  code: '',
  name: '',
  content: '',
});

// 计算属性
const filteredTemplates = computed(() => {
  if (!searchKeyword.value) {
    return templates.value;
  }
  return templates.value.filter(
    (t) =>
      t.name.includes(searchKeyword.value) ||
      t.code.includes(searchKeyword.value),
  );
});

// 初始化 Vditor 编辑器
const initVditor = () => {
  if (vditorInstance) {
    vditorInstance.destroy();
    vditorInstance = null;
  }

  nextTick(() => {
    vditorInstance = new Vditor('vditor', {
      height: '100%',
      placeholder: '请输入 Markdown 格式的提示词内容...',
      cache: {
        enable: false,
      },
      input: (value) => {
        formData.content = value;
      },
    });
  });
};

// 监听选中模板变化，更新编辑器内容
watch(selectedTemplate, (newTemplate) => {
  if (vditorInstance && newTemplate && vditorInstance.vditor) {
    nextTick(() => {
      vditorInstance.setValue(formData.content || '');
    });
  }
});

// 方法
const loadTemplates = async () => {
  try {
    const response = await listPromptTemplates();
    templates.value = response || [];
  } catch (error) {
    ElMessage.error('加载提示词列表失败: ' + error.message);
  }
};

const handleSearch = async () => {
  if (!searchKeyword.value) {
    await loadTemplates();
    return;
  }

  try {
    const response = await searchPromptTemplates(searchKeyword.value);
    templates.value = response || [];
  } catch (error) {
    ElMessage.error('搜索失败: ' + error.message);
  }
};

const handleSelectTemplate = (template) => {
  selectedTemplate.value = template;
  isCreating.value = false;
  Object.assign(formData, {
    id: template.id,
    code: template.code,
    name: template.name,
    content: template.content,
  });

  // 更新编辑器内容
  nextTick(() => {
    if (vditorInstance && vditorInstance.vditor) {
      vditorInstance.setValue(template.content || '');
    }
  });
};

const handleCreate = () => {
  isCreating.value = true;
  selectedTemplate.value = { id: null };
  Object.assign(formData, {
    id: null,
    code: '',
    name: '',
    content: '',
  });

  // 清空编辑器内容
  nextTick(() => {
    if (vditorInstance && vditorInstance.vditor) {
      vditorInstance.setValue('');
    }
  });
};

const handleCancel = () => {
  if (isCreating.value) {
    selectedTemplate.value = null;
    isCreating.value = false;
  } else if (selectedTemplate.value) {
    // 恢复原始数据
    handleSelectTemplate(selectedTemplate.value);
  }
};

const handleSave = async () => {
  // 确保获取最新的编辑器内容
  if (vditorInstance && vditorInstance.vditor) {
    formData.content = vditorInstance.getValue();
  }

  if (!formData.code || !formData.name || !formData.content) {
    ElMessage.warning('请填写必填字段');
    return;
  }

  saving.value = true;
  try {
    if (isCreating.value) {
      await createPromptTemplate(formData);
      ElMessage.success('创建成功');
    } else {
      await updatePromptTemplate(formData.id, formData);
      ElMessage.success('保存成功');
    }
    await loadTemplates();
    isCreating.value = false;
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message);
  } finally {
    saving.value = false;
  }
};

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm(
      '确定删除该提示词模板吗？此操作不可恢复',
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    );

    await deletePromptTemplate(selectedTemplate.value.id);
    ElMessage.success('删除成功');
    selectedTemplate.value = null;
    await loadTemplates();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message);
    }
  }
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

// 生命周期
onMounted(() => {
  loadTemplates();
  initVditor();
});

onBeforeUnmount(() => {
  // 销毁编辑器实例
  if (vditorInstance) {
    vditorInstance.destroy();
    vditorInstance = null;
  }
});
</script>

<style scoped lang="scss">
.prompt-template-manage-page {
  min-height: 100vh;
  padding: 120px 24px 40px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  background: linear-gradient(135deg, #f8fbff 0%, #eef2ff 60%, #e0e7ff 100%);

  @media (max-width: 768px) {
    padding: 100px 16px 32px;
  }

  // 主容器
  .main-container {
    flex: 1;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    overflow: hidden;
    height: calc(100vh - 220px);
    position: relative;
  }

  // 左侧列表
  .prompt-list-aside {
    border-right: 1px solid #f0f0f0;
    display: flex;
    flex-direction: column;
    background: #fafbfc;
    overflow: hidden;

    .list-header {
      padding: 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid #f0f0f0;
      background: #fff;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }

    .search-input {
      padding: 16px;
      background: #fff;
      border-bottom: 1px solid #f0f0f0;
    }

    .prompt-list-scrollbar {
      flex: 1;
      padding: 12px;
      overflow-y: auto;

      .prompt-item {
        padding: 14px;
        margin-bottom: 8px;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
        border: 1px solid transparent;
        background: #fff;

        &:hover {
          border-color: #d0d0d0;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
          transform: translateY(-1px);
        }

        &.active {
          background: linear-gradient(135deg, #f0f7ff 0%, #e8f4ff 100%);
          border-color: #409eff;
          box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
        }

        .prompt-item-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;
          gap: 8px;

          .prompt-name {
            font-weight: 600;
            font-size: 14px;
            color: #303133;
            flex: 1;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }

        .prompt-meta {
          font-size: 11px;
          color: #c0c4cc;

          .meta-item {
            margin-right: 12px;
          }
        }
      }
    }
  }

  // 右侧编辑器
  .editor-main {
    padding: 0;
    overflow: hidden;

    .editor-container {
      height: 100%;
      display: flex;
      flex-direction: column;

      .editor-header {
        padding: 20px 24px;
        border-bottom: 1px solid #f0f0f0;
        display: flex;
        justify-content: space-between;
        align-items: center;
        background: #fafbfc;
        flex-shrink: 0;

        h2 {
          margin: 0;
          font-size: 18px;
          font-weight: 600;
          color: #303133;
        }

        .editor-actions {
          display: flex;
          gap: 8px;
        }
      }

      .editor-form {
        flex: 1;
        padding: 24px;
        overflow-y: auto;
        display: flex;
        flex-direction: column;
        gap: 16px;

        .basic-info-form {
          flex-shrink: 0;
        }

        .editor-content-wrapper {
          flex: 1;
          display: flex;
          flex-direction: column;
          min-height: 400px;

          .editor-content-header {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 12px;
            font-size: 14px;
            font-weight: 500;
            color: #606266;
            flex-shrink: 0;

            .required-mark {
              color: #f56c6c;
              font-weight: bold;
            }
          }

          .vditor-wrapper {
            flex: 1;
            border: 1px solid #dcdfe6;
            border-radius: 8px;
            overflow: hidden;

            :deep(.vditor) {
              border: none;
              height: 100%;
            }

            // 让工具栏圆角与容器匹配
            :deep(.vditor-toolbar) {
              border-radius: 8px 8px 0 0;
            }

            // 确保面板和 tooltip 正常显示
            :deep(.vditor-panel),
            :deep(.vditor-hint) {
              z-index: 1000 !important;
            }

            :deep(.vditor-tooltipped::after),
            :deep(.vditor-tooltipped::before) {
              z-index: 1001 !important;
            }
          }
        }
      }
    }
  }
}
</style>
