<!--
  COSMIC 功能过程分组编辑器
  以分组方式展示功能过程，支持触发事件/功能过程为组，子过程为组内条目
  提供优雅的交互体验和完整的编辑功能
-->
<template>
  <div class="process-editor">
    <input
      ref="importInputRef"
      class="file-input"
      type="file"
      accept=".xlsx,.xls"
      @change="handleImportFile"
    />
    <!-- 头部操作区 -->
    <div class="editor-header">
      <div class="header-main">
        <h2>功能过程表格</h2>
        <div class="header-stats">
          <span class="stat-item">
            <ElIcon><List /></ElIcon>
            共 {{ processGroups.length }} 个功能过程
          </span>
          <span class="stat-item">
            <ElIcon><DocumentCopy /></ElIcon>
            共 {{ totalSubProcesses }} 个子过程
          </span>
        </div>
      </div>
      <div class="header-actions">
        <ElButton
          type="primary"
          plain
          :loading="importing"
          :disabled="importing"
          :icon="Upload"
          @click="handleImportClick"
        >
          导入表格
        </ElButton>
        <ElButton type="info" plain :icon="List" @click="openTaskDrawer">
          我的任务
        </ElButton>
        <ElButton
          type="primary"
          :loading="exportLoading"
          :disabled="exportLoading || !processGroups.length"
          :icon="Download"
          @click="emitExport"
        >
          导出表格
        </ElButton>
        <ElButton type="primary" :icon="Plus" @click="addNewProcessGroup">
          新增功能过程
        </ElButton>
      </div>
    </div>

    <!-- 主编辑区域 -->
    <div class="editor-body">
      <div v-if="processGroups.length" class="process-groups">
        <div
          v-for="(group, groupIndex) in processGroups"
          :key="groupIndex"
          class="process-group"
        >
          <ElCard shadow="hover" class="group-card">
            <!-- 分组头部 -->
            <template #header>
              <div class="group-header">
                <div class="group-header-top">
                  <div class="group-title">
                    <ElIcon class="group-icon">
                      <Folder />
                    </ElIcon>
                    <span class="group-label"
                      >功能过程 {{ groupIndex + 1 }}</span
                    >
                    <ElTag size="small" type="primary" effect="plain">
                      {{ group.subProcesses.length }} 个子过程
                    </ElTag>
                  </div>
                  <div class="group-header-actions">
                    <ElButton
                      type="danger"
                      size="small"
                      :icon="Delete"
                      @click="removeProcessGroup(groupIndex)"
                    >
                      删除分组
                    </ElButton>
                  </div>
                </div>

                <div class="group-meta">
                  <div class="meta-field">
                    <label class="field-label">
                      <ElIcon><Lightning /></ElIcon>
                      事件：
                    </label>
                    <ElInput
                      :key="`trigger-${groupIndex}`"
                      v-model="group.triggerEvent"
                      placeholder="请输入事件"
                      class="field-input"
                      clearable
                    />
                  </div>
                  <div class="meta-field">
                    <label class="field-label">
                      <ElIcon><Operation /></ElIcon>
                      过程：
                    </label>
                    <ElInput
                      :key="`func-${groupIndex}`"
                      v-model="group.functionalProcess"
                      placeholder="请输入过程"
                      class="field-input"
                      clearable
                    />
                  </div>
                </div>
              </div>
            </template>

            <!-- 子过程表格 -->
            <div class="sub-processes">
              <ElTable
                :data="group.subProcesses"
                class="sub-process-table"
                show-header
                size="small"
                border
                :row-key="(row) => row.id"
              >
                <ElTableColumn label="拖拽" width="60" align="center">
                  <template #default="{ $index }">
                    <div
                      class="drag-handle"
                      :draggable="true"
                      @dragstart="
                        handleSubProcessDragStart(groupIndex, $index, $event)
                      "
                      @dragover="handleSubProcessDragOver($event)"
                      @drop="handleSubProcessDrop(groupIndex, $index, $event)"
                      @dragend="handleSubProcessDragEnd"
                    >
                      <ElIcon><Rank /></ElIcon>
                    </div>
                  </template>
                </ElTableColumn>

                <ElTableColumn label="子过程描述" min-width="200">
                  <template #default="{ row, $index }">
                    <ElInput
                      :key="`desc-${$index}`"
                      v-model="row.subProcessDesc"
                      placeholder="请输入子过程描述"
                      size="small"
                    />
                  </template>
                </ElTableColumn>

                <ElTableColumn label="数据移动" width="100">
                  <template #default="{ row, $index }">
                    <ElSelect
                      :key="`type-${$index}`"
                      v-model="row.dataMovementType"
                      placeholder="选择"
                      size="small"
                    >
                      <ElOption label="E" value="E" />
                      <ElOption label="R" value="R" />
                      <ElOption label="W" value="W" />
                      <ElOption label="X" value="X" />
                    </ElSelect>
                  </template>
                </ElTableColumn>

                <ElTableColumn label="数据组" min-width="140">
                  <template #default="{ row, $index }">
                    <ElInput
                      :key="`group-${$index}`"
                      v-model="row.dataGroup"
                      placeholder="数据组"
                      size="small"
                    />
                  </template>
                </ElTableColumn>

                <ElTableColumn label="数据属性" min-width="140">
                  <template #default="{ row, $index }">
                    <ElInput
                      :key="`attr-${$index}`"
                      v-model="row.dataAttributes"
                      placeholder="数据属性"
                      size="small"
                    />
                  </template>
                </ElTableColumn>

                <ElTableColumn
                  label="操作"
                  width="120"
                  align="center"
                  fixed="right"
                >
                  <template #default="{ $index }">
                    <div class="action-buttons">
                      <ElTooltip content="在此后插入" placement="top">
                        <ElButton
                          type="primary"
                          size="small"
                          text
                          :icon="Bottom"
                          @click="insertSubProcessAfter(groupIndex, $index)"
                        />
                      </ElTooltip>
                      <ElTooltip content="删除" placement="top">
                        <ElButton
                          type="danger"
                          size="small"
                          text
                          :icon="Close"
                          @click="removeSubProcess(groupIndex, $index)"
                        />
                      </ElTooltip>
                    </div>
                  </template>
                </ElTableColumn>
              </ElTable>

              <!-- 空状态 -->
              <div
                v-if="!group.subProcesses.length"
                class="empty-sub-processes"
              >
                <ElEmpty
                  :image-size="60"
                  description="暂无子过程，点击右上角'删除分组'可删除此功能过程"
                />
              </div>
            </div>
          </ElCard>
        </div>
      </div>

      <div v-else class="empty-state">
        <ElEmpty :image-size="120" description="暂无功能过程数据">
          <ElButton
            type="primary"
            :icon="Plus"
            size="large"
            @click="addNewProcessGroup"
          >
            创建第一个功能过程
          </ElButton>
        </ElEmpty>
      </div>
    </div>

    <!-- 底部操作区 -->
    <div v-if="processGroups.length > 0" class="editor-footer">
      <div class="footer-note">
        <ElIcon><InfoFilled /></ElIcon>
        <span>完善所有功能过程信息后可生成需求文档</span>
      </div>
      <div class="footer-actions">
        <ElButton
          type="warning"
          size="large"
          :loading="sequenceLoading"
          :disabled="sequenceLoading || !processGroups.length"
          :icon="Connection"
          @click="emitGenerateSequence"
        >
          生成时序图
        </ElButton>
        <ElButton
          type="success"
          size="large"
          :loading="documentLoading"
          :disabled="documentLoading || !processGroups.length"
          :icon="DocumentChecked"
          @click="emitGenerate"
        >
          生成需求文档
        </ElButton>
      </div>
    </div>

    <ElDrawer
      v-model="taskDrawerVisible"
      title="我的子过程生成任务"
      size="55%"
      direction="btt"
      destroy-on-close
      :close-on-click-modal="false"
      class="task-drawer"
    >
      <div class="task-toolbar">
        <div class="task-toolbar-left">
          <ElButton
            type="primary"
            plain
            :icon="RefreshRight"
            :loading="taskLoading"
            @click="loadTaskList"
          >
            刷新
          </ElButton>
        </div>
        <div v-if="taskError" class="task-toolbar-right">
          <ElTag type="danger" effect="dark">{{ taskError }}</ElTag>
        </div>
      </div>

      <ElEmpty
        v-if="!taskList.length && !taskLoading"
        description="暂无生成任务，提交后在此查看进度"
      />
      <ElTable
        v-else
        :data="taskList"
        size="small"
        border
        :row-class-name="taskRowClassName"
        :loading="taskLoading"
      >
        <ElTableColumn prop="taskId" label="任务ID" width="120" />
        <ElTableColumn label="状态" width="140">
          <template #default="{ row }">
            <ElTag :type="resolveStatusTagType(row.status)" effect="plain">
              {{ row.statusLabel || row.status }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="processCount" label="子过程数" width="100">
          <template #default="{ row }">
            {{ row.processCount ?? '--' }}
          </template>
        </ElTableColumn>
        <ElTableColumn prop="createdTime" label="创建时间" min-width="160" />
        <ElTableColumn prop="finishedTime" label="完成时间" min-width="160">
          <template #default="{ row }">
            {{ row.finishedTime || '进行中' }}
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <div class="task-actions">
              <ElButton
                type="success"
                size="small"
                plain
                :disabled="row.status !== 'SUCCEEDED'"
                :loading="applyingTaskId === row.taskId"
                @click="handleApplyTask(row)"
              >
                应用
              </ElButton>
            </div>
            <div
              v-if="row.status === 'FAILED' && row.errorMessage"
              class="task-error-text"
            >
              {{ row.errorMessage }}
            </div>
          </template>
        </ElTableColumn>
      </ElTable>
    </ElDrawer>
  </div>
</template>

<script setup>
/**
 * 功能过程分组编辑器
 * 将触发事件、功能过程与子过程编辑流程整合在一体
 */

import { cosmicService } from '@/api';
import {
  Bottom,
  Close,
  Delete,
  Connection,
  DocumentChecked,
  DocumentCopy,
  Download,
  Folder,
  InfoFilled,
  Lightning,
  List,
  Operation,
  Plus,
  RefreshRight,
  Rank,
  Upload,
} from '@element-plus/icons-vue';

const props = defineProps({
  initialProcesses: {
    type: Array,
    default: () => [],
  },
  exportLoading: {
    type: Boolean,
    default: false,
  },
  documentLoading: {
    type: Boolean,
    default: false,
  },
  sequenceLoading: {
    type: Boolean,
    default: false,
  },
  latestTaskId: {
    type: [Number, String],
    default: null,
  },
});

const emit = defineEmits([
  'process-change',
  'export-table',
  'generate-doc',
  'generate-sequence',
]);

// 工具函数：生成唯一ID
const generateId = () =>
  `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

// 工具函数：深度克隆
const cloneData = (source = []) => {
  try {
    return structuredClone(source);
  } catch {
    return JSON.parse(JSON.stringify(source));
  }
};

const EXCEL_FILE_PATTERN = /\.(xlsx|xls)$/i;
const ALLOWED_MOVEMENT_TYPES = new Set(['E', 'R', 'W', 'X']);
const STATUS_TAG_TYPE = {
  PENDING: 'info',
  RUNNING: 'warning',
  SUCCEEDED: 'success',
  FAILED: 'danger',
};

const normalizeMovementType = (value) => {
  const normalized = (value ?? '').toString().trim().toUpperCase();
  return ALLOWED_MOVEMENT_TYPES.has(normalized) ? normalized : 'E';
};

const resolveStatusTagType = (status) => STATUS_TAG_TYPE[status] || 'info';

// 将嵌套过程数据转换为分组数据
const convertToGrouped = (processes = []) => {
  if (!Array.isArray(processes) || !processes.length) return [];

  return processes.map((process) => {
    const triggerEvent = (process?.triggerEvent || '').trim();
    const functionalProcess = (process?.functionalProcess || '').trim();
    const steps = Array.isArray(process?.processSteps)
      ? process.processSteps.map((step) => ({
          id: generateId(),
          subProcessDesc: (step?.subProcessDesc || '').trim(),
          dataMovementType: normalizeMovementType(step?.dataMovementType),
          dataGroup: (step?.dataGroup || '').trim(),
          dataAttributes: (step?.dataAttributes || '').trim(),
        }))
      : [];

    if (!steps.length) {
      steps.push({
        id: generateId(),
        subProcessDesc: '',
        dataMovementType: 'E',
        dataGroup: '',
        dataAttributes: '',
      });
    }

    return {
      id: generateId(),
      triggerEvent,
      functionalProcess,
      subProcesses: steps,
    };
  });
};

// 将分组数据转换回嵌套过程数据
const convertToFlat = (groups = []) => {
  if (!groups.length) return [];

  return groups.map((group) => ({
    triggerEvent: (group?.triggerEvent || '').trim(),
    functionalProcess: (group?.functionalProcess || '').trim(),
    processSteps: Array.isArray(group?.subProcesses)
      ? group.subProcesses.map((subProcess) => ({
          subProcessDesc: (subProcess?.subProcessDesc || '').trim(),
          dataMovementType: normalizeMovementType(subProcess?.dataMovementType),
          dataGroup: (subProcess?.dataGroup || '').trim(),
          dataAttributes: (subProcess?.dataAttributes || '').trim(),
        }))
      : [],
  }));
};

// 使用 reactive 但避免深度监听在输入时的干扰
const state = reactive({
  processGroups: convertToGrouped(props.initialProcesses),
});

const importInputRef = ref(null);
const importing = ref(false);
const taskDrawerVisible = ref(false);
const taskLoading = ref(false);
const taskList = ref([]);
const taskError = ref('');
const applyingTaskId = ref(null);

const resetImportInput = (inputEl) => {
  const element = inputEl ?? importInputRef.value;
  if (element) {
    element.value = '';
  }
};

const handleImportClick = () => {
  if (importing.value) {
    return;
  }
  importInputRef.value?.click();
};

const handleImportFile = async (event) => {
  const inputEl = event?.target ?? importInputRef.value;
  const [file] = event?.target?.files ?? [];
  if (!file) {
    resetImportInput(inputEl);
    return;
  }

  if (!EXCEL_FILE_PATTERN.test(file.name)) {
    ElMessage.error('请上传 Excel 文件（.xlsx/.xls）');
    resetImportInput(inputEl);
    return;
  }

  importing.value = true;
  try {
    const result = await cosmicService.importCosmicProcesses(file);
    const imported = Array.isArray(result?.processes) ? result.processes : [];
    const sanitized = imported
      .map((process) => {
        const triggerEvent = (process?.triggerEvent || '').trim();
        const functionalProcess = (process?.functionalProcess || '').trim();
        const steps = Array.isArray(process?.processSteps)
          ? process.processSteps
              .map((step) => ({
                subProcessDesc: (step?.subProcessDesc || '').trim(),
                dataMovementType: normalizeMovementType(step?.dataMovementType),
                dataGroup: (step?.dataGroup || '').trim(),
                dataAttributes: (step?.dataAttributes || '').trim(),
              }))
              .filter((step) => step.subProcessDesc && step.dataGroup)
          : [];

        return {
          triggerEvent,
          functionalProcess,
          processSteps: steps,
        };
      })
      .filter(
        (item) =>
          item.triggerEvent &&
          item.functionalProcess &&
          item.processSteps.length,
      );

    if (!sanitized.length) {
      ElMessage.warning('未解析到有效的子过程，请检查 Excel 内容');
      return;
    }

    state.processGroups = convertToGrouped(sanitized);
    const flatProcesses = convertToFlat(state.processGroups);
    lastFlatData = JSON.stringify(flatProcesses);
    emit('process-change', cloneData(flatProcesses));
    ElMessage.success('子过程已导入，可继续校验与编辑');
  } catch (error) {
    ElMessage.error(error.message || '子过程导入失败，请稍后重试');
  } finally {
    importing.value = false;
    resetImportInput(inputEl);
  }
};

// 缓存上一次的数据，用于减少不必要的更新
let lastFlatData = JSON.stringify(convertToFlat(state.processGroups));

// 计算属性
const processGroups = computed(() => state.processGroups);
const totalSubProcesses = computed(() => {
  return state.processGroups.reduce(
    (total, group) => total + group.subProcesses.length,
    0,
  );
});
const highlightedTaskId = computed(() => {
  const id = props.latestTaskId;
  return id === null || id === undefined ? '' : String(id);
});

// 监听外部数据变化（优化版）
watch(
  () => props.initialProcesses,
  (newValue, oldValue) => {
    const newData = JSON.stringify(newValue);
    if (newData !== JSON.stringify(oldValue) && newData !== lastFlatData) {
      isInternalUpdate = true;
      nextTick(() => {
        state.processGroups = convertToGrouped(newValue);
        lastFlatData = newData;
      });
    }
  },
  { deep: false, flush: 'pre' },
);

// 监听内部数据变化，向外发送（优化防抖处理）
let emitTimer = null;
let isInternalUpdate = false;

watch(
  () => state.processGroups,
  (value) => {
    if (isInternalUpdate) {
      isInternalUpdate = false;
      return;
    }

    if (emitTimer) {
      clearTimeout(emitTimer);
    }

    emitTimer = setTimeout(() => {
      const flatProcesses = convertToFlat(value);
      const currentData = JSON.stringify(flatProcesses);

      // 只有在数据真正变化时才发送
      if (currentData !== lastFlatData) {
        lastFlatData = currentData;
        emit('process-change', cloneData(flatProcesses));
      }
    }, 300);
  },
  { deep: true, flush: 'post' },
);

// 打开任务抽屉时自动刷新任务列表
watch(
  () => taskDrawerVisible.value,
  (visible) => {
    if (visible) {
      loadTaskList();
    }
  },
);

watch(
  () => props.latestTaskId,
  (taskId) => {
    if (taskId && taskDrawerVisible.value) {
      loadTaskList();
    }
  },
);

const taskRowClassName = ({ row }) => {
  if (!row?.taskId || !highlightedTaskId.value) {
    return '';
  }
  return String(row.taskId) === highlightedTaskId.value
    ? 'task-row-highlight'
    : '';
};

const openTaskDrawer = async () => {
  taskDrawerVisible.value = true;
  await loadTaskList();
};

const loadTaskList = async () => {
  if (taskLoading.value) {
    return;
  }
  taskLoading.value = true;
  taskError.value = '';
  try {
    const tasks = await cosmicService.listAnalysisTasks();
    taskList.value = Array.isArray(tasks) ? tasks : [];
  } catch (error) {
    taskError.value = error.message || '任务列表获取失败';
    ElMessage.error(taskError.value);
  } finally {
    taskLoading.value = false;
  }
};

const handleApplyTask = async (task) => {
  if (!task?.taskId) {
    ElMessage.warning('任务信息缺失，无法应用');
    return;
  }
  if (task.status !== 'SUCCEEDED') {
    ElMessage.warning('仅支持应用已完成的任务');
    return;
  }
  applyingTaskId.value = task.taskId;
  try {
    const detail = await cosmicService.getAnalysisTaskDetail(task.taskId);
    const processes = Array.isArray(detail?.processes) ? detail.processes : [];
    if (!processes.length) {
      throw new Error('任务未返回可用的子过程结果');
    }
    state.processGroups = convertToGrouped(processes);
    const flatProcesses = convertToFlat(state.processGroups);
    lastFlatData = JSON.stringify(flatProcesses);
    emit('process-change', cloneData(flatProcesses));
    ElMessage.success('任务结果已应用到表格，可继续导出或生成文档');
    taskDrawerVisible.value = false;
  } catch (error) {
    ElMessage.error(error.message || '应用任务结果失败，请稍后重试');
  } finally {
    applyingTaskId.value = null;
  }
};

// 添加新的功能过程分组
const addNewProcessGroup = () => {
  const newGroup = {
    id: generateId(),
    triggerEvent: '',
    functionalProcess: '',
    subProcesses: [
      {
        id: generateId(),
        subProcessDesc: '',
        dataMovementType: 'E',
        dataGroup: '',
        dataAttributes: '',
      },
    ],
  };

  state.processGroups.push(newGroup);
  ElMessage.success('已添加新的功能过程分组');
};

// 删除功能过程分组
const removeProcessGroup = (groupIndex) => {
  if (state.processGroups.length === 1) {
    ElMessage.warning('至少需要保留一个功能过程分组');
    return;
  }

  state.processGroups.splice(groupIndex, 1);
  ElMessage.success('已删除功能过程分组');
};

// 删除子过程
const removeSubProcess = (groupIndex, subIndex) => {
  const group = state.processGroups[groupIndex];
  if (group.subProcesses.length === 1) {
    ElMessage.warning('每个分组至少需要保留一个子过程');
    return;
  }

  group.subProcesses.splice(subIndex, 1);
  ElMessage.success('已删除子过程');
};

// 在指定位置后插入子过程
const insertSubProcessAfter = (groupIndex, subIndex) => {
  const newSubProcess = {
    id: generateId(),
    subProcessDesc: '',
    dataMovementType: 'E',
    dataGroup: '',
    dataAttributes: '',
  };

  state.processGroups[groupIndex].subProcesses.splice(
    subIndex + 1,
    0,
    newSubProcess,
  );
  ElMessage.success('已在此后插入新的子过程');
};

// 子过程拖拽状态
const subProcessDragState = reactive({
  groupIndex: null,
  startIndex: null,
  draggedData: null,
});

// 子过程拖拽开始
const handleSubProcessDragStart = (groupIndex, index, event) => {
  subProcessDragState.groupIndex = groupIndex;
  subProcessDragState.startIndex = index;
  subProcessDragState.draggedData = {
    ...state.processGroups[groupIndex].subProcesses[index],
  };

  event.dataTransfer.effectAllowed = 'move';
  event.dataTransfer.setData('text/plain', index.toString());
};

// 子过程拖拽悬停
const handleSubProcessDragOver = (event) => {
  if (subProcessDragState.startIndex === null) return;
  event.preventDefault();
  event.dataTransfer.dropEffect = 'move';
};

// 子过程拖拽放下
const handleSubProcessDrop = (groupIndex, targetIndex, event) => {
  if (
    subProcessDragState.startIndex === null ||
    subProcessDragState.groupIndex !== groupIndex ||
    subProcessDragState.startIndex === targetIndex
  ) {
    return;
  }

  event.preventDefault();

  const startIndex = subProcessDragState.startIndex;
  const subProcesses = state.processGroups[groupIndex].subProcesses;
  const [movedItem] = subProcesses.splice(startIndex, 1);
  subProcesses.splice(targetIndex, 0, movedItem);

  ElMessage.success('子过程顺序已调整');
};

// 子过程拖拽结束
const handleSubProcessDragEnd = () => {
  subProcessDragState.groupIndex = null;
  subProcessDragState.startIndex = null;
  subProcessDragState.draggedData = null;
};

// 导出和生成文档
const emitExport = () => {
  emit('export-table');
};

const emitGenerate = () => {
  emit('generate-doc');
};

const emitGenerateSequence = () => {
  emit('generate-sequence');
};
</script>

<style lang="scss" scoped>
@use 'sass:color';

.file-input {
  display: none;
}

.process-editor {
  background: $background-primary;
  border-radius: $border-radius-2xl;
  box-shadow: $shadow-lg;
  border: 1px solid $border-light;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 600px;
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
      background: linear-gradient(135deg, #4a90e2 0%, #50c878 100%);
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
  overflow: hidden;
}

// 过程分组容器
.process-groups {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: $spacing-2xl $spacing-3xl;
  overflow: auto;
}

.editor-footer {
  padding: $spacing-xl $spacing-3xl;
  border-top: 1px solid $border-light;
  background: rgba($background-secondary, 0.6);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: $spacing-lg;
  flex-wrap: wrap;

  .footer-note {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    color: $secondary-color;
    font-size: $font-size-sm;
  }

  .footer-actions {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
  }

  @media (max-width: $breakpoint-md) {
    flex-direction: column;
    align-items: stretch;

    .footer-actions {
      justify-content: center;
    }
  }
}

// 单个分组卡片
.process-group {
  .group-card {
    border-radius: 1rem;
    border: 1px solid rgba(74, 144, 226, 0.1);
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      transform: translateY(-2px);
      box-shadow:
        0 12px 40px rgba(74, 144, 226, 0.12),
        0 4px 12px rgba(0, 0, 0, 0.08);
      border-color: rgba(74, 144, 226, 0.2);
    }

    :deep(.el-card__header) {
      padding: 1.5rem;
      border-bottom: 1px solid rgba(74, 144, 226, 0.08);
      background: linear-gradient(
        135deg,
        rgba(74, 144, 226, 0.03) 0%,
        rgba(80, 200, 120, 0.02) 100%
      );
    }

    :deep(.el-card__body) {
      padding: 1.5rem;
    }
  }
}

// 分组头部
.group-header {
  display: flex;
  flex-direction: column;
  gap: 1rem;

  .group-header-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;

    .group-title {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex: 1;

      .group-icon {
        color: #4a90e2;
        font-size: 1.25rem;
      }

      .group-label {
        font-size: 1.125rem;
        font-weight: 600;
        color: #1f2937;
      }
    }

    .group-header-actions {
      display: flex;
      gap: 0.5rem;
      flex-shrink: 0;
    }
  }

  .group-meta {
    display: flex;
    flex-direction: row;
    gap: 1rem;

    .meta-field {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex: 1;

      .field-label {
        display: flex;
        align-items: center;
        gap: 0.375rem;
        min-width: 70px;
        font-weight: 600;
        font-size: 0.875rem;
        color: #374151;
        white-space: nowrap;

        .el-icon {
          color: #4a90e2;
        }
      }

      .field-input {
        flex: 1;

        :deep(.el-input__wrapper) {
          border-radius: 0.5rem;
          border: 1px solid rgba(74, 144, 226, 0.2);
          background: rgba(255, 255, 255, 0.9);
          transition: all 0.2s ease;

          &:hover {
            border-color: rgba(74, 144, 226, 0.4);
          }

          &.is-focus {
            border-color: #4a90e2;
            box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.1);
          }
        }
      }
    }
  }
}

// 子过程区域
.sub-processes {
  .drag-handle {
    width: 32px;
    height: 32px;
    border-radius: $border-radius-md;
    border: 1px solid $border-medium;
    background: $background-secondary;
    display: flex;
    align-items: center;
    justify-content: center;
    color: $secondary-color;
    cursor: grab;
    transition: all $transition-fast;
    margin: 0 auto;

    &:hover {
      border-color: $accent-color;
      color: $accent-color;
      background: rgba($accent-color, 0.08);
    }

    &:active {
      cursor: grabbing;
    }
  }

  .action-buttons {
    display: flex;
    gap: $spacing-xs;
    justify-content: center;
  }

  .sub-process-table {
    border-radius: 0.5rem;
    overflow: hidden;

    :deep(.el-table) {
      --el-table-border-color: rgba(74, 144, 226, 0.1);
      --el-table-header-bg-color: rgba(74, 144, 226, 0.05);
      --el-table-row-hover-bg-color: rgba(74, 144, 226, 0.03);
      font-size: 0.875rem;
    }

    :deep(.el-table th) {
      background: rgba(74, 144, 226, 0.05);
      color: #374151;
      font-weight: 600;
      font-size: 0.875rem;
      padding: 8px 12px;
    }

    :deep(.el-table td) {
      padding: 6px 12px;
      background: rgba(255, 255, 255, 0.8);
    }

    :deep(.el-input--small .el-input__wrapper) {
      border-radius: 0.375rem;
      border: 1px solid rgba(156, 163, 175, 0.2);
      background: rgba(255, 255, 255, 0.9);
      transition: all 0.15s ease;
      min-height: 28px;

      &:hover {
        border-color: rgba(74, 144, 226, 0.4);
      }

      &.is-focus {
        border-color: #4a90e2;
        box-shadow: 0 0 0 2px rgba(74, 144, 226, 0.1);
      }
    }

    :deep(.el-select--small .el-input__wrapper) {
      border-radius: 0.375rem;
      border: 1px solid rgba(156, 163, 175, 0.2);
      background: rgba(255, 255, 255, 0.9);
      min-height: 28px;
    }

    :deep(.el-input-number--small) {
      .el-input__wrapper {
        border-radius: 0.375rem;
        min-height: 28px;
      }
    }

    .row-index {
      font-weight: 600;
      color: #6b7280;
      font-size: 0.75rem;
    }
  }

  .empty-sub-processes {
    padding: 2rem;
    text-align: center;
    background: rgba(156, 163, 175, 0.02);
    border: 2px dashed rgba(156, 163, 175, 0.2);
    border-radius: 0.75rem;
  }
}

// 空状态
.empty-state {
  padding: $spacing-3xl;
  text-align: center;
}

// 动画效果
.sub-process-enter-active,
.sub-process-leave-active {
  transition: all 0.3s ease;
}

.sub-process-enter-from {
  opacity: 0;
  transform: translateY(-10px) scale(0.95);
}

.sub-process-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.95);
}

// 响应式设计
@media (max-width: 1024px) {
  .group-header-top {
    flex-direction: column;
    align-items: flex-start;

    .group-header-actions {
      align-self: flex-end;
    }
  }

  .sub-process-table {
    :deep(.el-table__body-wrapper) {
      overflow-x: auto;
    }
  }
}

@media (max-width: 640px) {
  .editor-header {
    padding: $spacing-xl;
  }

  .process-groups {
    padding: $spacing-lg;
  }

  .group-card :deep(.el-card__header),
  .group-card :deep(.el-card__body) {
    padding: 1rem;
  }

  .header-actions,
  .group-header-actions {
    flex-direction: column;
    width: 100%;
  }

  .group-meta {
    flex-direction: column;
  }

  .meta-field {
    flex-direction: column;
    align-items: flex-start !important;

    .field-label {
      min-width: auto;
    }
  }
}

.task-drawer {
  .task-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: $spacing-md;
  }

  .task-actions {
    display: flex;
    gap: $spacing-sm;
    justify-content: center;
    align-items: center;
  }

  .task-error-text {
    margin-top: 6px;
    color: #c30000;
    font-size: 12px;
    text-align: left;
    word-break: break-word;
  }
}

:deep(.task-row-highlight) {
  background: rgba(80, 200, 120, 0.12) !important;
}

// Element Plus 组件样式覆盖
:deep(.el-button) {
  border-radius: 0.5rem;
  font-weight: 500;
  transition: all 0.2s ease;
}

:deep(.el-tag) {
  border-radius: 0.375rem;
  font-weight: 500;
}

:deep(.el-card) {
  --el-card-border-color: transparent;
}

:deep(.el-empty) {
  --el-empty-padding: 20px 0;
}
</style>
