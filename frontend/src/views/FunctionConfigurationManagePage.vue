<template>
  <div class="function-config-page">
    <AppHeader />

    <div class="stats-section">
      <div class="stats-container">
        <div class="stat-card">
          <div class="stat-icon total-icon">
            <ElIcon><Grid /></ElIcon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalFunctions }}</div>
            <div class="stat-label">功能数量</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon model-icon">
            <ElIcon><Cpu /></ElIcon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ modelConfigured }}</div>
            <div class="stat-label">模型已配置</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon prompt-icon">
            <ElIcon><Document /></ElIcon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ promptConfigured }}</div>
            <div class="stat-label">提示词已配置</div>
          </div>
        </div>
      </div>
    </div>

    <div class="toolbar-section">
      <div class="toolbar-container">
        <ElButton
          type="primary"
          :icon="RefreshRight"
          size="large"
          class="toolbar-btn"
          @click="loadData"
        >
          刷新数据
        </ElButton>
      </div>
    </div>

    <div v-loading="loading" class="config-section">
      <ElTable :data="configurations" class="config-table" border>
        <ElTableColumn label="功能" min-width="220">
          <template #default="{ row }">
            <div class="function-title">{{ row.functionDescription }}</div>
            <div class="function-code">{{ row.functionCode }}</div>
          </template>
        </ElTableColumn>
        <ElTableColumn label="模型" min-width="220">
          <template #default="{ row }">
            <div v-if="row.modelMapping" class="config-block">
              <div class="config-title">
                {{ row.modelMapping.model?.modelName || '-' }}
              </div>
              <div class="config-sub">
                {{ row.modelMapping.model?.provider?.providerName || '-' }}
              </div>
            </div>
            <ElTag v-else type="info" size="small">未配置</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="提示词" min-width="220">
          <template #default="{ row }">
            <div v-if="row.promptMapping" class="config-block">
              <div class="config-title">
                {{ row.promptMapping.promptTemplate?.name || '-' }}
              </div>
              <div class="config-sub">{{ row.promptMapping.promptCode }}</div>
            </div>
            <ElTag v-else type="info" size="small">未配置</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="260" align="center">
          <template #default="{ row }">
            <ElSpace>
              <ElButton
                type="primary"
                size="small"
                :icon="Edit"
                @click="openEditDialog(row)"
              >
                配置
              </ElButton>
              <ElButton
                v-if="row.modelMapping"
                type="danger"
                link
                size="small"
                @click="handleClearModelMapping(row)"
              >
                清除模型
              </ElButton>
              <ElButton
                v-if="row.promptMapping"
                type="danger"
                link
                size="small"
                @click="handleClearPromptMapping(row)"
              >
                清除提示词
              </ElButton>
            </ElSpace>
          </template>
        </ElTableColumn>
      </ElTable>
    </div>

    <ElEmpty
      v-if="!loading && !configurations.length"
      description="暂无功能数据"
      :image-size="200"
      class="empty-state"
    />

    <ElDialog
      v-model="dialogVisible"
      width="600px"
      :title="editingItem?.functionDescription"
      :close-on-click-modal="false"
      class="config-dialog"
    >
      <ElForm label-width="110px">
        <ElFormItem label="功能代码">
          <ElInput :value="editingItem?.functionCode" disabled />
        </ElFormItem>

        <ElFormItem label="选择模型">
          <ElSelect
            v-model="form.modelId"
            placeholder="请选择模型"
            filterable
            clearable
          >
            <ElOptionGroup
              v-for="provider in providers"
              :key="provider.id"
              :label="provider.providerName"
            >
              <ElOption
                v-for="model in provider.models"
                :key="model.id"
                :label="`${model.modelName} (${provider.providerName})`"
                :value="model.id"
              />
            </ElOptionGroup>
          </ElSelect>
        </ElFormItem>

        <ElFormItem label="提示词模板">
          <ElSelect
            v-model="form.promptCode"
            placeholder="请选择提示词"
            filterable
            clearable
          >
            <ElOption
              v-for="template in promptTemplates"
              :key="template.code"
              :label="`${template.name} (${template.code})`"
              :value="template.code"
            />
          </ElSelect>
        </ElFormItem>
      </ElForm>

      <template #footer>
        <ElSpace>
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton type="primary" :loading="saving" @click="handleSubmit">
            保存
          </ElButton>
        </ElSpace>
      </template>
    </ElDialog>
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue';
import {
  deleteFunctionModelMapping,
  deleteFunctionPromptMapping,
  listFunctionConfigurations,
  setFunctionModelMapping,
  setFunctionPromptMapping,
} from '@/api/functionConfiguration';
import {
  Cpu,
  Document,
  Edit,
  Grid,
  RefreshRight,
} from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

const loading = ref(false);
const saving = ref(false);
const configurations = ref([]);
const providers = ref([]);
const promptTemplates = ref([]);

const dialogVisible = ref(false);
const editingItem = ref(null);
const form = reactive({
  modelId: null,
  promptCode: '',
});

