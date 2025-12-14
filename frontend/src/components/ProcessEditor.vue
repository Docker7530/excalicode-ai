<!--
  功能过程在线列表编辑器
  提供简洁高效的表格式编辑体验，支持内联编辑、拖拽排序、快捷操作
-->
<template>
  <div class="process-editor">
    <input
      ref="fileInputRef"
      class="file-input"
      type="file"
      accept=".xlsx,.xls"
      @change="handleImportFile"
    />
    <!-- 头部操作区 -->
    <div class="editor-header">
      <div class="header-main">
        <h2>功能过程管理</h2>
        <div class="header-stats">
          <span class="stat-item">
            <ElIcon><List /></ElIcon>
            共 {{ processes.length }} 个过程
          </span>
        </div>
      </div>
      <div class="header-actions">
        <ElInput
          ref="searchInput"
          v-model="searchKeyword"
          placeholder="搜索过程..."
          clearable
          style="width: 280px"
          :prefix-icon="Search"
        />
        <div class="import-group">
          <ElButton
            type="primary"
            :icon="Upload"
            :loading="importing"
            @click="handleImportClick"
          >
            导入过程
          </ElButton>
          <ElTooltip placement="bottom" effect="light">
            <template #content>
              <div class="import-tooltip">
                <p class="tooltip-title">导入模板要求：</p>
                <ul class="tooltip-list">
                  <li>Sheet 名称：<strong>功能点拆分表</strong></li>
                  <li>必须包含列：<strong>功能过程</strong></li>
                </ul>
                <p class="tooltip-note">提示：使用我们的通用拆分模板</p>
              </div>
            </template>
            <ElIcon class="import-hint-icon">
              <QuestionFilled />
            </ElIcon>
          </ElTooltip>
        </div>
        <ElButton type="primary" :icon="Plus" @click="addNewProcess">
          新增过程
        </ElButton>
      </div>
    </div>

    <!-- 主编辑区域 -->
    <div class="editor-body">
      <div class="table-container">
        <!-- 数据表格 -->
        <ElTable
          v-if="filteredProcesses.length > 0"
          :data="filteredProcesses"
          class="process-table"
          stripe
          border
          highlight-current-row
          @row-click="handleRowClick"
        >
          <!-- 拖拽手柄列 -->
          <ElTableColumn width="60" align="center" class-name="drag-column">
            <template #default="{ $index }">
              <div
                class="drag-handle"
                :draggable="true"
                @dragstart="handleDragStart($index, $event)"
                @dragover="handleDragOver($event)"
                @drop="handleDrop($index, $event)"
                @dragend="handleDragEnd"
              >
                <ElIcon><Rank /></ElIcon>
              </div>
            </template>
          </ElTableColumn>

          <!-- 序号列 -->
          <ElTableColumn prop="index" label="#" width="60" align="center">
            <template #default="{ $index }">
              <span class="row-number">{{ $index + 1 }}</span>
            </template>
          </ElTableColumn>

          <!-- 过程描述列 -->
          <ElTableColumn prop="description" label="过程描述" min-width="400">
            <template #default="{ row, $index }">
              <div v-if="editingIndex === $index" class="inline-editor">
                <ElInput
                  v-model="editingText"
                  type="textarea"
                  :autosize="{ minRows: 1, maxRows: 4 }"
                  placeholder="请输入过程描述..."
                  @blur="finishEdit"
                  @keydown.enter.exact="finishEdit"
                  @keydown.escape="cancelEdit"
                />
              </div>
              <div
                v-else
                class="description-cell"
                @dblclick="startEdit($index, row.description)"
              >
                <span v-if="row.description" class="description-text">
                  {{ row.description }}
                </span>
                <span v-else class="description-placeholder">
                  双击编辑过程描述...
                </span>
              </div>
            </template>
          </ElTableColumn>

          <!-- 操作列 -->
          <ElTableColumn label="操作" width="140" align="center" fixed="right">
            <template #default="{ row, $index }">
              <div class="action-buttons">
                <ElTooltip content="编辑" placement="top">
                  <ElButton
                    :icon="Edit"
                    size="small"
                    circle
                    @click.stop="startEdit($index, row.description)"
                  />
                </ElTooltip>
                <ElTooltip content="复制" placement="top">
                  <ElButton
                    :icon="CopyDocument"
                    size="small"
                    circle
                    @click.stop="duplicateProcess(row, $index)"
                  />
                </ElTooltip>
                <ElTooltip content="删除" placement="top">
                  <ElButton
                    :icon="Delete"
                    type="danger"
                    size="small"
                    circle
                    @click.stop="deleteProcess($index)"
                  />
                </ElTooltip>
              </div>
            </template>
          </ElTableColumn>
        </ElTable>

        <!-- 空状态 -->
        <div v-else-if="processes.length === 0" class="empty-state">
          <ElEmpty description="还没有创建任何过程" :image-size="120">
            <ElButton type="primary" :icon="Plus" @click="addNewProcess">
              创建第一个过程
            </ElButton>
          </ElEmpty>
        </div>

        <!-- 搜索无结果 -->
        <div v-else class="search-empty">
          <ElEmpty description="没有找到匹配的过程" :image-size="100">
            <ElButton type="info" text :icon="RefreshLeft" @click="resetSearch">
              清除搜索条件
            </ElButton>
          </ElEmpty>
        </div>
      </div>
    </div>

    <!-- 底部操作区 -->
    <div v-if="processes.length > 0" class="editor-footer">
      <div class="footer-note">
        <ElIcon><InfoFilled /></ElIcon>
        <span>完善所有过程描述后可进入下一步</span>
      </div>
      <div class="footer-actions">
        <ElButton
          type="success"
          size="large"
          :disabled="!canConfirm"
          :icon="DocumentChecked"
          @click="confirmProcesses"
        >
          生成子过程
        </ElButton>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 功能过程列表编辑器
 * 支持导入、排序、搜索、内联编辑等高频操作
 */

