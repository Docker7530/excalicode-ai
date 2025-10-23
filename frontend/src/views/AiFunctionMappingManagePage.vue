<template>
  <div class="ai-function-mapping-page">
    <!-- 渐变头部区域 -->
    <div class="page-header">
      <div class="header-background"></div>
      <div class="header-content">
        <div class="header-top">
          <ElButton
            :icon="ArrowLeft"
            circle
            size="large"
            class="back-button"
            @click="handleBack"
          />
          <ElBreadcrumb separator="/" class="breadcrumb">
            <ElBreadcrumbItem :to="{ path: '/backend-manage' }">
              后台管理
            </ElBreadcrumbItem>
            <ElBreadcrumbItem>功能-模型映射管理</ElBreadcrumbItem>
          </ElBreadcrumb>
        </div>
        <div class="header-info">
          <h1 class="page-title">
            <ElIcon class="title-icon"><Connection /></ElIcon>
            功能-模型映射管理
          </h1>
        </div>
      </div>
    </div>

    <!-- 统计卡片区域 -->
    <div class="stats-section">
      <div class="stat-card total-card">
        <div class="stat-icon-wrapper">
          <ElIcon><Grid /></ElIcon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ functionTypes.length }}</div>
          <div class="stat-label">功能类型</div>
        </div>
      </div>

      <div class="stat-card configured-card">
        <div class="stat-icon-wrapper">
          <ElIcon><CircleCheck /></ElIcon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ mappings.length }}</div>
          <div class="stat-label">已配置</div>
        </div>
      </div>

      <div class="stat-card unconfigured-card">
        <div class="stat-icon-wrapper">
          <ElIcon><WarningFilled /></ElIcon>
        </div>
        <div class="stat-content">
          <div class="stat-value">{{ unmappedCount }}</div>
          <div class="stat-label">未配置</div>
        </div>
      </div>
    </div>

    <!-- 操作工具栏 -->
    <div class="toolbar-section">
      <div class="toolbar-container">
        <ElButton
          type="success"
          :icon="Setting"
          size="large"
          @click="handleAddMapping"
        >
          快速配置
        </ElButton>
        <ElButton :icon="RefreshRight" size="large" @click="handleClearCache">
          清除缓存
        </ElButton>
      </div>
    </div>

    <!-- 功能映射卡片列表 -->
    <div v-loading="loading" class="mapping-grid">
      <div
        v-for="functionType in functionTypes"
        :key="functionType.code"
        class="function-card"
        :class="{ 'is-configured': getMappingByCode(functionType.code) }"
        @click="handleEditMapping(functionType)"
      >
        <div class="card-glow"></div>
        <div class="card-header">
          <div class="function-icon">
            <ElIcon
              :size="32"
              :color="getMappingStatus(functionType.code).color"
            >
              <component :is="getMappingStatus(functionType.code).icon" />
            </ElIcon>
          </div>
          <div
            class="status-badge"
            :class="getMappingStatus(functionType.code).badgeClass"
          >
            {{ getMappingStatus(functionType.code).text }}
          </div>
        </div>

        <div class="card-body">
          <h3 class="function-name">{{ functionType.description }}</h3>
          <div class="function-code">{{ functionType.code }}</div>

          <div v-if="getMappingByCode(functionType.code)" class="mapping-info">
            <div class="info-row">
              <ElIcon class="info-icon"><OfficeBuilding /></ElIcon>
              <span class="info-label">厂商</span>
              <span class="info-value">
                {{
                  getMappingByCode(functionType.code).model?.provider
                    ?.providerName || '-'
                }}
              </span>
            </div>
            <div class="info-row">
              <ElIcon class="info-icon"><Cpu /></ElIcon>
              <span class="info-label">模型</span>
              <span class="info-value model-name">
                {{
                  getMappingByCode(functionType.code).model?.modelName || '-'
                }}
              </span>
            </div>
            <div class="info-row">
              <ElIcon class="info-icon"><Link /></ElIcon>
              <span class="info-label">API</span>
              <ElText class="info-value api-url" truncated>
                {{
                  getMappingByCode(functionType.code).model?.provider
                    ?.baseUrl || '-'
                }}
              </ElText>
            </div>
          </div>

          <div v-else class="mapping-empty">
            <ElIcon :size="40" color="#e5e7eb"><Warning /></ElIcon>
            <p class="empty-text">未配置映射</p>
            <p class="empty-hint">将使用默认模型</p>
          </div>
        </div>

        <div class="card-footer">
          <ElButton type="success" link :icon="Edit" class="edit-btn">
            {{ getMappingByCode(functionType.code) ? '编辑配置' : '立即配置' }}
          </ElButton>
          <ElButton
            v-if="getMappingByCode(functionType.code)"
            type="danger"
            link
            :icon="Delete"
            class="delete-btn"
            @click.stop="handleDeleteMapping(functionType)"
          >
            删除
          </ElButton>
        </div>

        <div class="card-corner-decoration"></div>
      </div>
    </div>

    <!-- 空状态 -->
    <ElEmpty
      v-if="!loading && !functionTypes.length"
      description="暂无功能类型数据"
      :image-size="200"
      class="empty-state"
    />

    <!-- 配置映射对话框 -->
    <ElDialog
      v-model="mappingDialogVisible"
      :title="mappingDialogTitle"
      width="600px"
      :close-on-click-modal="false"
      class="mapping-dialog"
    >
      <template #header>
        <div class="dialog-header">
          <div class="dialog-header-background"></div>
          <div class="dialog-header-content">
            <ElIcon :size="24" class="dialog-icon"><Connection /></ElIcon>
            <span class="dialog-title">{{ mappingDialogTitle }}</span>
          </div>
        </div>
      </template>

      <ElForm
        ref="mappingFormRef"
        :model="mappingForm"
        :rules="mappingRules"
        label-width="120px"
        class="mapping-form"
      >
        <ElFormItem label="功能类型">
          <ElInput
            :value="mappingForm.functionTypeDescription"
            disabled
            placeholder="功能类型"
            class="function-type-input"
          >
            <template #prefix>
              <ElIcon><Grid /></ElIcon>
            </template>
          </ElInput>
        </ElFormItem>

        <ElFormItem label="选择模型" prop="modelId">
          <ElSelect
            v-model="mappingForm.modelId"
            placeholder="请选择要使用的 AI 模型"
            filterable
            style="width: 100%"
            size="large"
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
              >
                <div class="model-option">
                  <div class="model-option-left">
                    <ElIcon><Cpu /></ElIcon>
                    <span class="model-option-name">{{ model.modelName }}</span>
                  </div>
                  <ElTag size="small" type="success" effect="light">
                    {{ provider.providerName }}
                  </ElTag>
                </div>
              </ElOption>
            </ElOptionGroup>
          </ElSelect>
        </ElFormItem>

        <ElAlert
          title="配置说明"
          type="info"
          :closable="false"
          show-icon
          class="config-alert"
        >
          <template #default>
            <p class="alert-text">
              配置后，该功能将使用指定的 AI 模型进行处理。
            </p>
            <p class="alert-text">
              如果未配置或删除配置，将自动使用
              <strong>application.properties</strong> 中配置的默认模型。
            </p>
          </template>
        </ElAlert>
      </ElForm>

      <template #footer>
        <div class="dialog-footer">
          <ElButton size="large" @click="mappingDialogVisible = false">
            取消
          </ElButton>
          <ElButton type="success" size="large" @click="handleMappingSubmit">
            <ElIcon class="btn-icon"><Check /></ElIcon>
            确定配置
          </ElButton>
        </div>
      </template>
    </ElDialog>
  </div>
