<template>
  <div class="ai-provider-page">
    <!-- 统计卡片 -->
    <div class="stats-section">
      <div class="stats-container">
        <div class="stat-card">
          <div class="stat-icon provider-icon">
            <ElIcon><OfficeBuilding /></ElIcon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ providers.length }}</div>
            <div class="stat-label">AI 厂商</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon model-icon">
            <ElIcon><Grid /></ElIcon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalModels }}</div>
            <div class="stat-label">AI 模型</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon key-icon">
            <ElIcon><Key /></ElIcon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ providers.length }}</div>
            <div class="stat-label">API 配置</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="toolbar-section">
      <div class="toolbar-container">
        <ElButton
          type="primary"
          :icon="Plus"
          size="large"
          class="add-btn"
          @click="handleAddProvider"
        >
          新增厂商
        </ElButton>
      </div>
    </div>

    <!-- 厂商列表 -->
    <div v-loading="loading" class="providers-section">
      <div class="providers-container">
        <div
          v-for="provider in providers"
          :key="provider.id"
          class="provider-card"
        >
          <!-- 卡片头部 -->
          <div class="provider-header">
            <div class="provider-title-wrapper">
              <div class="provider-logo">
                <ElIcon><Grid /></ElIcon>
              </div>
              <div class="provider-title-info">
                <h3 class="provider-name">{{ provider.providerName }}</h3>
                <ElTag type="info" size="small" class="model-count">
                  {{ provider.models?.length || 0 }} 个模型
                </ElTag>
              </div>
            </div>
            <div class="provider-actions">
              <ElButton
                :icon="Edit"
                circle
                size="small"
                @click="handleEditProvider(provider)"
              />
              <ElButton
                :icon="Delete"
                circle
                size="small"
                type="danger"
                @click="handleDeleteProvider(provider)"
              />
            </div>
          </div>

          <!-- 卡片内容 -->
          <div class="provider-content">
            <div class="provider-info-item">
              <span class="info-label">
                <ElIcon><Link /></ElIcon>
                API 地址
              </span>
              <ElText class="info-value" truncated>
                {{ provider.baseUrl }}
              </ElText>
            </div>
            <div class="provider-info-item">
              <span class="info-label">
                <ElIcon><Key /></ElIcon>
                API Key
              </span>
              <ElText class="info-value api-key">
                {{ provider.maskedApiKey }}
              </ElText>
            </div>
            <div class="provider-info-item">
              <span class="info-label">
                <ElIcon><Clock /></ElIcon>
                创建时间
              </span>
              <span class="info-value">{{ provider.createdTime }}</span>
            </div>
          </div>

          <ElDivider />

          <!-- 模型列表 -->
          <div class="models-section">
            <div class="models-header">
              <span class="section-title">
                <ElIcon><Grid /></ElIcon>
                支持的模型
              </span>
              <ElButton
                :icon="Plus"
                size="small"
                type="primary"
                text
                @click="handleAddModel(provider)"
              >
                添加模型
              </ElButton>
            </div>

            <div v-if="provider.models?.length" class="models-grid">
              <div
                v-for="model in provider.models"
                :key="model.id"
                class="model-chip"
                @click="handleEditModel(provider, model)"
              >
                <span class="model-name">{{ model.modelName }}</span>
                <ElIcon
                  class="model-remove"
                  @click.stop="handleDeleteModel(model)"
                >
                  <Close />
                </ElIcon>
              </div>
            </div>
            <ElEmpty
              v-else
              description="暂无模型"
              :image-size="60"
              class="empty-models"
            />
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <ElEmpty
        v-if="!loading && !providers.length"
        description="暂无厂商数据，点击上方按钮新增"
        :image-size="200"
        class="empty-state"
      />
    </div>

    <!-- 厂商对话框 -->
    <ElDialog
      v-model="providerDialogVisible"
      :title="providerDialogTitle"
      width="600px"
      :close-on-click-modal="false"
      class="custom-dialog"
    >
      <ElForm
        ref="providerFormRef"
        :model="providerForm"
        :rules="providerRules"
        label-width="100px"
      >
        <ElFormItem label="厂商名称" prop="providerName">
          <ElInput
            v-model="providerForm.providerName"
            placeholder="如：OpenAI、Anthropic"
            clearable
          >
            <template #prefix>
              <ElIcon><OfficeBuilding /></ElIcon>
            </template>
          </ElInput>
        </ElFormItem>

        <ElFormItem label="API 地址" prop="baseUrl">
          <ElInput
            v-model="providerForm.baseUrl"
            placeholder="如：https://api.openai.com/v1"
            clearable
          >
            <template #prefix>
              <ElIcon><Link /></ElIcon>
            </template>
          </ElInput>
        </ElFormItem>

        <ElFormItem v-if="!providerForm.id" label="API Key" prop="apiKey">
          <ElInput
            v-model="providerForm.apiKey"
            type="textarea"
            :rows="3"
            placeholder="请输入 API 密钥"
            show-password
          />
        </ElFormItem>

        <ElFormItem v-else label="更新 API Key">
          <ElInput
            v-model="providerForm.apiKey"
            type="textarea"
            :rows="3"
            placeholder="留空则保持原密钥不变"
            show-password
          />
          <template #extra>
            <ElText type="info" size="small">
              当前: {{ providerForm.maskedApiKey || '***' }}（留空不修改）
            </ElText>
          </template>
        </ElFormItem>
      </ElForm>

      <template #footer>
        <ElButton @click="providerDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleProviderSubmit"> 确定 </ElButton>
      </template>
    </ElDialog>

    <!-- 模型对话框 -->
    <ElDialog
      v-model="modelDialogVisible"
      :title="modelDialogTitle"
      width="500px"
      :close-on-click-modal="false"
      class="custom-dialog"
    >
      <ElForm
        ref="modelFormRef"
        :model="modelForm"
        :rules="modelRules"
        label-width="100px"
      >
        <ElFormItem label="模型名称" prop="modelName">
          <ElInput
            v-model="modelForm.modelName"
            placeholder="如：gpt-4、claude-3"
            clearable
          >
            <template #prefix>
              <ElIcon><Grid /></ElIcon>
            </template>
          </ElInput>
        </ElFormItem>
        <ElFormItem label="JSON Schema">
          <ElSwitch
            v-model="modelForm.supportsJsonSchema"
            inline-prompt
            active-text="支持"
            inactive-text="不支持"
          />
          <template #extra>
            <ElText type="info" size="small">
              关闭后，将以传统字符串格式返回响应。
            </ElText>
          </template>
        </ElFormItem>
      </ElForm>

      <template #footer>
        <ElButton @click="modelDialogVisible = false">取消</ElButton>
        <ElButton type="primary" @click="handleModelSubmit">确定</ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup>