const totalFunctions = computed(() => configurations.value.length);
const modelConfigured = computed(
  () => configurations.value.filter((item) => item.modelMapping).length,
);
const promptConfigured = computed(
  () => configurations.value.filter((item) => item.promptMapping).length,
);

const loadData = async () => {
  loading.value = true;
  try {
    const response = await listFunctionConfigurations();
    configurations.value = response?.functions || [];
    providers.value = response?.providers || [];
    promptTemplates.value = response?.promptTemplates || [];
  } catch (error) {
    console.error('加载功能配置失败', error);
    ElMessage.error(error.message || '加载功能配置失败');
  } finally {
    loading.value = false;
  }
};

const openEditDialog = (item) => {
  editingItem.value = item;
  form.modelId = item.modelMapping?.modelId ?? null;
  form.promptCode = item.promptMapping?.promptCode ?? '';
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  if (!editingItem.value) {
    return;
  }

  const tasks = [];
  const { functionCode } = editingItem.value;

  const currentModelId = editingItem.value.modelMapping?.modelId ?? null;
  if (form.modelId !== currentModelId) {
    if (form.modelId) {
      tasks.push(
        setFunctionModelMapping({
          functionType: functionCode,
          modelId: form.modelId,
        }),
      );
    } else if (editingItem.value.modelMapping?.id) {
      tasks.push(deleteFunctionModelMapping(editingItem.value.modelMapping.id));
    }
  }

  const currentPromptCode = editingItem.value.promptMapping?.promptCode ?? '';
  const promptChanged = form.promptCode !== currentPromptCode;

  if (promptChanged) {
    if (form.promptCode) {
      tasks.push(
        setFunctionPromptMapping({
          functionCode,
          promptCode: form.promptCode,
        }),
      );
    } else if (editingItem.value.promptMapping) {
      tasks.push(deleteFunctionPromptMapping(functionCode, currentPromptCode));
    }
  }

  if (!tasks.length) {
    ElMessage.info('未检测到需要保存的改动');
    return;
  }

  saving.value = true;
  try {
    await Promise.all(tasks);
    ElMessage.success('配置已更新');
    dialogVisible.value = false;
    await loadData();
  } catch (error) {
    console.error('保存配置失败', error);
    ElMessage.error(error.message || '保存配置失败');
  } finally {
    saving.value = false;
  }
};

const handleClearModelMapping = async (item) => {
  if (!item.modelMapping?.id) {
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确定清除【${item.functionDescription}】的模型配置？`,
      '确认清除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    );

    await deleteFunctionModelMapping(item.modelMapping.id);
    ElMessage.success('模型配置已清除');
    await loadData();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清除模型配置失败', error);
      ElMessage.error(error.message || '清除模型配置失败');
    }
  }
};

const handleClearPromptMapping = async (item) => {
  if (!item.promptMapping?.promptCode) {
    return;
  }
  try {
    await ElMessageBox.confirm(
      `确定清除【${item.functionDescription}】的提示词配置？`,
      '确认清除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    );

    await deleteFunctionPromptMapping(
      item.functionCode,
      item.promptMapping.promptCode,
    );
    ElMessage.success('提示词配置已清除');
    await loadData();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清除提示词配置失败', error);
      ElMessage.error(error.message || '清除提示词配置失败');
    }
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.function-config-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7eb 100%);
  padding: 120px 0 60px;
}

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
  color: white;
}

.total-icon {
  background: linear-gradient(135deg, #409eff, #5cacee);
}

.model-icon {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.prompt-icon {
  background: linear-gradient(135deg, #e6a23c, #f6bb76);
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

.toolbar-section {
  padding: 0 32px;
  margin-bottom: 24px;
}

.toolbar-container {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-btn {
  height: 48px;
  padding: 0 32px;
  font-size: 1rem;
  font-weight: 600;
}

.config-section {
  padding: 0 32px;
}

.config-table {
  max-width: 1400px;
  margin: 0 auto;
  background: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 6px 18px rgba(31, 41, 51, 0.08);

  :deep(.cell) {
    padding: 16px;
  }
}

.function-title {
  font-weight: 600;
  color: #1f2933;
}

.function-code {
  font-size: 0.75rem;
  color: #8a97a6;
  margin-top: 4px;
}

.config-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.config-title {
  font-weight: 600;
  color: #1f2933;
}

.config-sub {
  font-size: 0.85rem;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.empty-state {
  max-width: 1400px;
  margin: 60px auto;
  padding: 0 32px;
}

.config-dialog {
  :deep(.el-dialog__header) {
    background: linear-gradient(135deg, #67c23a, #85ce61);
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
      }
    }
  }
}

@media (max-width: 768px) {
  .page-header {
    padding: 32px 20px;
  }

  .stats-section,
  .toolbar-section,
  .config-section {
    padding: 0 20px;
  }

  .stats-container {
    grid-template-columns: 1fr;
  }

  .page-title {
    font-size: 2rem;
  }
}
</style>
