<!--

  需求分析页面

  管理需求分析、过程拆解与文档生成的多阶段流程

-->

<template>
  <div class="requirement-analysis-page">
    <AppHeader />

    <div class="page-container">
      <main class="main-content">
        <div class="content-wrapper">
          <div class="workflow-header">
            <ElSteps
              class="workflow-steps"
              :active="activeStepIndex"
              align-center
              finish-status="success"
              process-status="process"
            >
              <ElStep
                v-for="step in stepConfigs"
                :key="step.key"
                :status="getStepStatus(step)"
              >
                <template #title>
                  <div
                    class="step-title"
                    :class="{
                      active: step.key === currentStepKey,
                      completed:
                        step.isCompleted && step.key !== currentStepKey,
                      disabled: !step.isEnabled,
                    }"
                    role="button"
                    :tabindex="step.isEnabled ? 0 : -1"
                    :aria-disabled="!step.isEnabled"
                    :aria-current="
                      step.key === currentStepKey ? 'step' : undefined
                    "
                    @click="step.isEnabled && handleStepClick(step.key)"
                    @keydown.enter.prevent="
                      step.isEnabled && handleStepClick(step.key)
                    "
                    @keydown.space.prevent="
                      step.isEnabled && handleStepClick(step.key)
                    "
                  >
                    <span class="step-title-text">{{ step.label }}</span>
                  </div>
                </template>
              </ElStep>
            </ElSteps>
          </div>

          <AnalysisForm
            v-if="currentStep === 'requirement'"
            :loading="isBreakingDown || isEnhancing"
            :disabled="isBreakingDown || isEnhancing"
            :initial-requirement="originalRequirement"
            :initial-expected-process-count="expectedProcessCount"
            @submit="handleBreakdownSubmit"
            @enhance="handleEnhanceSubmit"
          />

          <LoadingAnimation
            v-if="currentStep === 'breakdown-loading'"
            :active="isBreakingDown"
            :progress="breakdownProgress"
            loading-text="AI 正在拆解功能过程..."
          />

          <ProcessEditor
            v-if="currentStep === 'process-editor'"
            :initial-processes="functionalProcesses"
            @confirm="handleProcessConfirm"
            @process-change="handleProcessChange"
          />

          <LoadingAnimation
            v-if="currentStep === 'analysis-loading'"
            :active="isAnalyzing"
            :progress="analyzeProgress"
            loading-text="AI 正在生成功能过程表格..."
          />

          <ProcessTableEditor
            v-if="currentStep === 'table-editor'"
            :initial-processes="cosmicProcesses"
            :latest-task-id="latestAnalysisTaskId"
            :export-loading="isExporting"
            :document-loading="isDocumentGenerating"
            :sequence-loading="isSequenceGenerating"
            @process-change="handleCosmicProcessChange"
            @export-table="handleExportTable"
            @generate-doc="handleGenerateDocument"
            @generate-sequence="handleGenerateSequenceDiagram"
          />

          <LoadingAnimation
            v-if="currentStep === 'document-loading'"
            :active="isDocumentGenerating"
            :progress="documentProgress"
            loading-text="正在生成需求文档..."
          />

          <DocumentEditor
            v-if="currentStep === 'document-editor'"
            :initial-content="documentPreview"
            :generating="isDocumentGenerating"
            :finalizing="isDocumentFinalizing"
            @regenerate="handleDocumentRegenerate"
            @finalize="handleFinalizeDocument"
            @content-change="handleDocumentContentChange"
          />

          <LoadingAnimation
            v-if="currentStep === 'sequence-loading'"
            :active="isSequenceGenerating"
            :progress="sequenceProgress"
            loading-text="AI 正在生成时序图..."
          />

          <SequenceDiagramViewer
            v-if="currentStep === 'sequence-viewer'"
            :diagram="sequenceDiagram"
          />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
/**
 * 需求分析全流程页面
 * 串联需求拆解、过程管理、文档生成等核心步骤
 */

import { cosmicService } from '@/api';
import AppHeader from '@/components/AppHeader.vue';
import { MESSAGES, WORKFLOW_CONFIG } from '@/constants';
import { validateRequirementName } from '@/utils';