import {
  createModel,
  createProvider,
  deleteModel,
  deleteProvider,
  listProviders,
  updateModel,
  updateProvider,
} from '@/api/aiProvider';
import {
  Clock,
  Close,
  Delete,
  Edit,
  Grid,
  Key,
  Link,
  OfficeBuilding,
  Plus,
} from '@element-plus/icons-vue';

// 数据
const loading = ref(false);
const providers = ref([]);

// 统计数据
const totalModels = computed(() => {
  return providers.value.reduce((sum, p) => sum + (p.models?.length || 0), 0);
});

// 厂商对话框
const providerDialogVisible = ref(false);
const providerDialogTitle = ref('新增厂商');
const providerFormRef = ref(null);
const providerForm = reactive({
  id: null,
  providerName: '',
  baseUrl: '',
  apiKey: '',
  maskedApiKey: '',
});

const providerRules = {
  providerName: [
    { required: true, message: '请输入厂商名称', trigger: 'blur' },
  ],
  baseUrl: [{ required: true, message: '请输入 API 地址', trigger: 'blur' }],
  apiKey: [
    {
      validator: (rule, value, callback) => {
        if (!providerForm.id && !value) {
          callback(new Error('请输入 API Key'));
        } else {
          callback();
        }
      },
      trigger: 'blur',
    },
  ],
};

