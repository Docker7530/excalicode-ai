<template>
  <div class="task-allocation-page">
    <div class="page-shell">
      <div class="view-switch">
        <button
          class="switch-card"
          :class="{ active: activePanel === 'import' }"
          type="button"
          @click="switchPanel('import')"
        >
          <ElIcon><UploadFilled /></ElIcon>
          <div class="switch-text">
            <h3>导入任务模板</h3>
          </div>
        </button>
        <button
          class="switch-card"
          :class="{ active: activePanel === 'overview' }"
          type="button"
          @click="switchPanel('overview')"
        >
          <ElIcon><List /></ElIcon>
          <div class="switch-text">
            <h3>任务批次总览</h3>
          </div>
        </button>
      </div>

      <Transition name="fade-slide" mode="out-in">
        <section
          v-if="activePanel === 'import'"
          key="import"
          class="panel-wrapper"
        >
          <ElCard class="card publish-card">
            <template #header>
              <div class="card-header">
                <ElIcon><UploadFilled /></ElIcon>
                <span>导入任务模板</span>
              </div>
            </template>
            <div class="import-controls">
              <ElUpload
                :auto-upload="false"
                :show-file-list="false"
                accept=".xlsx,.xls"
                :on-change="handleFileChange"
              >
                <ElButton type="primary" :loading="importing"
                  >上传 Excel</ElButton
                >
              </ElUpload>
              <p class="hint">
                模板需包含「需求标题」「需求描述」「计入工作量(人天)」三列。导入后可为每条记录指定执行人并发布。
              </p>
            </div>
            <ElForm
              v-if="drafts.length"
              :model="batchForm"
              label-width="88px"
              class="batch-form"
            >
              <ElFormItem label="批次标题" required>
                <ElInput
                  v-model="batchForm.title"
                  placeholder="请输入大任务标题，如：四季度需求拆解"
                  maxlength="100"
                  show-word-limit
                />
              </ElFormItem>
              <ElFormItem label="批次说明">
                <ElInput
                  v-model="batchForm.description"
                  placeholder="可选：补充背景或注意事项"
                  type="textarea"
                  :rows="2"
                  maxlength="200"
                  show-word-limit
                />
              </ElFormItem>
            </ElForm>

            <div v-if="drafts.length" class="draft-table-wrapper">
              <div class="table-caption">
                批次包含 {{ drafts.length }} 条任务，请填写执行人：
              </div>
              <ElTable :data="drafts" border>
                <ElTableColumn
                  prop="rowIndex"
                  label="行号"
                  width="80"
                  align="center"
                />
                <ElTableColumn prop="title" label="任务标题" min-width="180">
                  <template #default="{ row }">
                    <ElTooltip :content="row.title" placement="top">
                      <span class="text-ellipsis">{{ row.title }}</span>
                    </ElTooltip>
                  </template>
                </ElTableColumn>
                <ElTableColumn
                  prop="description"
                  label="任务描述"
                  min-width="220"
                >
                  <template #default="{ row }">
                    <TaskDescriptionPopover
                      class="description-cell"
                      :text="row.description"
                      :width="420"
                      :max-preview-lines="2"
                    />
                  </template>
                </ElTableColumn>
                <ElTableColumn
                  prop="workloadManDay"
                  label="工作量(人天)"
                  width="140"
                  align="center"
                />
                <ElTableColumn label="执行人" width="220">
                  <template #default="{ row }">
                    <ElSelect
                      v-model="assignmentMap[row.rowIndex]"
                      placeholder="选择执行人"
                      clearable
                      style="width: 180px"
                    >
                      <ElOption
                        v-for="user in assignees"
                        :key="user.id"
                        :label="user.username"
                        :value="user.id"
                      />
                    </ElSelect>
                  </template>
                </ElTableColumn>
              </ElTable>
            </div>

            <div v-if="drafts.length" class="actions">
              <ElButton
                type="primary"
                :loading="publishing"
                :disabled="!canPublish"
                @click="handlePublish"
              >
                发布任务批次
              </ElButton>
              <ElButton text @click="resetDrafts">清空草稿</ElButton>
            </div>
          </ElCard>
        </section>
        <section v-else key="overview" class="panel-wrapper">
          <ElCard class="card overview-card">
            <template #header>
              <div class="card-header">
                <ElIcon><List /></ElIcon>
                <span>任务批次总览</span>
              </div>
            </template>
            <ElTable v-loading="loadingBatches" :data="batchList" border>
              <ElTableColumn prop="title" label="批次标题" min-width="200">
                <template #default="{ row }">
                  <div class="batch-title">
                    <span>{{ row.title }}</span>
                    <ElTag
                      v-if="
                        row.totalTasks === row.completedTasks &&
                        row.totalTasks > 0
                      "
                      size="small"
                      type="success"
                    >
                      已完成
                    </ElTag>
                  </div>
                  <div v-if="row.description" class="batch-desc">
                    {{ row.description }}
                  </div>
                </template>
              </ElTableColumn>
              <ElTableColumn label="完成度" width="140" align="center">
                <template #default="{ row }">
                  <span>{{ row.completedTasks }}/{{ row.totalTasks }}</span>
                </template>
              </ElTableColumn>
              <ElTableColumn
                prop="totalWorkload"
                label="总工作量"
                width="140"
                align="center"
              >
                <template #default="{ row }">
                  {{ formatWorkload(row.totalWorkload) }} 人天
                </template>
              </ElTableColumn>
              <ElTableColumn
                prop="publishedTime"
                label="发布时间"
                width="180"
              />
              <ElTableColumn prop="createdByName" label="发布人" width="120" />
              <ElTableColumn label="操作" width="140" align="center">
                <template #default="{ row }">
                  <ElButton
                    type="primary"
                    text
                    size="small"
                    @click="openBatchDetail(row.id)"
                  >
                    查看详情
                  </ElButton>
                </template>
              </ElTableColumn>
            </ElTable>
          </ElCard>
        </section>
      </Transition>
    </div>

    <ElDrawer
      v-model="detailVisible"
      direction="btt"
      size="78vh"
      :with-header="false"
      custom-class="detail-drawer bottom-sheet"
    >
      <template #default>
        <div v-if="activeBatch" class="detail-container">
          <div class="drawer-handle" aria-hidden="true" />
          <div class="detail-header">
            <div>
              <h2 class="detail-title">{{ activeBatch.title }}</h2>
              <p v-if="activeBatch.description" class="detail-desc">
                {{ activeBatch.description }}
              </p>
            </div>
            <div class="detail-meta">
              <span
                >任务数：{{ activeBatch.completedTasks }}/{{
                  activeBatch.totalTasks
                }}</span
              >
              <span
                >总工作量：{{
                  formatWorkload(activeBatch.totalWorkload)
                }}
                人天</span
              >
              <span>发布时间：{{ formatDate(activeBatch.publishedTime) }}</span>
            </div>
          </div>

          <ElTable :data="activeBatch.tasks" border stripe>
            <ElTableColumn prop="title" label="任务标题" min-width="200">
              <template #default="{ row }">
                <ElTooltip :content="row.title" placement="top">
                  <span class="text-ellipsis">{{ row.title }}</span>
                </ElTooltip>
              </template>
            </ElTableColumn>
            <ElTableColumn prop="description" label="任务描述" min-width="240">
              <template #default="{ row }">
                <div class="description-actions">
                  <TaskDescriptionPopover
                    class="description-cell"
                    :text="row.description"
                    :width="420"
                    :max-preview-lines="2"
                  />
                  <ElButton
                    link
                    type="primary"
                    size="small"
                    @click="openEditDescription(row)"
                    >编辑</ElButton
                  >
                </div>
              </template>
            </ElTableColumn>
            <ElTableColumn
              prop="workloadManDay"
              label="工作量"
              width="120"
              align="center"
            >
              <template #default="{ row }">
                {{ row.workloadManDay }} 人天
              </template>
            </ElTableColumn>
            <ElTableColumn label="执行人" width="220">
              <template #default="{ row }">
                <ElSelect
                  v-model="row.assigneeId"
                  placeholder="选择执行人"
                  style="width: 180px"
                  @change="(value) => handleDetailAssigneeChange(row, value)"
                >
                  <ElOption
                    v-for="user in assignees"
                    :key="user.id"
                    :label="user.username"
                    :value="user.id"
                  />
                </ElSelect>
              </template>
            </ElTableColumn>
            <ElTableColumn label="状态" width="140" align="center">
              <template #default="{ row }">
                <ElTag
                  :type="row.status === 'COMPLETED' ? 'success' : 'info'"
                  >{{ row.statusLabel }}</ElTag
                >
              </template>
            </ElTableColumn>
          </ElTable>
        </div>
        <ElEmpty v-else description="暂无数据" />
      </template>
    </ElDrawer>

    <ElDialog
      v-model="descriptionDialogVisible"
      title="修改任务描述"
      width="640px"
      :close-on-click-modal="false"
    >
      <ElInput
        v-model="descriptionDraft"
        type="textarea"
        :rows="6"
        placeholder="请输入任务描述"
        maxlength="2000"
        show-word-limit
      />
      <template #footer>
        <ElButton @click="descriptionDialogVisible = false">取消</ElButton>
        <ElButton
          type="primary"
          :loading="savingDescription"
          @click="saveDescription"
          >保存</ElButton
        >
      </template>
    </ElDialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  importTaskDrafts,
  publishTaskBatch,
  fetchTaskBatches,
  fetchTaskBatchDetail,
  updateTaskAssignee,
  updateTaskDescription,
  fetchAssignableUsers,
} from '@/api/task.js';
import TaskDescriptionPopover from '@/components/TaskDescriptionPopover.vue';
import { List, UploadFilled } from '@element-plus/icons-vue';

