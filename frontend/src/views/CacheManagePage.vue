<template>
  <div class="cache-manage-page">
    <AppHeader />

    <section class="page-body">
      <header class="page-heading">
        <span class="heading-meta">
          {{
            readableLastUpdated
              ? `上次刷新：${readableLastUpdated}`
              : '尚未刷新'
          }}
        </span>
        <ElButton
          type="primary"
          plain
          :loading="loading"
          :icon="RefreshRight"
          @click="loadCacheStats"
        >
          刷新全部
        </ElButton>
      </header>

      <div class="summary-grid">
        <div class="summary-card">
          <span class="summary-label">缓存数量</span>
          <span class="summary-value">{{ summary.totalCaches }}</span>
        </div>
        <div class="summary-card">
          <span class="summary-label">条目总数</span>
          <span class="summary-value">{{ summary.totalEntries }}</span>
        </div>
        <div class="summary-card">
          <span class="summary-label">平均命中率</span>
          <span class="summary-value">{{ summary.avgHitRate }}</span>
        </div>
      </div>

      <ElCard shadow="never" class="list-card">
        <div class="list-toolbar">
          <ElInput
            v-model="filterKeyword"
            placeholder="输入缓存名称关键字"
            size="small"
            clearable
            :prefix-icon="Search"
            class="toolbar-search"
          />
          <ElSelect v-model="sortBy" size="small" class="toolbar-sort">
            <ElOption label="按条目数量" value="sizeDesc" />
            <ElOption label="按命中率" value="hitRateDesc" />
            <ElOption label="按缓存名称" value="nameAsc" />
          </ElSelect>
        </div>

        <ElTable
          v-loading="loading"
          :data="filteredStats"
          stripe
          border
          class="cache-table"
        >
          <ElTableColumn prop="cacheName" label="缓存名称" min-width="200">
            <template #default="{ row }">
              <div class="name-cell">
                <span class="cache-name">{{ row.cacheName }}</span>
                <ElTag v-if="!row.size" size="small" effect="plain">空</ElTag>
              </div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="条目数" prop="size" width="110" />
          <ElTableColumn label="命中率" width="130">
            <template #default="{ row }">
              <span :class="['rate-chip', getRateTone(row.hitRate)]">
                {{ formatHitRate(row.hitRate) }}
              </span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="请求次数" prop="requestCount" width="140" />
          <ElTableColumn label="命中 / 未命中" width="170">
            <template #default="{ row }">
              <span class="compact-stat">{{ row.hitCount }}</span>
              <span class="compact-divider">/</span>
              <span class="compact-stat warning">{{ row.missCount }}</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="驱逐次数" prop="evictionCount" width="140" />
          <ElTableColumn label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <ElButton
                type="primary"
                link
                size="small"
                :icon="View"
                @click="handleViewDetail(row.cacheName)"
              >
                详情
              </ElButton>
              <ElButton
                type="danger"
                link
                size="small"
                :icon="Delete"
                @click="handleClearCache(row.cacheName)"
              >
                清空
              </ElButton>
            </template>
          </ElTableColumn>
        </ElTable>

        <ElEmpty
          v-if="!loading && !filteredStats.length"
          description="未找到匹配的缓存"
          class="list-empty"
        />
      </ElCard>
    </section>

    <ElDrawer
      v-model="detailDrawerVisible"
      :title="currentCacheName ? `缓存详情：${currentCacheName}` : '缓存详情'"
      size="520px"
      :close-on-click-modal="false"
      destroy-on-close
      class="detail-drawer"
    >
      <div v-loading="detailLoading" class="drawer-body">
        <template v-if="cacheDetail">
          <div class="detail-stat-list">
            <div class="detail-stat">
              <span class="label">条目数</span>
              <span class="value">{{ cacheDetail.stats?.size ?? '-' }}</span>
            </div>
            <div class="detail-stat">
              <span class="label">命中率</span>
              <span class="value">{{
                formatHitRate(cacheDetail.stats?.hitRate)
              }}</span>
            </div>
            <div class="detail-stat">
              <span class="label">请求次数</span>
              <span class="value">{{
                cacheDetail.stats?.requestCount ?? '-'
              }}</span>
            </div>
            <div class="detail-stat">
              <span class="label">驱逐次数</span>
              <span class="value">{{
                cacheDetail.stats?.evictionCount ?? '-'
              }}</span>
            </div>
          </div>

          <ElAlert
            v-if="cacheDetail?.truncated"
            type="info"
            :closable="false"
            class="truncate-alert"
          >
            已显示前 {{ cacheDetail?.entries?.length }} 条，共
            {{ cacheDetail?.totalEntries }} 条
          </ElAlert>

          <ElTable
            :data="cacheDetail?.entries"
            size="small"
            border
            class="detail-table"
            :header-cell-style="{ backgroundColor: '#fafafa' }"
          >
            <ElTableColumn label="键" prop="key" min-width="200">
              <template #default="{ row }">
                <code class="entry-key">{{ row.key }}</code>
              </template>
            </ElTableColumn>
            <ElTableColumn label="值类型" prop="valueType" width="140">
              <template #default="{ row }">
                <ElTag type="info" effect="plain" size="small">
                  {{ row.valueType }}
                </ElTag>
              </template>
            </ElTableColumn>
            <ElTableColumn label="值" prop="value" min-width="220">
              <template #default="{ row }">
                <ElTooltip
                  effect="dark"
                  :content="row.value"
                  placement="top-start"
                  :disabled="!row.value"
                >
                  <ElText truncated class="entry-value">{{ row.value }}</ElText>
                </ElTooltip>
              </template>
            </ElTableColumn>
            <ElTableColumn label="操作" width="90" align="center">
              <template #default="{ row }">
                <ElButton
                  type="danger"
                  link
                  size="small"
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
            class="detail-empty"
          />
        </template>

        <template v-else-if="!detailLoading">
          <ElEmpty description="未加载缓存详情" class="detail-empty" />
        </template>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <ElButton @click="detailDrawerVisible = false">关闭</ElButton>
          <ElButton
            type="primary"
            :icon="RefreshRight"
            @click="loadCacheDetail"
          >
            刷新
          </ElButton>
        </div>
      </template>
    </ElDrawer>
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue';
import {
  clearCache,
  evictCacheKey,
  getCacheDetail,
  getCacheStats,
} from '@/api/cacheManage';
import { Delete, RefreshRight, Search, View } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';