import { cosmicService } from '@/api';
import {
  CopyDocument,
  Delete,
  DocumentChecked,
  Edit,
  InfoFilled,
  List,
  Plus,
  QuestionFilled,
  Rank,
  RefreshLeft,
  Search,
  Upload,
} from '@element-plus/icons-vue';

const props = defineProps({
  initialProcesses: {
    type: Array,
    default: () => [],
  },
  readonly: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['update:processes', 'confirm', 'process-change']);

// ==================== 响应式状态 ====================
const processes = ref([]);
const nextId = ref(1);
const searchKeyword = ref('');
const editingIndex = ref(-1);
const editingText = ref('');
const searchInput = ref(null);
const fileInputRef = ref(null);
const importing = ref(false);

// 拖拽状态
const dragState = reactive({
  startIndex: null,
  draggedData: null,
});

// ==================== 计算属性 ====================
const filteredProcesses = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  if (!keyword) return processes.value;

  return processes.value.filter((process) => {
    const content = `${process.description ?? ''}`.toLowerCase();
    return content.includes(keyword);
  });
});

const canConfirm = computed(
  () =>
    processes.value.length > 0 &&
    processes.value.every((process) => process.description?.trim()),
);

// ==================== 方法 ====================
function updateSortIndexes() {
  processes.value.forEach((process, index) => {
    process.sortIndex = index + 1;
  });
}

function emitProcessChange() {
  updateSortIndexes();
  emit('update:processes', processes.value);
  emit('process-change', processes.value);
}

/**
 * 触发隐藏的文件选择器
 */
function handleImportClick() {
  if (props.readonly || importing.value) {
    return;
  }
  fileInputRef.value?.click();
}

/**
 * 处理 Excel 导入并同步更新过程列表
 */
async function handleImportFile(event) {
  const [file] = event.target?.files ?? [];
  if (!file) {
    return;
  }

  const isExcel = /\.(xlsx|xls)$/i.test(file.name);
  if (!isExcel) {
    ElMessage.error('请上传 Excel 文件（.xlsx/.xls）');
    event.target.value = '';
    return;
  }

  importing.value = true;
  try {
    const result = await cosmicService.importFunctionalProcesses(file);
    const imported = (result?.functionalProcesses ?? [])
      .map((item, index) => ({
        id: `process_${Date.now()}_${index}`,
        description: (item?.description || '').trim(),
      }))
      .filter((item) => item.description);

    if (!imported.length) {
      ElMessage.warning('未解析到有效的功能过程，请检查 Excel 内容');
      return;
    }

    processes.value = imported;
    nextId.value = processes.value.length + 1;
    emitProcessChange();
    ElMessage.success('功能过程已导入，可继续编辑');
  } catch (error) {
    ElMessage.error(error.message || '功能过程导入失败，请稍后重试');
  } finally {
    importing.value = false;
    if (event?.target) {
      event.target.value = '';
    }
  }
}