// ==================== 页面状态：统一管理步骤与数据 ====================
const state = reactive({
  currentStep: 'requirement',
  isEnhancing: false,
  isBreakingDown: false,
  breakdownProgress: 0,
  functionalProcesses: [],
  isAnalyzing: false,
  analyzeProgress: 0,
  cosmicProcesses: [],
  latestAnalysisTaskId: null,
  isExporting: false,
  isDocumentGenerating: false,
  isDocumentFinalizing: false,
  documentProgress: 0,
  isSequenceGenerating: false,
  sequenceProgress: 0,
  sequenceDiagram: '',
  originalRequirement: '',
  expectedProcessCount: null,
  requirementName: '',
  documentPreview: '',
  latestDocumentMode: 'PREVIEW',
});

const breakdownTimer = ref();
const analyzeTimer = ref();
const documentTimer = ref();
const sequenceTimer = ref();
const enhancementAbortController = ref(null);

// ==================== 计算属性：派生 UI 需要的视图数据 ====================
const currentStep = computed(() => state.currentStep);
const isEnhancing = computed(() => state.isEnhancing);

const currentStepKey = computed(() => {
  if (state.currentStep === 'requirement') return 'requirement';
  if (['breakdown-loading', 'process-editor'].includes(state.currentStep))
    return 'process-editor';
  if (['analysis-loading', 'table-editor'].includes(state.currentStep))
    return 'table-editor';
  if (['document-loading', 'document-editor'].includes(state.currentStep))
    return 'document-editor';
  if (['sequence-loading', 'sequence-viewer'].includes(state.currentStep))
    return 'sequence-diagram';
  return 'requirement';
});

const isBreakingDown = computed(() => state.isBreakingDown);
const breakdownProgress = computed(() => state.breakdownProgress);
const functionalProcesses = computed(() => state.functionalProcesses);
const isAnalyzing = computed(() => state.isAnalyzing);
const analyzeProgress = computed(() => state.analyzeProgress);
const cosmicProcesses = computed(() => state.cosmicProcesses);
const isExporting = computed(() => state.isExporting);
const isDocumentGenerating = computed(() => state.isDocumentGenerating);
const isDocumentFinalizing = computed(() => state.isDocumentFinalizing);
const documentProgress = computed(() => state.documentProgress);
const originalRequirement = computed(() => state.originalRequirement);
const documentPreview = computed(() => state.documentPreview);
const isSequenceGenerating = computed(() => state.isSequenceGenerating);
const sequenceProgress = computed(() => state.sequenceProgress);
const sequenceDiagram = computed(() => state.sequenceDiagram);

const expectedProcessCount = computed(() => state.expectedProcessCount);
const latestAnalysisTaskId = computed(() => state.latestAnalysisTaskId);

// ==================== 数据转换工具 ====================
const serializeFunctionalProcesses = (items = []) =>
  Array.isArray(items)
    ? items.map((item) => ({
        description: (item.description || '').trim(),
      }))
    : [];

// 步骤面包屑配置：根据状态决定可达性与完成态
const stepConfigs = computed(() => {
  const hasFunctional = state.functionalProcesses.length > 0;
  const hasDocument = !!state.documentPreview;

  return WORKFLOW_CONFIG.STEPS.map((stepMeta) => {
    const base = {
      key: stepMeta.key,
      index: stepMeta.index,
      label: stepMeta.label,
      description: stepMeta.description,
    };

    switch (stepMeta.key) {
      case 'requirement':
        return {
          ...base,
          isEnabled: true,
          isCompleted: hasFunctional,
        };
      case 'process-editor':
        return {
          ...base,
          isEnabled: true, // 第二步默认放开，支持直接导入Excel
          isCompleted: hasFunctional,
        };
      case 'table-editor':
        return {
          ...base,
          isEnabled: true,
          isCompleted: hasDocument,
        };
      case 'document-editor':
        return {
          ...base,
          isEnabled:
            hasDocument ||
            ['document-loading', 'document-editor'].includes(state.currentStep),
          isCompleted: state.latestDocumentMode === 'FINALIZE',
        };
      case 'sequence-diagram':
        return {
          ...base,
          isEnabled:
            !!state.sequenceDiagram ||
            ['sequence-loading', 'sequence-viewer'].includes(state.currentStep),
          isCompleted: !!state.sequenceDiagram,
        };
      default:
        return {
          ...base,
          isEnabled: false,
          isCompleted: false,
        };
    }
  });
});