// 模型对话框
const modelDialogVisible = ref(false);
const modelDialogTitle = ref('新增模型');
const modelFormRef = ref(null);
const modelForm = reactive({
  id: null,
  providerId: null,
  modelName: '',
  supportsJsonSchema: true,
});

const modelRules = {
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
};

const currentProvider = ref(null);

// 加载数据
const loadProviders = async () => {
  loading.value = true;
  try {
    const data = await listProviders();
    providers.value = data || [];
  } catch (error) {
    console.error('加载失败:', error);
    ElMessage.error(error.message || '加载失败');
  } finally {
    loading.value = false;
  }
};

// 厂商操作
const handleAddProvider = () => {
  providerDialogTitle.value = '新增厂商';
  Object.assign(providerForm, {
    id: null,
    providerName: '',
    baseUrl: '',
    apiKey: '',
    maskedApiKey: '',
  });
  providerDialogVisible.value = true;
};

const handleEditProvider = (row) => {
  providerDialogTitle.value = '编辑厂商';
  Object.assign(providerForm, {
    id: row.id,
    providerName: row.providerName,
    baseUrl: row.baseUrl,
    apiKey: '',
    maskedApiKey: row.maskedApiKey,
  });
  providerDialogVisible.value = true;
};

const handleProviderSubmit = async () => {
  if (!providerFormRef.value) return;

  await providerFormRef.value.validate(async (valid) => {
    if (!valid) return;

    try {
      const data = {
        providerName: providerForm.providerName,
        baseUrl: providerForm.baseUrl,
      };

      if (providerForm.apiKey) {
        data.apiKey = providerForm.apiKey;
      }

      if (providerForm.id) {
        data.id = providerForm.id;
        await updateProvider(providerForm.id, data);
      } else {
        await createProvider(data);
      }

      ElMessage.success('操作成功');
      providerDialogVisible.value = false;
      await loadProviders();
    } catch (error) {
      console.error('提交失败:', error);
      ElMessage.error(error.message || '操作失败');
    }
  });
};

const handleDeleteProvider = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定删除厂商 "${row.providerName}"? 这将同时删除所有关联模型。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    );

    await deleteProvider(row.id);
    ElMessage.success('删除成功');
    await loadProviders();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
      ElMessage.error(error.message || '删除失败');
    }
  }
};

// 模型操作
const handleAddModel = (provider) => {
  currentProvider.value = provider;
  modelDialogTitle.value = `为 ${provider.providerName} 添加模型`;
  Object.assign(modelForm, {
    id: null,
    providerId: provider.id,
    modelName: '',
    supportsJsonSchema: true,
  });
  modelDialogVisible.value = true;
};

const handleEditModel = (provider, model) => {
  currentProvider.value = provider;
  modelDialogTitle.value = '编辑模型';
  Object.assign(modelForm, {
    id: model.id,
    providerId: provider.id,
    modelName: model.modelName,
    supportsJsonSchema: model.supportsJsonSchema ?? true,
  });
  modelDialogVisible.value = true;
};

const handleModelSubmit = async () => {
  if (!modelFormRef.value) return;

  await modelFormRef.value.validate(async (valid) => {
    if (!valid) return;

    try {
      const data = { ...modelForm };

      if (data.id) {
        await updateModel(data.id, data);
      } else {
        await createModel(data);
      }

      ElMessage.success('操作成功');
      modelDialogVisible.value = false;
      await loadProviders();
    } catch (error) {
      console.error('提交失败:', error);
      ElMessage.error(error.message || '操作失败');
    }
  });
};

const handleDeleteModel = async (model) => {
  try {
    await ElMessageBox.confirm(
      `确定删除模型 "${model.modelName}"?`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    );

    await deleteModel(model.id);
    ElMessage.success('删除成功');
    await loadProviders();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
      ElMessage.error(error.message || '删除失败');
    }
  }
};

// 初始化
onMounted(() => {
  loadProviders();
});
</script>