</template>

<script setup>
import {
  clearCache,
  deleteMapping,
  getFunctionTypes,
  listMappings,
  setMapping,
} from '@/api/aiFunctionMapping';
import { listProviders } from '@/api/aiProvider';
import {
  ArrowLeft,
  Check,
  CircleCheck,
  Connection,
  Cpu,
  Delete,
  Edit,
  Grid,
  Link,
  OfficeBuilding,
  RefreshRight,
  Setting,
  Warning,
  WarningFilled,
} from '@element-plus/icons-vue';

const router = useRouter();

// 数据
const loading = ref(false);
const functionTypes = ref([]);
const mappings = ref([]);
const providers = ref([]);

// 统计数据
const unmappedCount = computed(() => {
  return functionTypes.value.length - mappings.value.length;
});

// 映射对话框
const mappingDialogVisible = ref(false);
const mappingDialogTitle = ref('配置映射');
const mappingFormRef = ref(null);
const mappingForm = reactive({
  functionType: '',
  functionTypeDescription: '',
  modelId: null,
});

const mappingRules = {
  modelId: [{ required: true, message: '请选择模型', trigger: 'change' }],
};

// 返回上一页
const handleBack = () => {
  router.push('/backend-manage');
};

// 加载数据
const loadData = async () => {
  loading.value = true;
  try {
    const [typesData, mappingsData, providersData] = await Promise.all([
      getFunctionTypes(),
      listMappings(),
      listProviders(),
    ]);
    functionTypes.value = typesData || [];
    mappings.value = mappingsData || [];
    providers.value = providersData || [];
  } catch (error) {
    console.error('加载数据失败:', error);
    ElMessage.error(error.message || '加载数据失败');
  } finally {
    loading.value = false;
  }
};

// 根据功能类型代码获取映射
const getMappingByCode = (code) => {
  return mappings.value.find((m) => m.functionType === code);
};

