<template>
  <div class="system-setting-manage-page">
    <AppHeader />

    <section class="page-body">
      <header class="page-heading">
        <div class="heading-left">
          <h2 class="page-title">系统设置</h2>
          <ElText type="info" size="small">仅维护配置 key / value</ElText>
        </div>
        <div class="heading-actions">
          <ElButton
            plain
            :icon="RefreshRight"
            :loading="loading"
            @click="loadSettings"
          >
            刷新
          </ElButton>
          <ElButton type="primary" :icon="Plus" @click="openCreateDialog">
            新增
          </ElButton>
        </div>
      </header>

      <ElAlert
        type="info"
        :closable="false"
        class="quick-hint"
        show-icon
        title="首页『使用技巧』内容可用 key=home.usageTips 配置"
      />

      <ElCard shadow="never" class="list-card">
        <div class="list-toolbar">
          <ElInput
            v-model="keyword"
            placeholder="搜索配置 key"
            clearable
            size="small"
            :prefix-icon="Search"
            class="toolbar-search"
          />
        </div>

        <ElTable
          v-loading="loading"
          :data="filteredSettings"
          stripe
          border
          class="settings-table"
        >
          <ElTableColumn prop="configKey" label="配置 key" min-width="240" />
          <ElTableColumn label="配置 value" min-width="460">
            <template #default="{ row }">
              <div class="value-cell">
                {{ row.configValue || '' }}
              </div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <ElButton
                type="primary"
                link
                size="small"
                :icon="Edit"
                @click="openEditDialog(row)"
              >
                编辑
              </ElButton>
              <ElButton
                type="danger"
                link
                size="small"
                :icon="Delete"
                @click="handleDelete(row)"
              >
                删除
              </ElButton>
            </template>
          </ElTableColumn>
        </ElTable>

        <ElEmpty
          v-if="!loading && filteredSettings.length === 0"
          description="暂无系统设置"
          class="list-empty"
        />
      </ElCard>
    </section>

    <ElDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="760px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <ElForm :model="formData" label-width="90px">
        <ElFormItem label="配置 key" required>
          <ElInput
            v-model="formData.configKey"
            placeholder="例如：home.usageTips"
            :disabled="isEditing"
          />
        </ElFormItem>
        <ElFormItem label="配置 value">
          <ElInput
            v-model="formData.configValue"
            type="textarea"
            :rows="12"
            :autosize="{ minRows: 10, maxRows: 18 }"
            placeholder="支持长文本，可写 Markdown"
          />
        </ElFormItem>
      </ElForm>

      <template #footer>
        <ElButton :disabled="saving" @click="dialogVisible = false">
          取消
        </ElButton>
        <ElButton type="primary" :loading="saving" @click="handleSave">
          保存
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue';
import {
  deleteAdminSetting,
  listAdminSettings,
  upsertAdminSetting,
} from '@/api/sysSetting.js';
import {
  Delete,
  Edit,
  Plus,
  RefreshRight,
  Search,
} from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';

const loading = ref(false);
const saving = ref(false);
const keyword = ref('');
const settings = ref([]);

const dialogVisible = ref(false);
const isEditing = ref(false);
const formData = reactive({
  configKey: '',
  configValue: '',
});

const dialogTitle = computed(() =>
  isEditing.value ? '编辑系统设置' : '新增系统设置',
);

const filteredSettings = computed(() => {
  const list = settings.value || [];
  const k = (keyword.value || '').trim();
  if (!k) return list;
  return list.filter((item) => (item?.configKey || '').includes(k));
});

const loadSettings = async () => {
  loading.value = true;
  try {
    const data = await listAdminSettings();
    settings.value = data || [];
  } catch (error) {
    console.error('加载系统设置失败', error);
    ElMessage.error(error.message || '加载系统设置失败');
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  formData.configKey = '';
  formData.configValue = '';
};

const openCreateDialog = () => {
  isEditing.value = false;
  resetForm();
  dialogVisible.value = true;
};

const openEditDialog = (row) => {
  isEditing.value = true;
  formData.configKey = row?.configKey || '';
  formData.configValue = row?.configValue || '';
  dialogVisible.value = true;
};

const handleSave = async () => {
  const key = (formData.configKey || '').trim();
  if (!key) {
    ElMessage.warning('配置 key 不能为空');
    return;
  }

  saving.value = true;
  try {
    await upsertAdminSetting(key, formData.configValue ?? '');
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    await loadSettings();
  } catch (error) {
    console.error('保存系统设置失败', error);
    ElMessage.error(error.message || '保存失败');
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (row) => {
  const key = row?.configKey;
  if (!key) return;

  try {
    await ElMessageBox.confirm(`确定删除配置【${key}】吗？`, '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }

  try {
    await deleteAdminSetting(key);
    ElMessage.success('已删除');
    await loadSettings();
  } catch (error) {
    console.error('删除系统设置失败', error);
    ElMessage.error(error.message || '删除失败');
  }
};

onMounted(() => {
  loadSettings();
});
</script>

<style scoped lang="scss">
.system-setting-manage-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 50%, #f1f5f9 100%);
}

.page-body {
  max-width: 1200px;
  margin: 0 auto;
  padding: 110px 24px 64px;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.heading-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.page-title {
  margin: 0;
  font-size: 1.6rem;
  color: #0f172a;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.heading-actions {
  display: flex;
  gap: 10px;
}

.quick-hint {
  margin-bottom: 14px;
}

.list-card {
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.24);
}

.list-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.toolbar-search {
  width: 260px;
}

.value-cell {
  white-space: pre-wrap;
  word-break: break-word;
  color: #334155;
  line-height: 1.6;
  max-height: 120px;
  overflow: auto;
}

.list-empty {
  padding: 24px 0;
}
</style>