<style scoped lang="scss">
.ai-provider-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7eb 100%);
  padding: 96px 0 40px;

  @media (max-width: 768px) {
    padding: 84px 0 32px;
  }
}

// 统计卡片
.stats-section {
  padding: 0 32px;
  margin-bottom: 32px;
}

.stats-container {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
}

.stat-card {
  background: white;
  border-radius: 20px;
  padding: 28px 32px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, #409eff, #67c23a);
    transform: scaleX(0);
    transform-origin: left;
    transition: transform 0.3s ease;
  }

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);

    &::before {
      transform: scaleX(1);
    }

    .stat-icon {
      transform: scale(1.1);
    }
  }
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  transition: transform 0.3s ease;

  &.provider-icon {
    background: linear-gradient(135deg, #409eff, #5cacee);
    color: white;
  }

  &.model-icon {
    background: linear-gradient(135deg, #67c23a, #85ce61);
    color: white;
  }

  &.key-icon {
    background: linear-gradient(135deg, #e6a23c, #f6bb76);
    color: white;
  }
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: #303133;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 0.9375rem;
  color: #909399;
  font-weight: 500;
}

// 操作栏
.toolbar-section {
  padding: 0 32px;
  margin-bottom: 24px;
}

.toolbar-container {
  max-width: 1400px;
  margin: 0 auto;
}

.add-btn {
  height: 48px;
  padding: 0 32px;
  font-size: 1rem;
  font-weight: 600;
}

// 厂商列表
.providers-section {
  padding: 0 32px;
}

.providers-container {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(550px, 1fr));
  gap: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.provider-card {
  background: white;
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  border: 2px solid transparent;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
    border-color: #409eff;
  }
}

.provider-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.provider-title-wrapper {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.provider-logo {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: linear-gradient(135deg, #409eff, #5cacee);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 28px;
}

.provider-title-info {
  flex: 1;
}

.provider-name {
  font-size: 1.375rem;
  font-weight: 700;
  color: #303133;
  margin: 0 0 8px 0;
}

.model-count {
  font-weight: 500;
}

.provider-actions {
  display: flex;
  gap: 8px;
}

.provider-content {
  margin-bottom: 24px;
}

.provider-info-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;

  &:last-child {
    border-bottom: none;
  }
}

.info-label {
  width: 120px;
  font-size: 0.875rem;
  color: #606266;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;

  .el-icon {
    color: #409eff;
  }
}

.info-value {
  flex: 1;
  font-size: 0.875rem;
  color: #303133;

  &.api-key {
    font-family: 'Courier New', monospace;
    color: #67c23a;
    font-weight: 600;
  }
}

.models-section {
  margin-top: 24px;
}

.models-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: #606266;
  display: flex;
  align-items: center;
  gap: 8px;

  .el-icon {
    color: #409eff;
  }
}

.models-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.model-chip {
  background: linear-gradient(135deg, #f0f9ff, #e0f2fe);
  border: 2px solid #409eff;
  border-radius: 20px;
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: linear-gradient(135deg, #e0f2fe, #bae6fd);
    transform: translateY(-2px);
  }
}

.model-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: #409eff;
}

.model-remove {
  color: #f56c6c;
  font-size: 16px;
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.2);
  }
}

.empty-models {
  padding: 20px 0;
}

.empty-state {
  max-width: 1400px;
  margin: 0 auto;
  padding: 60px 0;
}

// 对话框样式
.custom-dialog {
  :deep(.el-dialog__header) {
    background: linear-gradient(135deg, #409eff, #5cacee);
    padding: 20px 24px;
    margin: 0;

    .el-dialog__title {
      color: white;
      font-weight: 600;
      font-size: 1.125rem;
    }

    .el-dialog__headerbtn {
      top: 20px;
      right: 20px;

      .el-dialog__close {
        color: white;
        font-size: 20px;
      }
    }
  }

  :deep(.el-dialog__body) {
    padding: 32px 24px;
  }

  :deep(.el-dialog__footer) {
    padding: 16px 24px;
    border-top: 1px solid #f0f0f0;
  }
}
</style>
