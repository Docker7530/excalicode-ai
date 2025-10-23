<template>
  <div class="function-prompt-mapping-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-background"></div>
      <div class="header-content">
        <div class="header-nav">
          <ElButton
            :icon="ArrowLeft"
            circle
            size="large"
            class="back-btn"
            @click="handleBack"
          />
          <ElBreadcrumb separator="/" class="breadcrumb">
            <ElBreadcrumbItem :to="{ path: '/backend-manage' }">
              后台管理
            </ElBreadcrumbItem>
            <ElBreadcrumbItem>功能-提示词映射管理</ElBreadcrumbItem>
          </ElBreadcrumb>
        </div>
        <div class="header-info">
          <h1 class="page-title">
            <ElIcon class="title-icon"><Link /></ElIcon>
            功能-提示词映射管理
          </h1>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-section">
      <div class="stats-container">
        <div class="stat-card">
          <div class="stat-icon provider-icon">
            <ElIcon><Document /></ElIcon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ functionTypes.length }}</div>
            <div class="stat-label">功能类型</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon model-icon">
            <ElIcon><Check /></ElIcon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ configuredCount }}</div>
            <div class="stat-label">已配置</div>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon key-icon">
            <ElIcon><Warning /></ElIcon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              {{ functionTypes.length - configuredCount }}
            </div>
            <div class="stat-label">未配置</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="toolbar-section">
      <div class="toolbar-container">
        <ElButton type="primary" :icon="Refresh" size="large" @click="loadData">
          刷新数据
        </ElButton>
        <ElButton
          type="warning"
          :icon="Delete"
          size="large"
          @click="handleClearCache"
        >
          清除缓存
        </ElButton>
      </div>
    </div>

    <!-- 功能卡片列表 -->
    <div class="functions-section">
      <div class="functions-container">
        <div
          v-for="functionType in functionTypes"
          :key="functionType.code"
          class="function-card"
        >
          <div class="function-header">
            <h3>{{ functionType.description }}</h3>
            <ElTag
              :type="
                isFunctionConfigured(functionType.code) ? 'success' : 'info'
              "
              size="small"
            >
              {{
                isFunctionConfigured(functionType.code) ? '已配置' : '未配置'
              }}
            </ElTag>
          </div>

          <div class="function-code">
            <ElText type="info" size="small">{{ functionType.code }}</ElText>
          </div>

          <div class="function-content">
            <div
              v-if="getMappingByFunctionCode(functionType.code)"
              class="mapping-info"
            >
              <ElDescriptions :column="1" size="small" border>
                <ElDescriptionsItem label="提示词代码">
                  {{ getMappingByFunctionCode(functionType.code).promptCode }}
                </ElDescriptionsItem>
                <ElDescriptionsItem label="提示词名称">
                  {{
                    getMappingByFunctionCode(functionType.code).promptTemplate
                      ?.name || '-'
                  }}
                </ElDescriptionsItem>
                <ElDescriptionsItem label="优先级">
                  {{ getMappingByFunctionCode(functionType.code).priority }}
                </ElDescriptionsItem>
              </ElDescriptions>
            </div>
            <ElEmpty v-else description="未配置提示词" :image-size="80" />
          </div>

          <div class="function-actions">
            <ElButton
              type="primary"
              size="small"
              :icon="Setting"
              @click="openConfigDialog(functionType)"
            >
              {{
                isFunctionConfigured(functionType.code)
                  ? '修改配置'
                  : '立即配置'
              }}
            </ElButton>
            <ElButton
              v-if="isFunctionConfigured(functionType.code)"
              type="danger"
              size="small"
              :icon="Delete"
              plain
              @click="handleDeleteMapping(functionType.code)"
            >
              删除
            </ElButton>
          </div>
        </div>
      </div>
    </div>

    <!-- 配置对话框 -->
    <ElDialog
      v-model="dialogVisible"
      :title="`配置功能: ${currentFunction?.description || ''}`"
      width="600px"
    >
      <ElForm :model="configForm" label-width="100px">
        <ElFormItem label="功能代码">
          <ElInput v-model="currentFunction.code" disabled />
        </ElFormItem>

        <ElFormItem label="功能名称">
          <ElInput v-model="currentFunction.description" disabled />
        </ElFormItem>

        <ElFormItem label="提示词" required>
          <ElSelect
            v-model="configForm.promptCode"
            placeholder="请选择提示词模板"
            filterable
            style="width: 100%"
          >
            <ElOption
              v-for="template in promptTemplates"
              :key="template.code"
              :label="`${template.name} (${template.code})`"
              :value="template.code"
            >
              <div class="prompt-option">
                <span class="prompt-option-name">{{ template.name }}</span>
                <ElTag size="small" type="info">{{ template.code }}</ElTag>
              </div>
              <div class="prompt-option-desc">{{ template.description }}</div>
            </ElOption>
          </ElSelect>
        </ElFormItem>

        <ElFormItem label="优先级">
          <ElInputNumber
            v-model="configForm.priority"
            :min="0"
            :max="100"
            placeholder="数字越大优先级越高"
            style="width: 100%"
          />
        </ElFormItem>
      </ElForm>

      <template #footer>
        <ElButton @click="dialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="saving" @click="handleSetMapping"
          >确定</ElButton
        >
      </template>
    </ElDialog>
  </div>