// 当前步骤序号，驱动步骤条高亮
const activeStepIndex = computed(() => {
  const index = stepConfigs.value.findIndex(
    (step) => step.key === currentStepKey.value,
  );
  return index >= 0 ? index : 0;
});

const getStepStatus = (step) => {
  if (step.key === currentStepKey.value) return 'process';
  if (step.isCompleted) return 'finish';
  return 'wait';
};

// ==================== 进度模拟：保持加载过程的细腻体验 ====================
const stopBreakdownProgressSimulation = () => {
  if (breakdownTimer.value) {
    clearInterval(breakdownTimer.value);
    breakdownTimer.value = undefined;
  }
};

const stopAnalyzeProgressSimulation = () => {
  if (analyzeTimer.value) {
    clearInterval(analyzeTimer.value);
    analyzeTimer.value = undefined;
  }
};

const stopDocumentProgressSimulation = () => {
  if (documentTimer.value) {
    clearInterval(documentTimer.value);
    documentTimer.value = undefined;
  }
};

const startBreakdownProgressSimulation = () => {
  stopBreakdownProgressSimulation();
  state.breakdownProgress = 0;
  let progress = 0;
  breakdownTimer.value = setInterval(() => {
    if (progress >= 85) return;
    progress = Math.min(progress + (Math.random() * 8 + 3), 85);
    state.breakdownProgress = Math.round(progress);
  }, 1000);
};

const startDocumentProgressSimulation = () => {
  stopDocumentProgressSimulation();
  state.documentProgress = 0;
  let progress = 0;
  documentTimer.value = setInterval(() => {
    if (progress >= 92) return;
    progress = Math.min(progress + (Math.random() * 6 + 4), 92);
    state.documentProgress = Math.round(progress);
  }, 850);
};

const stopSequenceProgressSimulation = () => {
  if (sequenceTimer.value) {
    clearInterval(sequenceTimer.value);
    sequenceTimer.value = undefined;
  }
};

const startSequenceProgressSimulation = () => {
  stopSequenceProgressSimulation();
  state.sequenceProgress = 0;
  let progress = 0;
  sequenceTimer.value = setInterval(() => {
    if (progress >= 90) return;
    progress = Math.min(progress + (Math.random() * 6 + 5), 90);
    state.sequenceProgress = Math.round(progress);
  }, 900);
};

// ==================== 流程切换与数据重置 ====================
const clearAfterRequirementChange = () => {
  state.functionalProcesses = [];
  state.cosmicProcesses = [];
  state.documentPreview = '';
  state.requirementName = '';
  state.isAnalyzing = false;
  state.analyzeProgress = 0;
  state.latestAnalysisTaskId = null;
  state.isDocumentGenerating = false;
  state.isDocumentFinalizing = false;
  state.documentProgress = 0;
  state.isExporting = false;
  state.isSequenceGenerating = false;
  state.sequenceProgress = 0;
  state.sequenceDiagram = '';
  stopSequenceProgressSimulation();
};

/**
 * 处理需求扩写美化
 */
const normalizeEnhanceOutput = (text = '') => {
  if (!text) return '';

  let normalized = text.replace(/\r\n/g, '\n');

  normalized = normalized.replace(
    /(\d+、)/g,
    (match, _group, offset, source) => {
      if (offset === 0) return match;
      if (source[offset - 1] === '\n') return match;
      return `\n${match}`;
    },
  );

  normalized = normalized.replace(/[ \t]+\n/g, '\n');
  normalized = normalized.replace(/\n{2,}/g, '\n');

  return normalized.trimStart();
};

const resolveExpectedProcessCount = (input) => {
  if (input === null || input === undefined) return null;
  const text = typeof input === 'number' ? String(input) : String(input).trim();
  if (!text) return null;

  const parsed = Number(text);
  if (Number.isInteger(parsed) && parsed > 0) {
    return parsed;
  }
  return undefined;
};

