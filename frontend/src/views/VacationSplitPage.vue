<!--
  员工休假记录拆分页面
  上传Excel文件，解析并展示备注不为空的员工休假记录
-->

<template>
  <div class="vacation-split-page">
    <div class="page-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h1 class="page-title">员工休假记录拆分</h1>
        <p class="page-description">
          上传员工休假记录Excel文件，自动筛选出包含备注的记录
        </p>
      </div>

      <!-- 上传区域 -->
      <div class="upload-section">
        <ElCard shadow="hover">
          <template #header>
            <div class="card-header-content">
              <ElIcon :size="20">
                <Upload />
              </ElIcon>
              <span>上传Excel文件</span>
            </div>
          </template>

          <ElUpload
            ref="uploadRef"
            class="upload-area"
            drag
            :auto-upload="false"
            :limit="1"
            accept=".xlsx,.xls"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
          >
            <ElIcon class="upload-icon" :size="80">
              <UploadFilled />
            </ElIcon>
            <div class="upload-text">
              将Excel文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="upload-tip">
                支持.xlsx和.xls格式，文件大小不超过10MB<br />
                需包含：身份证号码、姓名、一级部门、备注 四个列
              </div>
            </template>
          </ElUpload>

          <div v-if="selectedFile" class="file-info">
            <ElIcon :size="16">
              <Document />
            </ElIcon>
            <span class="file-name">{{ selectedFile.name }}</span>
            <span class="file-size"
              >({{ formatFileSize(selectedFile.size) }})</span
            >
          </div>

          <div class="upload-actions">
            <ElButton
              type="primary"
              size="large"
              :loading="loading"
              :disabled="!selectedFile"
              @click="handleUpload"
            >
              <ElIcon v-if="!loading">
                <Upload />
              </ElIcon>
              {{ loading ? '解析中...' : '开始解析' }}
            </ElButton>
            <ElButton v-if="selectedFile" size="large" @click="handleClear">
              清除
            </ElButton>
          </div>
        </ElCard>
      </div>

      <!-- 结果展示 -->
      <div v-if="result" class="result-section">
        <ElCard shadow="hover">
          <template #header>
            <div class="card-header-content">
              <ElIcon :size="20">
                <List />
              </ElIcon>
              <span>解析结果</span>
              <ElTag type="success" class="result-tag">
                共{{ result.validCount }}条有效记录
              </ElTag>
            </div>
          </template>

          <!-- 统计信息 -->
          <div class="statistics">
            <div class="stat-item">
              <span class="stat-label">总记录数：</span>
              <span class="stat-value">{{ result.totalCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">有效记录数：</span>
              <span class="stat-value primary">{{ result.validCount }}</span>
            </div>
          </div>

          <!-- 数据表格 -->
          <div class="table-toolbox">
            <span class="toolbox-label">显示列：</span>
            <ElCheckboxGroup
              v-model="visibleResultColumns"
              class="column-checkbox-group"
            >
              <ElCheckbox label="index"> 序号 </ElCheckbox>
              <ElCheckbox label="idCard"> 身份证号码 </ElCheckbox>
              <ElCheckbox label="name"> 姓名 </ElCheckbox>
              <ElCheckbox label="department"> 一级部门 </ElCheckbox>
            </ElCheckboxGroup>
          </div>

          <ElTable
            :data="result.records"
            stripe
            border
            :height="400"
            class="result-table"
          >
            <ElTableColumn
              v-if="visibleResultColumns.includes('index')"
              type="index"
              label="序号"
              width="60"
              align="center"
            />
            <ElTableColumn
              v-if="visibleResultColumns.includes('idCard')"
              prop="idCard"
              label="身份证号码"
              width="180"
              align="center"
            />
            <ElTableColumn
              v-if="visibleResultColumns.includes('name')"
              prop="name"
              label="姓名"
              width="120"
              align="center"
            />
            <ElTableColumn
              v-if="visibleResultColumns.includes('department')"
              prop="department"
              label="一级部门"
              min-width="150"
            />
            <ElTableColumn prop="remark" label="原始备注" min-width="220">
              <template #default="scope">
                <div class="remark-text">
                  {{ scope.row.remark || '-' }}
                </div>
              </template>
            </ElTableColumn>
            <ElTableColumn
              v-if="showCorrectedColumn"
              prop="correctedRemark"
              label="修正后备注"
              min-width="220"
            >
              <template #default="scope">
                <ElInput
                  v-model="scope.row.correctedRemark"
                  type="textarea"
                  :autosize="{ minRows: 2, maxRows: 6 }"
                  class="corrected-input"
                  placeholder="请输入修正后的备注"
                />
              </template>
            </ElTableColumn>
          </ElTable>

          <!-- 操作按钮 -->
          <div class="table-actions">
            <ElButton
              v-if="!showCorrectedColumn"
              type="primary"
              :loading="correctingLoading"
              @click="handleCorrectRemarks"
            >
              <ElIcon v-if="!correctingLoading">
                <Edit />
              </ElIcon>
              {{ correctingLoading ? 'AI修正中...' : '备注修正' }}
            </ElButton>
            <ElButton
              v-if="showCorrectedColumn && !detailTable"
              type="warning"
              :loading="generatingTableLoading"
              @click="handleGenerateTable"
            >
              <ElIcon v-if="!generatingTableLoading">
                <List />
              </ElIcon>
              {{ generatingTableLoading ? '生成中...' : '生成休假数据表' }}
            </ElButton>
          </div>
        </ElCard>
      </div>

      <!-- 休假数据表 -->
      <div v-if="detailTable" class="detail-table-section">
        <ElCard shadow="hover">
          <template #header>
            <div class="card-header-content">
              <ElIcon :size="20">
                <List />
              </ElIcon>
              <span>休假数据表（最终）</span>
              <ElTag type="warning" class="result-tag">
                共{{ detailTable.length }}条记录
              </ElTag>
            </div>
          </template>

          <!-- 休假数据表格 -->
          <ElTable
            :data="detailTable"
            stripe
            border
            :height="500"
            class="result-table"
          >
            <ElTableColumn
              type="index"
              label="序号"
              width="60"
              align="center"
            />
            <ElTableColumn
              prop="idCard"
              label="身份证号码"
              width="180"
              align="center"
            />
            <ElTableColumn
              prop="name"
              label="姓名"
              width="100"
              align="center"
            />
            <ElTableColumn
              prop="startDate"
              label="开始日期"
              width="120"
              align="center"
            />
            <ElTableColumn
              prop="endDate"
              label="结束日期"
              width="120"
              align="center"
            />
            <ElTableColumn
              prop="startTime"
              label="开始时间"
              width="100"
              align="center"
            >
              <template #default>
                <span>-</span>
              </template>
            </ElTableColumn>
            <ElTableColumn
              prop="endTime"
              label="结束时间"
              width="100"
              align="center"
            >
              <template #default>
                <span>-</span>
              </template>
            </ElTableColumn>
            <ElTableColumn
              prop="vacationDays"
              label="休假天数"
              width="120"
              align="center"
            />
            <ElTableColumn
              prop="vacationType"
              label="休假类型"
              width="120"
              align="center"
            />
            <ElTableColumn
              prop="annualLeaveYear"
              label="年休假归属年份"
              width="140"
              align="center"
            >
              <template #default>
                <span>-</span>
              </template>
            </ElTableColumn>
            <ElTableColumn
              prop="childName"
              label="子女姓名"
              width="100"
              align="center"
            >
              <template #default>
                <span>-</span>
              </template>
            </ElTableColumn>
            <ElTableColumn
              prop="remark"
              label="备注"
              width="100"
              align="center"
            >
              <template #default>
                <span>-</span>
              </template>
            </ElTableColumn>
            <ElTableColumn prop="department" label="一级部门" min-width="150" />
          </ElTable>

          <!-- 导出按钮 -->
          <div class="table-actions">
            <ElButton type="primary" @click="handleExportDetailTable">
              <ElIcon>
                <Download />
              </ElIcon>
              导出休假数据表
            </ElButton>
          </div>
        </ElCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { api } from '@/api';
import {
  Document,
  Download,
  Edit,
  List,
  Upload,
  UploadFilled,
} from '@element-plus/icons-vue';
import { genFileId } from 'element-plus';
import * as XLSX from 'xlsx';

const uploadRef = ref(null);
const selectedFile = ref(null);
const loading = ref(false);
const result = ref(null);
const correctingLoading = ref(false);
const showCorrectedColumn = ref(false);
const generatingTableLoading = ref(false);
const detailTable = ref(null);

const DEFAULT_VISIBLE_RESULT_COLUMNS = [
  'index',
  'idCard',
  'name',
  'department',
];
const visibleResultColumns = ref([...DEFAULT_VISIBLE_RESULT_COLUMNS]);

/**
 * 文件选择处理
 */
const handleFileChange = (file) => {
  selectedFile.value = file.raw;
  result.value = null;
  showCorrectedColumn.value = false;
  detailTable.value = null;
  visibleResultColumns.value = [...DEFAULT_VISIBLE_RESULT_COLUMNS];
};

/**
 * 文件数量超出处理
 */
const handleExceed = (files) => {
  const upload = uploadRef.value;
  if (!upload) return;

  const file = files?.[0];
  if (!file) return;

  upload.clearFiles();
  file.uid = genFileId();
  upload.handleStart(file);
};

/**
 * 清除文件
 */
const handleClear = () => {
  uploadRef.value?.clearFiles();
  selectedFile.value = null;
  result.value = null;
  showCorrectedColumn.value = false;
  detailTable.value = null;
  visibleResultColumns.value = [...DEFAULT_VISIBLE_RESULT_COLUMNS];
};

/**
 * 格式化文件大小
 */
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
};

/**
 * 上传并解析文件
 */
const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件');
    return;
  }

  loading.value = true;

  try {
    const formData = new FormData();
    formData.append('file', selectedFile.value);

    const response = await api.post('/api/vacation/split', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    result.value = response;

    ElMessage.success({
      message: `解析成功！共${response.validCount}条有效记录`,
      duration: 3000,
    });
  } catch (error) {
    console.error('解析失败:', error);
    ElMessage.error(error.message || '解析失败，请检查文件格式是否正确');
    result.value = null;
  } finally {
    loading.value = false;
  }
};