// 列表相关状态
const loading = ref(false);
const cacheStats = ref([]);
const filterKeyword = ref('');
const sortBy = ref('sizeDesc');
const lastUpdatedAt = ref(0);

// 详情抽屉
const detailDrawerVisible = ref(false);
const detailLoading = ref(false);
const currentCacheName = ref('');
const cacheDetail = ref(null);

// 汇总信息
const summary = computed(() => {
  if (!cacheStats.value.length) {
    return {
      totalCaches: 0,
      totalEntries: 0,
      avgHitRate: '0.00%',
    };
  }

  const totalCaches = cacheStats.value.length;
  const totalEntries = cacheStats.value.reduce(
    (sum, item) => sum + (item.size ?? 0),
    0,
  );
  const hitRates = cacheStats.value
    .map((item) => (typeof item.hitRate === 'number' ? item.hitRate : null))
    .filter((rate) => rate !== null);

  const avgHitRateValue = hitRates.length
    ? hitRates.reduce((sum, rate) => sum + rate, 0) / hitRates.length
    : 0;

  return {
    totalCaches,
    totalEntries,
    avgHitRate: `${avgHitRateValue.toFixed(2)}%`,
  };
});

const readableLastUpdated = computed(() => {
  if (!lastUpdatedAt.value) return '';
  const date = new Date(lastUpdatedAt.value);
  const format = (value) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${format(date.getMonth() + 1)}-${format(
    date.getDate(),
  )} ${format(date.getHours())}:${format(date.getMinutes())}:${format(
    date.getSeconds(),
  )}`;
});

// 过滤与排序
const filteredStats = computed(() => {
  const keyword = filterKeyword.value.trim().toLowerCase();
  let list = cacheStats.value.slice();

  if (keyword) {
    list = list.filter((item) =>
      item.cacheName?.toLowerCase().includes(keyword),
    );
  }

  switch (sortBy.value) {
    case 'hitRateDesc':
      list.sort((a, b) => (b.hitRate ?? 0) - (a.hitRate ?? 0));
      break;
    case 'nameAsc':
      list.sort((a, b) => (a.cacheName ?? '').localeCompare(b.cacheName ?? ''));
      break;
    case 'sizeDesc':
    default:
      list.sort((a, b) => (b.size ?? 0) - (a.size ?? 0));
      break;
  }

  return list;
});

const formatHitRate = (value) => {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return '--';
  }
  return `${Number(value).toFixed(2)}%`;
};

const getRateTone = (value) => {
  if (value === undefined || value === null) {
    return 'rate-neutral';
  }
  if (value >= 80) return 'rate-good';
  if (value >= 50) return 'rate-mid';
  return 'rate-bad';
};

// 加载缓存统计
const loadCacheStats = async () => {
  loading.value = true;
  try {
    const data = await getCacheStats();
    cacheStats.value = data || [];
    lastUpdatedAt.value = Date.now();
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
  detailDrawerVisible.value = true;
  await loadCacheDetail();
};

// 加载详情
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

    if (detailDrawerVisible.value && currentCacheName.value === cacheName) {
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

onMounted(() => {
  loadCacheStats();
});
</script>

<style scoped lang="scss">
.cache-manage-page {
  min-height: 100vh;
  background: #f5f6f7;
}

.page-body {
  max-width: 1200px;
  margin: 0 auto;
  padding: 96px 24px 48px;
}

.page-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.heading-meta {
  font-size: 0.875rem;
  color: #909399;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}

.summary-card {
  background: white;
  border: 1px solid #ebeef5;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.summary-label {
  color: #909399;
  font-size: 0.875rem;
}

.summary-value {
  font-size: 1.75rem;
  font-weight: 600;
  color: #303133;
}

.list-card {
  border-radius: 14px;
  border: 1px solid #ebeef5;
}

.list-toolbar {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-search {
  flex: 1;
  min-width: 240px;
}

.toolbar-sort {
  width: 160px;
}

.cache-table {
  font-size: 0.95rem;

  :deep(.el-table__header th) {
    background: #f9fafb;
  }
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cache-name {
  font-weight: 600;
  color: #303133;
}

.compact-stat {
  font-weight: 600;
  color: #303133;

  &.warning {
    color: #e6a23c;
  }
}

.compact-divider {
  margin: 0 4px;
  color: #c0c4cc;
}

.rate-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 600;
  background: #f4f4f5;
  color: #606266;

  &.rate-good {
    color: #1f8a4f;
    background: #e6f7ed;
  }

  &.rate-mid {
    color: #b25f00;
    background: #fff3e0;
  }

  &.rate-bad {
    color: #c4554d;
    background: #fdeceb;
  }
}

.list-empty {
  padding: 32px 0;
}

.detail-drawer {
  :deep(.el-drawer__header) {
    margin-bottom: 0;
    padding-bottom: 12px;
    border-bottom: 1px solid #f2f2f2;
  }

  :deep(.el-drawer__body) {
    padding: 0;
  }
}

.drawer-body {
  padding: 24px;
}

.detail-stat-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.detail-stat {
  background: #f9fafb;
  border-radius: 10px;
  padding: 12px 16px;

  .label {
    font-size: 0.8rem;
    color: #909399;
  }

  .value {
    display: block;
    margin-top: 6px;
    font-size: 1.25rem;
    font-weight: 600;
    color: #303133;
  }
}

.truncate-alert {
  margin-bottom: 12px;
}

.detail-table {
  border-radius: 10px;
  overflow: hidden;
}

.entry-key {
  font-family: 'JetBrains Mono', 'Courier New', monospace;
  font-size: 0.8rem;
  color: #409eff;
}

.entry-value {
  font-size: 0.85rem;
  color: #606266;
}

.detail-empty {
  margin-top: 24px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .page-body {
    padding: 84px 16px 32px;
  }

  .page-heading {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
