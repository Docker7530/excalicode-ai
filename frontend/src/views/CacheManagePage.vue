<template>
  <div class="cache-manage-page">
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
            <ElBreadcrumbItem>缓存管理</ElBreadcrumbItem>
          </ElBreadcrumb>
        </div>
        <div class="header-info">
          <h1 class="page-title">
            <ElIcon class="title-icon"><Coin /></ElIcon>
            缓存管理看板
          </h1>
          <p class="page-subtitle">
            监控缓存状态、查看缓存内容、管理缓存生命周期
          </p>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-section">
      <div class="stats-container">
        <div
          v-for="stat in cacheStats"
          :key="stat.cacheName"
          class="stat-card"
          @click="handleViewDetail(stat.cacheName)"
        >
          <div class="stat-header">
            <div class="stat-icon">
              <ElIcon><DataAnalysis /></ElIcon>
            </div>
            <div class="stat-title">{{ stat.cacheName }}</div>
          </div>
          <div class="stat-body">
            <div class="stat-row">
              <span class="stat-label">缓存大小</span>
              <span class="stat-value primary">{{ stat.size }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">命中率</span>
              <span class="stat-value success"
                >{{ stat.hitRate?.toFixed(2) }}%</span
              >
            </div>
            <div class="stat-row">
              <span class="stat-label">请求次数</span>
              <span class="stat-value">{{ stat.requestCount }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">命中次数</span>
              <span class="stat-value">{{ stat.hitCount }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">未命中次数</span>
              <span class="stat-value warning">{{ stat.missCount }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">驱逐次数</span>
              <span class="stat-value danger">{{ stat.evictionCount }}</span>
            </div>
          </div>
          <div class="stat-footer">
            <ElButton
              type="primary"
              size="small"
              :icon="View"
              @click.stop="handleViewDetail(stat.cacheName)"
            >
              查看详情
            </ElButton>
            <ElButton
              type="danger"
              size="small"
              :icon="Delete"
              @click.stop="handleClearCache(stat.cacheName)"
            >
              清空缓存
            </ElButton>
          </div>
        </div>
      </div>

      <ElEmpty
        v-if="!loading && !cacheStats.length"
        description="暂无缓存数据"
        :image-size="200"
        class="empty-state"
      />
    </div>

    <!-- 缓存详情对话框 -->
    <ElDialog
      v-model="detailDialogVisible"
      :title="`缓存详情: ${currentCacheName}`"
      width="900px"
      :close-on-click-modal="false"
      class="detail-dialog"
    >
      <div v-loading="detailLoading" class="detail-content">
        <!-- 统计信息 -->
        <div v-if="cacheDetail?.stats" class="detail-stats">
          <div class="detail-stat-item">
            <span class="label">缓存大小</span>
            <span class="value">{{ cacheDetail.stats.size }}</span>
          </div>
          <div class="detail-stat-item">
            <span class="label">命中率</span>
            <span class="value success">
              {{ cacheDetail.stats.hitRate?.toFixed(2) }}%
            </span>
          </div>
          <div class="detail-stat-item">
            <span class="label">总请求</span>
            <span class="value">{{ cacheDetail.stats.requestCount }}</span>
          </div>
          <div class="detail-stat-item">
            <span class="label">驱逐次数</span>
            <span class="value">{{ cacheDetail.stats.evictionCount }}</span>
          </div>
        </div>

        <!-- 缓存条目 -->
        <div class="detail-entries">
          <div class="entries-header">
            <h3>缓存条目</h3>
            <ElText v-if="cacheDetail?.truncated" type="warning" size="small">
              显示前 {{ cacheDetail?.entries?.length }} 条，共
              {{ cacheDetail?.totalEntries }} 条
            </ElText>
          </div>

          <ElTable :data="cacheDetail?.entries" border class="entries-table">
            <ElTableColumn label="键" prop="key" min-width="200">
              <template #default="{ row }">
                <ElText class="cache-key">{{ row.key }}</ElText>
              </template>
            </ElTableColumn>
            <ElTableColumn label="值类型" prop="valueType" width="150">
              <template #default="{ row }">
                <ElTag size="small">{{ row.valueType }}</ElTag>
              </template>
            </ElTableColumn>
            <ElTableColumn label="值" prop="value" min-width="300">
              <template #default="{ row }">
                <ElText class="cache-value" truncated>{{ row.value }}</ElText>
              </template>
            </ElTableColumn>
            <ElTableColumn label="操作" width="100" align="center">
              <template #default="{ row }">
                <ElButton
                  type="danger"
                  size="small"
                  link
                  :icon="Delete"
                  @click="handleEvictKey(row.key)"
                >
                  删除
                </ElButton>
              </template>
            </ElTableColumn>
          </ElTable>

          <ElEmpty
            v-if="!detailLoading && !cacheDetail?.entries?.length"
            description="缓存为空"
            :image-size="100"
          />
        </div>
      </div>

      <template #footer>
        <ElButton @click="detailDialogVisible = false">关闭</ElButton>
        <ElButton type="primary" :icon="RefreshRight" @click="loadCacheDetail">
          刷新
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup>
import {
  clearCache,
  evictCacheKey,
  getCacheDetail,
  getCacheStats,
} from '@/api/cacheManage';
import {
  ArrowLeft,
  Coin,
  DataAnalysis,
  Delete,
  RefreshRight,
  View,
} from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';

const router = useRouter();

// 缓存统计数据
const loading = ref(false);
const cacheStats = ref([]);

// 详情对话框
const detailDialogVisible = ref(false);
const detailLoading = ref(false);
const currentCacheName = ref('');
const cacheDetail = ref(null);

// 返回
const handleBack = () => {
  router.push('/backend-manage');
};

// 加载缓存统计
const loadCacheStats = async () => {
  loading.value = true;
  try {
    const data = await getCacheStats();
    cacheStats.value = data || [];
  } catch (error) {
    console.error('加载缓存统计失败:', error);
    ElMessage.error(error.message || '加载缓存统计失败');
  } finally {
    loading.value = false;
  }
};

// 查看详情
const handleViewDetail = async (cacheName) => {
  currentCacheName.value = cacheName;
  detailDialogVisible.value = true;
  await loadCacheDetail();
};

// 加载缓存详情
const loadCacheDetail = async () => {
  if (!currentCacheName.value) return;

  detailLoading.value = true;
  try {
    const data = await getCacheDetail(currentCacheName.value, 100);
    cacheDetail.value = data;
  } catch (error) {
    console.error('加载缓存详情失败:', error);
    ElMessage.error(error.message || '加载缓存详情失败');
  } finally {
    detailLoading.value = false;
  }
};

// 清空缓存
const handleClearCache = async (cacheName) => {
  try {
    await ElMessageBox.confirm(
      `确定清空缓存 "${cacheName}"? 这将删除所有缓存项。`,
      '清空确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    );

    await clearCache(cacheName);
    ElMessage.success('缓存已清空');
    await loadCacheStats();

    if (detailDialogVisible.value && currentCacheName.value === cacheName) {
      await loadCacheDetail();
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空缓存失败:', error);
      ElMessage.error(error.message || '清空缓存失败');
    }
  }
};

// 删除缓存键
const handleEvictKey = async (key) => {
  try {
    await ElMessageBox.confirm(`确定删除缓存项 "${key}"?`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });

    await evictCacheKey(currentCacheName.value, key);
    ElMessage.success('缓存项已删除');
    await loadCacheDetail();
    await loadCacheStats();
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除缓存项失败:', error);
      ElMessage.error(error.message || '删除缓存项失败');
    }
  }
};

// 初始化
onMounted(() => {
  loadCacheStats();
});
</script>

<style scoped lang="scss">
.cache-manage-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7eb 100%);
  padding-bottom: 40px;
}

// 页面头部
.page-header {
  position: relative;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
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

.page-subtitle {
  margin: 12px 0 0 0;
  font-size: 1rem;
  color: rgba(255, 255, 255, 0.85);
}

// 统计卡片
.stats-section {
  padding: 0 32px;
}

.stats-container {
  max-width: 1400px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 24px;
}

.stat-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  cursor: pointer;
  border: 2px solid transparent;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
    border-color: #f093fb;
  }
}