const handleEnhanceSubmit = async ({
  requirementDescription,
  expectedProcessCount: expectedCountInput,
}) => {
  if (state.isEnhancing) return;

  const previousRequirement =
    state.originalRequirement || requirementDescription;
  const resolvedCount = resolveExpectedProcessCount(expectedCountInput);
  if (resolvedCount !== undefined) {
    state.expectedProcessCount = resolvedCount;
  }

  state.isEnhancing = true;
  state.originalRequirement = '';

  const abortController = new AbortController();
  enhancementAbortController.value = abortController;

  const requestPayload = {
    requirementDescription,
  };

  if (typeof resolvedCount === 'number') {
    requestPayload.expectedProcessCount = resolvedCount;
  }

  try {
    const result = await cosmicService.enhanceRequirement(requestPayload, {
      signal: abortController.signal,
      onChunk: (_chunk, aggregated) => {
        state.originalRequirement = normalizeEnhanceOutput(aggregated);
      },
    });

    state.originalRequirement = normalizeEnhanceOutput(
      result.enhancedRequirement || requirementDescription,
    );

    ElMessage.success(MESSAGES.SUCCESS.REQUIREMENT_ENHANCED);
  } catch (error) {
    state.originalRequirement = previousRequirement;
    if (error?.message !== '流式请求已取消') {
      ElMessage.error(error.message || MESSAGES.ERROR.ENHANCE_FAILED);
    }
  } finally {
    enhancementAbortController.value = null;
    state.isEnhancing = false;
  }
};

/**
 * 接收需求输入后启动功能拆解流程
 */
const handleBreakdownSubmit = async ({
  requirementDescription,
  expectedProcessCount: expectedCountInput,
}) => {
  stopBreakdownProgressSimulation();
  stopAnalyzeProgressSimulation();
  stopDocumentProgressSimulation();

  const resolvedCount = resolveExpectedProcessCount(expectedCountInput);
  if (resolvedCount === undefined) {
    ElMessage.error(MESSAGES.WARNING.EXPECTED_PROCESS_COUNT_INVALID);
    return;
  }

  state.originalRequirement = requirementDescription;
  state.expectedProcessCount = resolvedCount;
  clearAfterRequirementChange();
  state.isBreakingDown = true;
  state.currentStep = 'breakdown-loading';
  startBreakdownProgressSimulation();

  try {
    const requestPayload = {
      requirementDescription,
    };

    if (typeof resolvedCount === 'number') {
      requestPayload.expectedProcessCount = resolvedCount;
    }

    const result = await cosmicService.breakdownRequirement(requestPayload);

    if (!result || result.success === false) {
      throw new Error(result?.errorMessage || '功能拆解失败');
    }

    state.functionalProcesses = Array.isArray(result?.functionalProcesses)
      ? result.functionalProcesses
      : [];

    state.breakdownProgress = 100;

    setTimeout(() => {
      state.isBreakingDown = false;
      state.currentStep = 'process-editor';
      stopBreakdownProgressSimulation();
    }, 600);

    ElMessage.success('功能拆解完成，请确认步骤');
  } catch (error) {
    stopBreakdownProgressSimulation();
    state.isBreakingDown = false;
    state.currentStep = 'requirement';
    ElMessage.error(error.message || '功能拆解失败，请稍后重试');
  }
};

const handleProcessChange = (processes) => {
  state.functionalProcesses = processes;
};

/**
 * 确认功能过程后，提交异步子过程生成任务
 */
const handleProcessConfirm = async (processes) => {
  if (state.isAnalyzing) return;

  state.functionalProcesses = processes;
  state.isAnalyzing = true;
  state.analyzeProgress = 0;
  stopAnalyzeProgressSimulation();

  try {
    const response = await cosmicService.submitAnalysisTask({
      functionalProcesses: serializeFunctionalProcesses(processes),
    });

    state.latestAnalysisTaskId = response?.taskId || null;
    state.currentStep = 'table-editor';

    ElMessage.success('子过程生成任务已提交，可在“我的任务”中查看进度并应用');
  } catch (error) {
    ElMessage.error(error.message || '子过程生成任务提交失败，请稍后重试');
  } finally {
    state.isAnalyzing = false;
  }
};

const handleCosmicProcessChange = (processes) => {
  state.cosmicProcesses = processes;
};

const buildProcessTablePayload = () => ({
  processes: state.cosmicProcesses,
});

const buildDocumentPreviewPayload = () => ({
  requirementName: state.requirementName,
  processes: state.cosmicProcesses,
});

const buildDocumentExportPayload = (overrideContent) => ({
  overrideDocumentContent: overrideContent,
});

const buildSequenceDiagramPayload = () => ({
  processes: state.cosmicProcesses,
});