/**
 * 修正备注
 */
const handleCorrectRemarks = async () => {
  if (!result.value || result.value.records.length === 0) {
    ElMessage.warning('没有可修正的数据');
    return;
  }

  correctingLoading.value = true;

  try {
    const response = await api.post('/api/vacation/correct', {
      records: result.value.records,
    });

    // 更新记录数据
    result.value.records = response;

    // 显示修正后的备注列
    showCorrectedColumn.value = true;

    ElMessage.success('备注修正完成');
  } catch (error) {
    console.error('备注修正失败:', error);
    ElMessage.error(error.message || '备注修正失败，请稍后重试');
  } finally {
    correctingLoading.value = false;
  }
};

/**
 * 生成休假数据表
 */
const handleGenerateTable = async () => {
  if (!result.value || result.value.records.length === 0) {
    ElMessage.warning('没有可生成的数据');
    return;
  }

  // 检查是否已完成备注修正
  const hasAllCorrected = result.value.records.every(
    (record) => record.correctedRemark,
  );
  if (!hasAllCorrected) {
    ElMessage.warning('请先完成备注修正');
    return;
  }

  generatingTableLoading.value = true;

  try {
    const response = await api.post('/api/vacation/generate-table', {
      records: result.value.records,
    });

    // 保存休假数据表
    detailTable.value = response;

    ElMessage.success(`休假数据表生成成功，共${response.length}条记录`);
  } catch (error) {
    console.error('休假数据表生成失败:', error);
    ElMessage.error(error.message || '休假数据表生成失败，请稍后重试');
  } finally {
    generatingTableLoading.value = false;
  }
};