// 获取映射状态
const getMappingStatus = (code) => {
  const mapping = getMappingByCode(code);
  if (mapping) {
    return {
      text: '已配置',
      badgeClass: 'badge-configured',
      color: '#67C23A',
      icon: CircleCheck,
    };
  } else {
    return {
      text: '使用默认',
      badgeClass: 'badge-default',
      color: '#909399',
      icon: WarningFilled,
    };
  }
};

// 配置映射
const handleAddMapping = () => {
  if (!functionTypes.value.length) {
    ElMessage.warning('暂无功能类型数据');
    return;
  }

  // 选择第一个未配置的功能类型
  const unmapped = functionTypes.value.find((ft) => !getMappingByCode(ft.code));
  if (unmapped) {
    handleEditMapping(unmapped);
  } else {
    ElMessage.info('所有功能类型均已配置');
  }
};

// 编辑映射
const handleEditMapping = (functionType) => {
  const mapping = getMappingByCode(functionType.code);
  mappingDialogTitle.value = mapping ? '编辑映射配置' : '新建映射配置';

  Object.assign(mappingForm, {
    functionType: functionType.code,
    functionTypeDescription: functionType.description,
    modelId: mapping?.modelId || null,
  });

  mappingDialogVisible.value = true;
};

// 提交映射
const handleMappingSubmit = async () => {
  if (!mappingFormRef.value) return;

  await mappingFormRef.value.validate(async (valid) => {
    if (!valid) return;

    try {
      await setMapping({
        functionType: mappingForm.functionType,
        modelId: mappingForm.modelId,
      });

      ElMessage.success('配置成功');
      mappingDialogVisible.value = false;
      await loadData();
    } catch (error) {
      console.error('配置失败:', error);
      ElMessage.error(error.message || '配置失败');
    }
  });
};