/**
 * 新增空白过程，并自动聚焦到编辑模式
 */
function addNewProcess() {
  const newProcess = {
    id: `process_${Date.now()}_${nextId.value++}`,
    description: '',
  };

  processes.value.push(newProcess);
  emitProcessChange();

  // 自动编辑新添加的过程
  nextTick(() => {
    const newIndex = processes.value.length - 1;
    startEdit(newIndex, '');
  });

  ElMessage.success('已添加新过程');
}

/**
 * 删除指定过程，操作前弹出确认
 */
async function deleteProcess(index) {
  try {
    await ElMessageBox.confirm('确定要删除这个过程吗？', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });

    processes.value.splice(index, 1);
    emitProcessChange();
    ElMessage.success('过程已删除');
  } catch {
    // 用户取消删除
  }
}

/**
 * 复制一条过程，附带“副本”后缀便于识别
 */
function duplicateProcess(process, index) {
  const newProcess = {
    id: `process_${Date.now()}_${nextId.value++}`,
    description: process.description ? `${process.description}（副本）` : '',
  };

  processes.value.splice(index + 1, 0, newProcess);
  emitProcessChange();
  ElMessage.success('已复制过程');
}

function resetSearch() {
  searchKeyword.value = '';
}

// ==================== 交互处理 ====================
/**
 * 点击行时进入编辑，保持操作连贯
 */
function handleRowClick(row, column) {
  if (column?.property === 'description') {
    const index = processes.value.findIndex((p) => p.id === row.id);
    if (index !== -1) {
      startEdit(index, row.description);
    }
  }
}

/**
 * 启动行内编辑状态
 */
function startEdit(index, text) {
  if (props.readonly) return;

  editingIndex.value = index;
  editingText.value = text || '';

  nextTick(() => {
    const editElement = document.querySelector(
      '.inline-editor .el-textarea__inner',
    );
    if (editElement) {
      editElement.focus();
    }
  });
}

/**
 * 保存当前编辑内容并退出编辑态
 */
function finishEdit() {
  if (editingIndex.value === -1) return;

  const trimmedText = editingText.value.trim();
  if (!trimmedText) {
    ElMessage.warning('过程描述不能为空');
    return;
  }

  processes.value[editingIndex.value].description = trimmedText;
  emitProcessChange();

  editingIndex.value = -1;
  editingText.value = '';
}

/**
 * 终止编辑并恢复原状
 */
function cancelEdit() {
  editingIndex.value = -1;
  editingText.value = '';
}

// 拖拽功能
/**
 * 开始拖拽时记录起始位置
 */
function handleDragStart(index, event) {
  if (props.readonly) {
    event.preventDefault();
    return;
  }

  dragState.startIndex = index;
  dragState.draggedData = { ...processes.value[index] };

  event.dataTransfer.effectAllowed = 'move';
  event.dataTransfer.setData('text/plain', index.toString());
}

/**
 * 拖拽经过目标行时允许放置
 */
function handleDragOver(event) {
  if (dragState.startIndex === null) return;
  event.preventDefault();
  event.dataTransfer.dropEffect = 'move';
}

/**
 * 在目标位置放置过程并更新列表顺序
 */
function handleDrop(targetIndex, event) {
  if (dragState.startIndex === null || dragState.startIndex === targetIndex)
    return;

  event.preventDefault();

  const startIndex = dragState.startIndex;
  const [movedItem] = processes.value.splice(startIndex, 1);
  processes.value.splice(targetIndex, 0, movedItem);

  emitProcessChange();

  ElMessage.success('过程顺序已调整');
}

/**
 * 拖拽结束后清理状态
 */
function handleDragEnd() {
  dragState.startIndex = null;
  dragState.draggedData = null;
}

/**
 * 提交全部过程，校验必填项后通知父组件 (V1 稳定版本)
 */