</template>

<script setup>
import {
  ArrowLeft,
  Check,
  Delete,
  Document,
  Link,
  Refresh,
  Setting,
  Warning,
} from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import {
  clearCache,
  deleteMapping,
  getFunctionTypes,
  listMappings,
  setMapping,
} from '../api/functionPromptMapping.js';
import { listPromptTemplates } from '../api/promptTemplate.js';

const router = useRouter();

// 数据
const functionTypes = ref([]);
const mappings = ref([]);
const promptTemplates = ref([]);
const dialogVisible = ref(false);
const saving = ref(false);
const currentFunction = ref({});
const configForm = ref({
  promptCode: '',
  priority: 0,
});

// 计算属性
const configuredCount = computed(() => {
  return new Set(mappings.value.map((m) => m.functionCode)).size;
});

// 返回
const handleBack = () => {
  router.push('/backend-manage');
};

// 方法
const loadData = async () => {
  try {
    const [functionTypesRes, mappingsRes, templatesRes] = await Promise.all([
      getFunctionTypes(),
      listMappings(),
      listPromptTemplates(),
    ]);

    functionTypes.value = functionTypesRes || [];
    mappings.value = mappingsRes || [];
    promptTemplates.value = templatesRes || [];
  } catch (error) {
    ElMessage.error('加载数据失败: ' + error.message);
  }
};

const isFunctionConfigured = (functionCode) => {
  return mappings.value.some((m) => m.functionCode === functionCode);
};

const getMappingByFunctionCode = (functionCode) => {
  return mappings.value.find((m) => m.functionCode === functionCode);
};

const openConfigDialog = (functionType) => {
  currentFunction.value = functionType;
  const existingMapping = getMappingByFunctionCode(functionType.code);

  if (existingMapping) {
    configForm.value = {
      promptCode: existingMapping.promptCode,
      priority: existingMapping.priority || 0,
    };
  } else {
    configForm.value = {
      promptCode: '',
      priority: 0,
    };
  }

  dialogVisible.value = true;
};

const handleSetMapping = async () => {
  if (!configForm.value.promptCode) {
    ElMessage.warning('请选择提示词模板');
    return;
  }

  saving.value = true;
  try {
    await setMapping({
      functionCode: currentFunction.value.code,
      promptCode: configForm.value.promptCode,
      priority: configForm.value.priority,
    });

    ElMessage.success('配置成功');
    dialogVisible.value = false;
    await loadData();
  } catch (error) {
    ElMessage.error('配置失败: ' + error.message);
  } finally {
    saving.value = false;
  }
};

const handleDeleteMapping = async (functionCode) => {
  try {
    const mapping = getMappingByFunctionCode(functionCode);
    if (!mapping) return;

    await ElMessageBox.confirm('确定删除该映射吗？', '确认删除', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    });

    await deleteMapping(functionCode, mapping.promptCode);
    ElMessage.success('删除成功');
    await loadData();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message);
    }
  }
};

const handleClearCache = async () => {
  try {
    await clearCache();
    ElMessage.success('缓存已清除');
  } catch (error) {
    ElMessage.error('清除缓存失败: ' + error.message);
  }
};

// 生命周期
onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.function-prompt-mapping-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7eb 100%);
  padding-bottom: 40px;
}

// 页面头部
.page-header {
  position: relative;
  background: linear-gradient(135deg, #909399 0%, #a6a9ad 100%);
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

.header-nav {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.back-btn {
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
  display: flex;
  gap: 12px;

  .el-button {
    height: 48px;
    padding: 0 32px;
    font-size: 1rem;
    font-weight: 600;
  }
}

// 功能列表
.functions-section {
  padding: 0 32px;
}

.functions-container {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 24px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.function-card {
  background: white;
  border-radius: 24px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  border: 2px solid transparent;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
    border-color: #909399;
  }

  .function-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h3 {
      font-size: 1.375rem;
      font-weight: 700;
      color: #303133;
      margin: 0;
    }
  }

  .function-code {
    margin-bottom: 20px;
    padding: 8px 12px;
    background: #f5f7fa;
    border-radius: 8px;
  }

  .function-content {
    min-height: 150px;
    margin-bottom: 20px;

    .mapping-info {
      :deep(.el-descriptions__label) {
        width: 100px;
      }
    }
  }

  .function-actions {
    display: flex;
    gap: 12px;
    justify-content: flex-end;
  }
}

// 对话框中的提示词选项样式
.prompt-option {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .prompt-option-name {
    font-weight: 500;
  }
}

.prompt-option-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