/**
 * 导出休假数据表为Excel
 */
const handleExportDetailTable = () => {
  if (!detailTable.value || detailTable.value.length === 0) {
    ElMessage.warning('没有可导出的休假数据表');
    return;
  }

  try {
    // 准备导出数据
    const exportData = detailTable.value.map((record, index) => ({
      序号: index + 1,
      身份证号码: record.idCard,
      姓名: record.name,
      开始日期: record.startDate,
      结束日期: record.endDate,
      开始时间: '',
      结束时间: '',
      休假天数: record.vacationDays,
      休假类型: record.vacationType,
      年休假归属年份: '',
      子女姓名: '',
      备注: '',
      一级部门: record.department,
    }));

    // 创建工作簿和工作表
    const worksheet = XLSX.utils.json_to_sheet(exportData);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, '休假数据表');

    // 设置列宽
    worksheet['!cols'] = [
      { wch: 8 }, // 序号
      { wch: 20 }, // 身份证号码
      { wch: 12 }, // 姓名
      { wch: 15 }, // 开始日期
      { wch: 15 }, // 结束日期
      { wch: 12 }, // 开始时间
      { wch: 12 }, // 结束时间
      { wch: 12 }, // 休假天数
      { wch: 12 }, // 休假类型
      { wch: 15 }, // 年休假归属年份
      { wch: 12 }, // 子女姓名
      { wch: 20 }, // 备注
      { wch: 20 }, // 一级部门
    ];

    // 导出文件
    const timestamp = new Date().getTime();
    const filename = `休假数据表_${timestamp}.xlsx`;
    XLSX.writeFile(workbook, filename);

    ElMessage.success('导出成功');
  } catch (error) {
    console.error('导出失败:', error);
    ElMessage.error('导出失败，请稍后重试');
  }
};
</script>