.stat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #f093fb, #f5576c);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
}

.stat-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #303133;
  flex: 1;
}

.stat-body {
  margin-bottom: 20px;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;

  &:last-child {
    border-bottom: none;
  }
}

.stat-label {
  font-size: 0.875rem;
  color: #606266;
  font-weight: 500;
}

.stat-value {
  font-size: 1rem;
  font-weight: 700;
  color: #303133;

  &.primary {
    color: #409eff;
  }

  &.success {
    color: #67c23a;
  }

  &.warning {
    color: #e6a23c;
  }

  &.danger {
    color: #f56c6c;
  }
}

.stat-footer {
  display: flex;
  gap: 8px;
}

.empty-state {
  max-width: 1400px;
  margin: 60px auto;
}

// 详情对话框
.detail-dialog {
  :deep(.el-dialog__header) {
    background: linear-gradient(135deg, #f093fb, #f5576c);
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
    padding: 24px;
  }

  :deep(.el-dialog__footer) {
    padding: 16px 24px;
    border-top: 1px solid #f0f0f0;
  }
}

.detail-content {
  min-height: 200px;
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.detail-stat-item {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;

  .label {
    font-size: 0.875rem;
    color: #606266;
    font-weight: 500;
  }

  .value {
    font-size: 1.5rem;
    font-weight: 700;
    color: #303133;

    &.success {
      color: #67c23a;
    }
  }
}

.detail-entries {
  margin-top: 24px;
}

.entries-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h3 {
    margin: 0;
    font-size: 1.125rem;
    font-weight: 600;
    color: #303133;
  }
}

.entries-table {
  border-radius: 12px;
  overflow: hidden;

  :deep(.cell) {
    padding: 12px;
  }
}

.cache-key {
  font-family: 'Courier New', monospace;
  font-size: 0.875rem;
  color: #409eff;
  font-weight: 600;
}

.cache-value {
  font-size: 0.875rem;
  color: #606266;
}
</style>