const ensureRequirementName = async ({ promptIfEmpty = false } = {}) => {
  const trimmed = (state.requirementName || '').trim();
  if (trimmed) {
    state.requirementName = trimmed;
    return trimmed;
  }

  if (!promptIfEmpty) {
    ElMessage.warning(MESSAGES.WARNING.EMPTY_REQUIREMENT_NAME);
    return null;
  }

  try {
    const { value } = await ElMessageBox.prompt(
      '请输入用于生成需求文档的需求名称',
      '需求名称',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        closeOnClickModal: false,
        closeOnPressEscape: false,
        inputValue: state.requirementName,
        inputValidator: (val) => {
          const result = validateRequirementName(val);
          return result.valid ? true : result.message;
        },
      },
    );
    const formatted = value?.trim() || '';
    state.requirementName = formatted;
    return formatted || null;
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return null;
    }
    throw error;
  }
};

/**
 * 将 COSMIC 子过程表格导出为 Excel
 */
const handleExportTable = async () => {
  if (!state.cosmicProcesses.length) {
    ElMessage.warning('暂无可导出的功能过程数据');
    return;
  }

  state.isExporting = true;

  try {
    await cosmicService.exportProcessTable(buildProcessTablePayload());
    ElMessage.success('功能过程表格已导出');
  } catch (error) {
    ElMessage.error(error.message || '导出表格失败，请稍后重试');
  } finally {
    state.isExporting = false;
  }
};

/**
 * 基于当前子过程数据生成需求文档草稿
 */
const handleGenerateDocument = async () => {
  if (!state.cosmicProcesses.length) {
    ElMessage.warning('请先确认功能过程表格');
    return;
  }

  const requirementNameValue = await ensureRequirementName({
    promptIfEmpty: true,
  });
  if (!requirementNameValue) {
    return;
  }

  state.isDocumentGenerating = true;
  state.currentStep = 'document-loading';
  state.documentProgress = 0;
  startDocumentProgressSimulation();

  try {
    const content = await cosmicService.generateDocumentPreview(
      buildDocumentPreviewPayload(),
    );

    state.documentPreview = content;
    state.documentProgress = 100;
    state.latestDocumentMode = 'PREVIEW';

    setTimeout(() => {
      state.isDocumentGenerating = false;
      state.currentStep = 'document-editor';
      stopDocumentProgressSimulation();
    }, 600);

    ElMessage.success('需求文档已生成，可在此处微调');
  } catch (error) {
    stopDocumentProgressSimulation();
    state.isDocumentGenerating = false;
    state.currentStep = 'table-editor';
    ElMessage.error(error.message || '需求文档生成失败，请稍后重试');
  }
};

/** 基于功能过程生成时序图 */
const handleGenerateSequenceDiagram = async () => {
  if (!state.cosmicProcesses.length) {
    ElMessage.warning('请先确认功能过程表格');
    return;
  }

  stopSequenceProgressSimulation();
  state.isSequenceGenerating = true;
  state.currentStep = 'sequence-loading';
  state.sequenceProgress = 0;
  startSequenceProgressSimulation();

  try {
    const diagram =
      (
        await cosmicService.generateSequenceDiagram(
          buildSequenceDiagramPayload(),
        )
      )?.trim() || '';
    if (!diagram) {
      throw new Error('AI 未返回有效的时序图内容');
    }

    state.sequenceDiagram = diagram;
    state.sequenceProgress = 100;

    setTimeout(() => {
      state.isSequenceGenerating = false;
      state.currentStep = 'sequence-viewer';
      stopSequenceProgressSimulation();
    }, 600);

    ElMessage.success('时序图已生成，可复制使用');
  } catch (error) {
    stopSequenceProgressSimulation();
    state.isSequenceGenerating = false;
    state.currentStep = 'table-editor';
    ElMessage.error(error.message || '时序图生成失败，请稍后重试');
  }
};

/**
 * 在文档阶段重新触发 AI 再生成
 */
const handleDocumentRegenerate = async () => {
  if (state.isDocumentGenerating) return;
  const requirementNameValue = await ensureRequirementName();
  if (!requirementNameValue) return;
  await handleGenerateDocument();
};

const handleDocumentContentChange = (content) => {
  state.documentPreview = content;
};

/**
 * 导出需求文档终稿
 */
