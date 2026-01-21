<template>
  <div class="my-tasks-page">
    <div class="page-container">
      <ElCard class="card">
        <template #header>
          <div class="card-header">
            <ElIcon><Memo /></ElIcon>
            <span>我的大任务</span>
          </div>
        </template>

        <ElTable
          v-loading="loadingBatches"
          :data="batchList"
          border
          @row-click="openBatchDetail"
        >
          <ElTableColumn prop="title" label="批次标题" min-width="200">
            <template #default="{ row }">
              <div class="batch-title">
                <span>{{ row.title }}</span>
                <ElTag
                  v-if="
                    row.completedTasks === row.totalTasks && row.totalTasks > 0
                  "
                  type="success"
                  size="small"
                >
                  已完成
                </ElTag>
              </div>
              <div v-if="row.description" class="batch-desc">
                {{ row.description }}
              </div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="我的任务" width="140" align="center">
            <template #default="{ row }">
              <span>{{ row.completedTasks }}/{{ row.totalTasks }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn
            prop="totalWorkload"
            label="我的工作量"
            width="140"
            align="center"
          >
            <template #default="{ row }">
              {{ formatWorkload(row.totalWorkload) }} 人天
            </template>
          </ElTableColumn>
          <ElTableColumn prop="publishedTime" label="发布时间" width="180" />
          <ElTableColumn label="操作" width="140" align="center">
            <template #default="{ row }">
              <ElButton
                type="primary"
                text
                size="small"
                @click.stop="openBatchDetail(row)"
              >
                查看任务
              </ElButton>
            </template>
          </ElTableColumn>
        </ElTable>
        <ElEmpty
          v-if="!loadingBatches && !batchList.length"
          description="还没有分配给你的任务"
        />
      </ElCard>
    </div>

    <ElDrawer
      v-model="detailVisible"
      direction="btt"
      size="75vh"
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
                >我的任务：{{ activeBatch.completedTasks }}/{{
                  activeBatch.totalTasks
                }}</span
              >
              <span
                >工作量：{{
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
              label="工作量"
              width="120"
              align="center"
            >
              <template #default="{ row }">
                {{ row.workloadManDay }} 人天
              </template>
            </ElTableColumn>
            <ElTableColumn label="状态" width="140" align="center">
              <template #default="{ row }">
                <ElButton
                  size="small"
                  class="status-toggle__btn"
                  :type="row.status === 'COMPLETED' ? 'success' : 'danger'"
                  plain
                  @click="() => toggleStatus(row)"
                >
                  {{ row.status === 'COMPLETED' ? '已完成' : '未完成' }}
                </ElButton>
              </template>
            </ElTableColumn>
          </ElTable>
        </div>
        <ElEmpty v-else description="暂无任务" />
      </template>
    </ElDrawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  fetchMyTaskBatches,
  fetchMyTaskBatchDetail,
  updateTaskStatus,
} from '@/api/task.js';
import TaskDescriptionPopover from '@/components/TaskDescriptionPopover.vue';
import { Memo } from '@element-plus/icons-vue';

const batchList = ref([]);
const loadingBatches = ref(false);
const detailVisible = ref(false);
const activeBatch = ref(null);

const loadBatches = async () => {
  try {
    loadingBatches.value = true;
    const data = await fetchMyTaskBatches();
    batchList.value = (data ?? []).map((item) => ({
      ...item,
      publishedTime: formatDate(item.publishedTime),
    }));
  } catch (error) {
    ElMessage.error(error?.message ?? '加载任务列表失败');
  } finally {
    loadingBatches.value = false;
  }
};

const openBatchDetail = async (row) => {
  const batchId = typeof row === 'number' ? row : row.id;
  try {
    detailVisible.value = true;
    activeBatch.value = null;
    const detail = await fetchMyTaskBatchDetail(batchId);
    activeBatch.value = normalizeDetail(detail);
  } catch (error) {
    detailVisible.value = false;
    ElMessage.error(error?.message ?? '加载任务详情失败');
  }
};

const handleStatusChange = async (task, newStatus) => {
  if (!task || task.status === newStatus) return;
  const previous = task.status;
  try {
    await updateTaskStatus(task.id, newStatus);
    task.statusLabel = newStatus === 'COMPLETED' ? '已完成' : '未完成';
    ElMessage.success('任务状态已更新');
    await loadBatches();
    if (activeBatch.value) {
      const refreshed = await fetchMyTaskBatchDetail(activeBatch.value.id);
      activeBatch.value = normalizeDetail(refreshed);
    }
  } catch (error) {
    task.status = previous;
    ElMessage.error(error?.message ?? '任务状态更新失败');
  }
};

const toggleStatus = (task) => {
  if (!task) return;
  const nextStatus = task.status === 'COMPLETED' ? 'NOT_STARTED' : 'COMPLETED';
  handleStatusChange(task, nextStatus);
};

const normalizeDetail = (detail) => {
  if (!detail) return null;
  return {
    ...detail,
    publishedTime: formatDate(detail.publishedTime),
    tasks: (detail.tasks ?? []).map((item) => ({
      ...item,
      publishedTime: formatDate(item.publishedTime),
      updatedTime: formatDate(item.updatedTime),
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

onMounted(() => {
  loadBatches();
});
</script>

<style scoped lang="scss">
.my-tasks-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f1f5f9 0%, #ffffff 100%);
  padding: 96px 24px 40px;
  position: relative;
  z-index: 0;

  @media (max-width: 768px) {
    padding: 84px 16px 32px;
  }
}

.page-container {
  max-width: 1080px;
  margin: 0 auto;
}

.card {
  border-radius: 18px;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.08);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 16px;
  color: #1e293b;
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

.status-toggle__btn {
  min-width: 80px;
  font-size: 13px;
}

.detail-drawer {
  padding: 0;
}

.bottom-sheet {
  border-top-left-radius: 24px;
  border-top-right-radius: 24px;
}

.drawer-handle {
  width: 80px;
  height: 6px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.2);
  align-self: center;
  margin-bottom: 12px;
}

.detail-container {
  padding: 24px 32px 32px;
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  gap: 12px;
  font-size: 14px;
  color: #64748b;
}

.detail-container :deep(.el-table) {
  --el-table-header-bg-color: #f8fafc;
}
</style>