const drafts = ref([]);
const activePanel = ref('import');
const batchForm = reactive({ title: '', description: '' });
const assignmentMap = reactive({});
const assignees = ref([]);
const importing = ref(false);
const publishing = ref(false);

const batchList = ref([]);
const loadingBatches = ref(false);
const activeBatch = ref(null);
const detailVisible = ref(false);

const descriptionDialogVisible = ref(false);
const editingDescriptionTask = ref(null);
const descriptionDraft = ref('');
const savingDescription = ref(false);

const switchPanel = (panel) => {
  activePanel.value = panel;
};

const resetDrafts = () => {
  drafts.value = [];
  batchForm.title = '';
  batchForm.description = '';
  Object.keys(assignmentMap).forEach((key) => delete assignmentMap[key]);
};

const loadAssignees = async () => {
  try {
    const data = await fetchAssignableUsers();
    assignees.value = data ?? [];
  } catch (error) {
    ElMessage.error(error?.message ?? '获取执行人列表失败');
  }
};

const loadBatches = async () => {
  try {
    loadingBatches.value = true;
    const data = await fetchTaskBatches();
    batchList.value = (data ?? []).map((item) => ({
      ...item,
      publishedTime: formatDate(item.publishedTime),
    }));
  } catch (error) {
    ElMessage.error(error?.message ?? '获取任务批次失败');
  } finally {
    loadingBatches.value = false;
  }
};