function confirmProcesses() {
  const emptyProcesses = processes.value.filter(
    (process) => !process.description?.trim(),
  );

  if (emptyProcesses.length > 0) {
    ElMessage.error('请完善所有过程的描述后再确认');
    return;
  }

  if (processes.value.length === 0) {
    ElMessage.error('至少需要一个过程');
    return;
  }

  updateSortIndexes();
  emit('confirm', processes.value);
}

// 初始化
/**
 * 根据外部初始值重建内部过程列表
 */
function initializeProcesses() {
  if (props.initialProcesses?.length > 0) {
    processes.value = props.initialProcesses.map((process, index) => ({
      ...process,
      sortIndex: index + 1,
    }));
    nextId.value = processes.value.length + 1;
  } else {
    processes.value = [];
    nextId.value = 1;
  }
}

// ==================== 监听逻辑 ====================
watch(
  () => props.initialProcesses,
  () => initializeProcesses(),
  { immediate: true, deep: true },
);
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

    .import-group {
      display: flex;
      align-items: center;
      gap: $spacing-xs;

      .import-hint-icon {
        font-size: $font-size-lg;
        color: $secondary-color;
        cursor: help;
        transition: all $transition-fast;

        &:hover {
          color: $accent-color;
          transform: scale(1.1);
        }
      }
    }
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

.table-container {
  flex: 1;
  padding: $spacing-2xl $spacing-3xl;
  display: flex;
  flex-direction: column;
  gap: $spacing-lg;
  overflow: auto;
}

.process-table {
  flex: 1;
  border-radius: $border-radius-xl;
  overflow: hidden;

  :deep(.el-table__header) {
    th {
      background: rgba($background-secondary, 0.8);
      color: $primary-color;
      font-weight: $font-weight-semibold;
    }
  }

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

  .row-number {
    width: 24px;
    height: 24px;
    border-radius: $border-radius-full;
    background: rgba($accent-color, 0.12);
    color: $accent-color;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: $font-weight-semibold;
    font-size: $font-size-sm;
    margin: 0 auto;
  }

  .type-tag {
    cursor: pointer;
    transition: all $transition-fast;

    &:hover {
      transform: scale(1.05);
    }

    &.clickable {
      user-select: none;
    }
  }

  .description-cell {
    padding: $spacing-sm;
    min-height: 40px;
    border-radius: $border-radius-md;
    cursor: pointer;
    transition: background-color $transition-fast;

    &:hover {
      background: rgba($accent-color, 0.05);
    }

    .description-text {
      color: $primary-color;
      line-height: 1.5;
    }

    .description-placeholder {
      color: $secondary-color;
      font-style: italic;
    }
  }

  .inline-editor {
    :deep(.el-textarea__inner) {
      border: 2px solid $accent-color;
      box-shadow: 0 0 0 3px rgba($accent-color, 0.12);
    }
  }

  .action-buttons {
    display: flex;
    gap: $spacing-xs;
    justify-content: center;
  }
}

.empty-state,
.search-empty {
  padding: $spacing-3xl;
  text-align: center;
}

.editor-footer {
  padding: $spacing-xl $spacing-3xl;
  border-top: 1px solid $border-light;
  background: rgba($background-secondary, 0.6);
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: $spacing-lg;
  flex-wrap: wrap;

  &:has(.footer-note) {
    justify-content: space-between;
  }

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

// Tooltip 样式
.import-tooltip {
  max-width: 300px;
  padding: $spacing-sm;

  .tooltip-title {
    margin: 0 0 $spacing-sm 0;
    font-weight: $font-weight-semibold;
    color: $primary-color;
    font-size: $font-size-base;
  }

  .tooltip-list {
    margin: 0 0 $spacing-sm 0;
    padding-left: $spacing-lg;
    list-style: none;

    li {
      margin: $spacing-xs 0;
      color: $primary-color;
      font-size: $font-size-sm;
      line-height: 1.5;

      &::before {
        content: '•';
        color: $accent-color;
        font-weight: bold;
        display: inline-block;
        width: 1em;
        margin-left: -1em;
      }

      strong {
        color: $accent-color;
        font-weight: $font-weight-medium;
      }
    }
  }

  .tooltip-note {
    margin: $spacing-sm 0 0 0;
    padding: $spacing-xs $spacing-sm;
    background: rgba($accent-color, 0.08);
    border-radius: $border-radius-md;
    color: $secondary-color;
    font-size: $font-size-xs;
    font-style: italic;
  }
}
</style>