<style lang="scss" scoped>
.vacation-split-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #eef2f7 100%);
  padding: 40px 0;
}

.page-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.page-header {
  text-align: center;
  margin-bottom: 48px;
}

.page-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 16px 0;
  background: linear-gradient(135deg, #409eff, #67c23a);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.page-description {
  font-size: 1.125rem;
  color: #64748b;
  margin: 0;
}

.upload-section {
  margin-bottom: 32px;
}

.card-header-content {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1.125rem;
  font-weight: 600;
}

.result-tag {
  margin-left: auto;
}

.upload-area {
  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    padding: 60px 20px;
    border: 2px dashed #d0d7de;
    border-radius: 12px;
    transition: all 0.3s ease;

    &:hover {
      border-color: #409eff;
      background-color: #f5f8ff;
    }
  }
}

.upload-icon {
  color: #409eff;
  margin-bottom: 16px;
}

.upload-text {
  color: #606266;
  font-size: 1rem;
  margin-bottom: 8px;

  em {
    color: #409eff;
    font-style: normal;
    font-weight: 600;
  }
}

.upload-tip {
  color: #909399;
  font-size: 0.875rem;
  line-height: 1.6;
  margin-top: 12px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-top: 16px;
  color: #606266;
}

.file-name {
  font-weight: 600;
}

.file-size {
  color: #909399;
  font-size: 0.875rem;
}

.upload-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
}

.result-section,
.detail-table-section {
  animation: slideUp 0.3s ease;
}

.detail-table-section {
  margin-top: 32px;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.statistics {
  display: flex;
  gap: 48px;
  padding: 24px;
  background: linear-gradient(135deg, #f8fafc, #eef2f7);
  border-radius: 12px;
  margin-bottom: 24px;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.stat-label {
  font-size: 0.975rem;
  color: #64748b;
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;

  &.primary {
    color: #409eff;
  }
}

.result-table {
  margin-bottom: 24px;

  :deep(.el-table__header) {
    th {
      background-color: #f5f7fa;
      font-weight: 600;
    }
  }
}

.table-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.corrected-text {
  color: #67c23a;
  font-weight: 500;
}
.table-toolbox {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbox-label {
  color: #666;
  font-size: 14px;
}

.column-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.remark-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.corrected-input :deep(.el-textarea__inner) {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