const handleFileChange = async (uploadFile) => {
  try {
    importing.value = true;
    const raw = uploadFile?.raw ?? uploadFile;
    const draftsResult = await importTaskDrafts(raw);
    drafts.value = draftsResult ?? [];
    Object.keys(assignmentMap).forEach((key) => delete assignmentMap[key]);
    ElMessage.success(`成功导入 ${drafts.value.length} 条任务，请填写批次信息`);
  } catch (error) {
    ElMessage.error(error?.message ?? '导入失败');
  } finally {
    importing.value = false;
  }
};

const canPublish = computed(() => {
  if (!drafts.value.length) return false;
  if (!batchForm.title.trim()) return false;
  return drafts.value.every((draft) => assignmentMap[draft.rowIndex]);
});

const handlePublish = async () => {
  if (!canPublish.value) {
    ElMessage.warning('请完善批次标题和执行人信息');
    return;
  }
  const payload = {
    batchTitle: batchForm.title.trim(),
    batchDescription: batchForm.description?.trim() || null,
    tasks: drafts.value.map((draft) => ({
      rowIndex: draft.rowIndex,
      title: draft.title,
      description: draft.description,
      workloadManDay: draft.workloadManDay,
      assigneeId: assignmentMap[draft.rowIndex],
    })),
  };
  try {
    publishing.value = true;
    const detail = await publishTaskBatch(payload);
    ElMessage.success('任务批次发布成功');
    resetDrafts();
    await loadBatches();
    if (detail) {
      activeBatch.value = normalizeBatchDetail(detail);
      detailVisible.value = true;
    }
  } catch (error) {
    ElMessage.error(error?.message ?? '发布失败');
  } finally {
    publishing.value = false;
  }
};