const handleFinalizeDocument = async (content) => {
  const trimmed = content?.trim();

  if (!trimmed) {
    ElMessage.warning('请先完善需求文档内容再导出');
    return;
  }

  state.isDocumentFinalizing = true;

  try {
    await cosmicService.exportRequirementDocument(
      buildDocumentExportPayload(content),
    );
    state.latestDocumentMode = 'FINALIZE';
    ElMessage.success('终稿已导出');
  } catch (error) {
    ElMessage.error(error.message || '导出终稿失败，请稍后重试');
  } finally {
    state.isDocumentFinalizing = false;
  }
};

/**
 * 回到第一步，方便重新录入需求
 */
const handleBackToRequirement = () => {
  stopBreakdownProgressSimulation();
  state.isBreakingDown = false;
  state.currentStep = 'requirement';
};

const handleBackToProcesses = () => {
  // 第二步默认放开，允许进入空的编辑器（用户可以导入Excel）
  stopAnalyzeProgressSimulation();
  state.isAnalyzing = false;
  state.currentStep = 'process-editor';
};

const handleBackToTable = ({ force = false } = {}) => {
  if (!force && !state.cosmicProcesses.length) {
    handleBackToProcesses();
    return;
  }

  stopDocumentProgressSimulation();
  stopSequenceProgressSimulation();
  state.isDocumentGenerating = false;
  state.isSequenceGenerating = false;
  state.currentStep = 'table-editor';
};

const scrollPageToTop = () => {
  if (typeof window === 'undefined') return;

  window.scrollTo({
    top: 0,
    left: 0,
    behavior: 'smooth',
  });
};

// 悬停状态管理
const handleStepClick = (target) => {
  if (
    target === currentStepKey.value ||
    !stepConfigs.value.find((s) => s.key === target)?.isEnabled
  ) {
    return;
  }
  handleStepNavigate(target);
  scrollPageToTop();
};

const handleStepNavigate = (target) => {
  if (target === currentStepKey.value) {
    return;
  }

  switch (target) {
    case 'requirement':
      handleBackToRequirement();
      break;

    case 'process-editor':
      // 第二步默认放开，允许直接访问
      handleBackToProcesses();
      break;

    case 'table-editor':
      handleBackToTable({ force: true });
      break;

    case 'document-editor':
      if (!state.documentPreview) return;
      state.currentStep = 'document-editor';
      break;
    case 'sequence-diagram':
      if (!state.sequenceDiagram) return;
      state.currentStep = 'sequence-viewer';
      break;

    default:
      break;
  }
};

const resetFlow = (options = {}) => {
  const { keepRequirement = false } = options;

  stopBreakdownProgressSimulation();
  stopAnalyzeProgressSimulation();
  stopDocumentProgressSimulation();
  stopSequenceProgressSimulation();

  state.currentStep = 'requirement';
  state.isBreakingDown = false;
  state.breakdownProgress = 0;
  state.functionalProcesses = [];
  state.isAnalyzing = false;
  state.analyzeProgress = 0;
  state.cosmicProcesses = [];
  state.isExporting = false;
  state.isDocumentGenerating = false;
  state.isDocumentFinalizing = false;
  state.documentProgress = 0;
  state.documentPreview = '';
  state.latestDocumentMode = 'PREVIEW';
  state.isSequenceGenerating = false;
  state.sequenceProgress = 0;
  state.sequenceDiagram = '';

  state.requirementName = '';
  if (!keepRequirement) {
    state.originalRequirement = '';
    state.expectedProcessCount = null;
  }
};

/**
 * 完整重置流程，回到初始状态
 */
const handleRestart = () => {
  resetFlow();
  ElMessage.info('已重置流程，请重新开始');
};

/**
 * 键盘快捷键：Ctrl/Cmd+R 重置，Esc 返回上一步
 */
const handleKeyboardShortcuts = (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'r') {
    event.preventDefault();
    handleRestart();
    return;
  }

  if (event.key === 'Escape') {
    if (currentStep.value === 'document-editor') {
      handleBackToTable();
      return;
    }

    if (currentStep.value === 'table-editor') {
      handleBackToProcesses();
      return;
    }

    if (currentStep.value === 'process-editor') {
      handleBackToRequirement();
    }
  }
};