// 删除映射
const handleDeleteMapping = async (functionType) => {
  const mapping = getMappingByCode(functionType.code);
  if (!mapping) return;

  try {
    await ElMessageBox.confirm(
      `确定要删除 "${functionType.description}" 的模型映射吗？删除后将使用默认模型。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    );

    await deleteMapping(mapping.id);
    ElMessage.success('删除成功');
    await loadData();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error);
      ElMessage.error(error.message || '删除失败');
    }
  }
};

// 清除缓存
const handleClearCache = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清除 ChatModel 缓存吗？清除后所有模型实例将重新加载。',
      '清除缓存确认',
      {
        confirmButtonText: '确定清除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    );

    await clearCache();
    ElMessage.success('缓存已清除');
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清除缓存失败:', error);
      ElMessage.error(error.message || '清除缓存失败');
    }
  }
};

// 初始化
onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.ai-function-mapping-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding-bottom: 60px;
}

// ========== 渐变头部区域 ==========
.page-header {
  position: relative;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  padding: 40px 32px;
  margin-bottom: 32px;
  overflow: hidden;
}

.header-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  opacity: 0.1;
  background:
    radial-gradient(circle at 20% 50%, white 0%, transparent 50%),
    radial-gradient(circle at 80% 50%, white 0%, transparent 50%);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  position: relative;
  z-index: 2;
}

.header-top {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.back-button {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;

  &:hover {
    background: rgba(255, 255, 255, 0.3);
  }
}

.breadcrumb {
  :deep(.el-breadcrumb__item) {
    .el-breadcrumb__inner {
      color: rgba(255, 255, 255, 0.9);
      font-weight: 500;

      &:hover {
        color: white;
      }
    }

    &:last-child .el-breadcrumb__inner {
      color: white;
    }
  }

  :deep(.el-breadcrumb__separator) {
    color: rgba(255, 255, 255, 0.6);
  }
}

.header-info {
  text-align: center;
}

.page-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: white;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;

  .title-icon {
    font-size: 2.5rem;
  }
}

// ========== 统计卡片区域 ==========
.stats-section {
  max-width: 1400px;
  margin: 0 auto 32px;
  padding: 0 32px;
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

    .stat-icon-wrapper {
      transform: scale(1.1);
    }
  }
}

.stat-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
  transition: transform 0.3s ease;
}

.total-card .stat-icon-wrapper {
  background: linear-gradient(135deg, #409eff, #5cacee);
}

.configured-card .stat-icon-wrapper {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.unconfigured-card .stat-icon-wrapper {
  background: linear-gradient(135deg, #e6a23c, #f0c78a);
}

.stat-content {
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

// ========== 工具栏区域 ==========
.toolbar-section {
  max-width: 1400px;
  margin: 0 auto 24px;
  padding: 0 32px;
}

.toolbar-container {
  display: flex;
  gap: 12px;

  .el-button {
    height: 48px;
    padding: 0 32px;
    font-size: 1rem;
    font-weight: 600;
  }
}

// ========== 功能映射卡片列表 ==========
.mapping-grid {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 40px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 24px;

  @media (max-width: 768px) {
    padding: 0 24px;
    grid-template-columns: 1fr;
  }
}

.function-card {
  background: white;
  border-radius: 20px;
  padding: 32px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, #909399, #c0c4cc);
    transform: scaleX(0);
    transform-origin: left;
    transition: transform 0.3s ease;
  }

  &.is-configured::before {
    background: linear-gradient(90deg, #67c23a, #85ce61);
  }

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
    border-color: rgba(103, 194, 58, 0.2);

    &::before {
      transform: scaleX(1);
    }

    .card-glow {
      opacity: 1;
    }

    .function-icon {
      transform: scale(1.05);
    }
  }
}

.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(
    circle,
    rgba(103, 194, 58, 0.08) 0%,
    transparent 70%
  );
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.function-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #f8fafc, #e2e8f0);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s ease;
}

.status-badge {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;

  &.badge-configured {
    background: linear-gradient(135deg, #67c23a, #85ce61);
    color: white;
    box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3);
  }

  &.badge-default {
    background: #f5f7fa;
    color: #909399;
    border: 1px solid #e4e7ed;
  }
}

.card-body {
  margin-bottom: 24px;
}

.function-name {
  font-size: 1.375rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.function-code {
  font-size: 0.75rem;
  color: #94a3b8;
  font-family: 'Courier New', monospace;
  background: #f8fafc;
  padding: 4px 12px;
  border-radius: 6px;
  display: inline-block;
  margin-bottom: 20px;
}

.mapping-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: #f8fafc;
  border-radius: 10px;
  transition: background 0.2s ease;

  &:hover {
    background: #f1f5f9;
  }

  .info-icon {
    color: #67c23a;
    font-size: 16px;
    flex-shrink: 0;
  }

  .info-label {
    font-size: 0.875rem;
    color: #64748b;
    font-weight: 500;
    min-width: 40px;
  }

  .info-value {
    flex: 1;
    font-size: 0.875rem;
    color: #1e293b;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    &.model-name {
      font-weight: 600;
      color: #67c23a;
    }

    &.api-url {
      font-family: 'Courier New', monospace;
      font-size: 0.75rem;
      color: #3b82f6;
    }
  }
}

.mapping-empty {
  text-align: center;
  padding: 32px 0;

  .empty-text {
    font-size: 1rem;
    font-weight: 600;
    color: #94a3b8;
    margin: 12px 0 4px 0;
  }

  .empty-hint {
    font-size: 0.875rem;
    color: #cbd5e1;
    margin: 0;
  }
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;

  .edit-btn,
  .delete-btn {
    font-weight: 600;
    font-size: 0.875rem;
  }
}

.card-corner-decoration {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 80px;
  height: 80px;
  background: linear-gradient(
    135deg,
    transparent 50%,
    rgba(103, 194, 58, 0.05) 50%
  );
  pointer-events: none;
}

// ========== 空状态 ==========
.empty-state {
  max-width: 1400px;
  margin: 60px auto;
  padding: 0 40px;
}

// ========== 配置映射对话框 ==========
.mapping-dialog {
  :deep(.el-dialog) {
    border-radius: 20px;
    overflow: hidden;
  }

  :deep(.el-dialog__header) {
    padding: 0;
    margin: 0;
  }

  :deep(.el-dialog__body) {
    padding: 32px;
  }

  :deep(.el-dialog__footer) {
    padding: 20px 32px 32px;
  }
}

.dialog-header {
  position: relative;
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
  padding: 32px;
  overflow: hidden;

  .dialog-header-background {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: radial-gradient(
      circle at 80% 50%,
      rgba(255, 255, 255, 0.2) 0%,
      transparent 70%
    );
    opacity: 0.5;
  }

  .dialog-header-content {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    gap: 12px;
    color: white;

    .dialog-icon {
      font-size: 24px;
    }

    .dialog-title {
      font-size: 1.5rem;
      font-weight: 700;
    }
  }
}

.mapping-form {
  .function-type-input {
    :deep(.el-input__wrapper) {
      background: #f8fafc;
    }
  }

  .el-select {
    :deep(.el-input__wrapper) {
      min-height: 48px;
    }
  }
}

.model-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .model-option-left {
    display: flex;
    align-items: center;
    gap: 10px;
    flex: 1;

    .el-icon {
      color: #67c23a;
    }

    .model-option-name {
      font-weight: 500;
    }
  }
}

.config-alert {
  margin-top: 24px;
  border-radius: 12px;

  :deep(.el-alert__content) {
    .alert-text {
      margin: 4px 0;
      line-height: 1.6;

      strong {
        color: #409eff;
        font-weight: 600;
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .el-button {
    min-width: 100px;

    .btn-icon {
      margin-right: 6px;
    }
  }
}
</style>