const openBatchDetail = async (batchId) => {
  try {
    detailVisible.value = true;
    activeBatch.value = null;
    const detail = await fetchTaskBatchDetail(batchId);
    activeBatch.value = normalizeBatchDetail(detail);
  } catch (error) {
    detailVisible.value = false;
    ElMessage.error(error?.message ?? '获取批次详情失败');
  }
};

const handleDetailAssigneeChange = async (task, newAssigneeId) => {
  if (!newAssigneeId) {
    ElMessage.warning('执行人不能为空');
    return;
  }
  const previous = task.assigneeId;
  try {
    const updated = await updateTaskAssignee(
      task.batchId,
      task.id,
      newAssigneeId,
    );
    Object.assign(task, updated);
    ElMessage.success('执行人已更新');
    await loadBatches();
  } catch (error) {
    task.assigneeId = previous;
    ElMessage.error(error?.message ?? '执行人更新失败');
  }
};

const openEditDescription = (task) => {
  editingDescriptionTask.value = task;
  descriptionDraft.value = task?.description ?? '';
  descriptionDialogVisible.value = true;
};

const saveDescription = async () => {
  const task = editingDescriptionTask.value;
  if (!task) {
    descriptionDialogVisible.value = false;
    return;
  }
  const nextDescription = (descriptionDraft.value ?? '').trim();
  if (!nextDescription) {
    ElMessage.warning('任务描述不能为空');
    return;
  }

  const previous = task.description;
  try {
    savingDescription.value = true;
    const updated = await updateTaskDescription(
      task.batchId,
      task.id,
      nextDescription,
    );
    task.description = updated?.description ?? nextDescription;
    if (updated?.updatedTime) {
      task.updatedTime = formatDate(updated.updatedTime);
    }
    ElMessage.success('任务描述已更新');
    descriptionDialogVisible.value = false;
  } catch (error) {
    task.description = previous;
    ElMessage.error(error?.message ?? '任务描述更新失败');
  } finally {
    savingDescription.value = false;
  }
};

const normalizeBatchDetail = (detail) => {
  if (!detail) return null;
  return {
    ...detail,
    publishedTime: formatDate(detail.publishedTime),
    tasks: (detail.tasks ?? []).map((task) => ({
      ...task,
      publishedTime: formatDate(task.publishedTime),
      updatedTime: formatDate(task.updatedTime),
    })),
  };
};

const formatDate = (value) => {
  if (!value) return '-';
  return value.replace('T', ' ').split('.')[0] ?? value;
};

const formatWorkload = (value) => {
  if (!value && value !== 0) return '0';
  return Number(value).toFixed(2);
};

onMounted(async () => {
  await Promise.all([loadAssignees(), loadBatches()]);
});
</script>

<style scoped lang="scss">
.task-allocation-page {
  min-height: 100vh;
  background: #ffffff;
  padding: 96px 24px 40px;
  position: relative;
  z-index: 0;

  @media (max-width: 768px) {
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
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.switch-card {
  border: 1px solid rgba(99, 102, 241, 0.2);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  padding: 16px 20px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
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
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
  color: #334155;
}

.import-controls {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.hint {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.batch-form {
  margin-top: 16px;
}

.draft-table-wrapper {
  margin-top: 12px;
}

.table-caption {
  margin-bottom: 8px;
  font-size: 14px;
  color: #475569;
}

.actions {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.text-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;

  &.multiline {
    white-space: normal;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
}

.description-cell {
  display: block;
}

.description-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.batch-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1e293b;
}

.batch-desc {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.25s ease;
}

.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

.detail-drawer {
  padding: 0;
}

.bottom-sheet {
  border-top-left-radius: 24px;
  border-top-right-radius: 24px;
}

.detail-container {
  padding: 24px 32px 32px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.drawer-handle {
  width: 80px;
  height: 6px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.2);
  align-self: center;
  margin-bottom: 12px;
}

.detail-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #1e293b;
}

.detail-desc {
  margin: 4px 0 0;
  color: #475569;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #64748b;
  font-size: 14px;
}

.detail-container :deep(.el-table) {
  --el-table-header-bg-color: #f8fafc;
}
</style>