onMounted(() => {
  document.title = '需求分析 - EXCALICODE AI';
  window.addEventListener('keydown', handleKeyboardShortcuts);
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyboardShortcuts);
  stopBreakdownProgressSimulation();
  stopAnalyzeProgressSimulation();
  stopDocumentProgressSimulation();
  stopSequenceProgressSimulation();
  enhancementAbortController.value?.abort();
});
</script>

<style lang="scss" scoped>
.requirement-analysis-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 50%, #f1f5f9 100%);
  display: flex;
  flex-direction: column;
}

.page-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: $spacing-xl 0 $spacing-4xl;

  @media (max-width: $breakpoint-md) {
    padding: $spacing-xl 0;
  }

  @media (max-width: $breakpoint-sm) {
    padding: $spacing-lg 0;
  }
}

.main-content {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 0 $spacing-lg;

  @media (max-width: $breakpoint-sm) {
    padding: 0 $spacing-md;
  }
}

.content-wrapper {
  width: 100%;
  max-width: $content-max-width;
  margin: 0 auto;
  animation: contentFadeIn 0.6s ease-out;
}

@keyframes contentFadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: $breakpoint-lg) {
  .content-wrapper {
    max-width: 100%;
  }
}

.workflow-header {
  position: sticky;
  top: 16px;
  z-index: 50;
}

.workflow-steps {
  padding: $spacing-md $spacing-lg;
  border-radius: $border-radius-lg;
  border: none;
  border-bottom: 2px solid rgba(148, 163, 184, 0.15);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(10px);
  margin-bottom: $spacing-xl;
  max-width: 100%;
  overflow: visible;
  position: relative;
  transition: all 0.3s ease;
  box-shadow:
    0 10px 28px rgba(15, 23, 42, 0.06),
    0 1px 0 rgba(148, 163, 184, 0.08);

  &::-webkit-scrollbar {
    height: 0;
  }

  @media (max-width: $breakpoint-md) {
    padding: $spacing-sm $spacing-md;
  }
}

@media (min-width: $breakpoint-md) {
  .workflow-steps {
    padding-right: calc($spacing-lg + 240px);
  }
}

@media (max-width: $breakpoint-sm) {
  .workflow-header {
    top: 8px;
  }
}

// 步骤容器
.workflow-steps :deep(.el-steps__item) {
  flex: 1 1 0;
  min-width: 120px;
  position: relative;
  padding: 0 2px;
}

// 隐藏节点图标
.workflow-steps :deep(.el-step__icon) {
  display: none;
}

// 隐藏连接线
.workflow-steps :deep(.el-step__line) {
  display: none;
}

// 标题样式 - 主要视觉元素
.workflow-steps :deep(.el-step__title) {
  font-weight: $font-weight-medium;
  color: rgba(100, 116, 139, 0.9);
  font-size: 15px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  padding: 0;
  margin: 0;
  position: relative;
  width: 100%;
  height: 100%;
}

// 等待状态
.workflow-steps :deep(.el-step__title.is-wait) {
  color: rgba(100, 116, 139, 0.65);
}

// 进行中状态 - 蓝色文字 + 底部指示器
.workflow-steps :deep(.el-step__title.is-process) {
  font-weight: $font-weight-bold;
  color: #2563eb;
  transform: scale(1.05);

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 70%;
    height: 3px;
    background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
    border-radius: 3px 3px 0 0;
    box-shadow: 0 -2px 12px rgba(37, 99, 235, 0.4);
    animation: slideIndicator 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  }
}

@keyframes slideIndicator {
  from {
    width: 0;
    opacity: 0;
  }
  to {
    width: 70%;
    opacity: 1;
  }
}

// 完成状态 - 绿色文字
.workflow-steps :deep(.el-step__title.is-finish) {
  color: #16a34a;
  font-weight: $font-weight-semibold;
}

// 步骤标题交互 - 扩大可点击区域
.step-title {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-height: 48px;
  padding: $spacing-md $spacing-lg;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  border-radius: $border-radius-md;

  &.disabled {
    cursor: not-allowed;
    opacity: 0.4;
  }

  &:not(.disabled) {
    cursor: pointer;

    &:hover {
      background: rgba(37, 99, 235, 0.06);
      transform: translateY(-2px);
    }

    &:active {
      transform: translateY(0);
      background: rgba(37, 99, 235, 0.08);
    }
  }
}

.step-title-text {
  font-weight: inherit;
  letter-spacing: 0.02em;
  transition: inherit;
  white-space: nowrap;
}
</style>
