/**
 * 应用常量定义
 * 统一管理项目中的所有常量，确保一致性和可维护性
 */

// ==================== 工作流配置 ====================
export const WORKFLOW_CONFIG = {
  // 步骤配置
  STEPS: [
    {
      key: 'requirement',
      index: 1,
      label: '需求描述',
      description: '输入业务需求，开始分析流程',
    },
    {
      key: 'process-editor',
      index: 2,
      label: '功能过程',
      description: '编辑和管理AI拆解的功能过程',
    },
    {
      key: 'table-editor',
      index: 3,
      label: '子过程描述',
      description: '完善COSMIC子过程描述并确认结果',
    },
    {
      key: 'document-editor',
      index: 4,
      label: '需求文档',
      description: '生成和编辑最终需求文档',
    },
    {
      key: 'sequence-diagram',
      index: 5,
      label: '系统时序图',
      description: '渲染并复制软件时序图',
    },
  ],
};

// ==================== 消息常量 ====================
export const MESSAGES = {
  SUCCESS: {
    REQUIREMENT_ENHANCED: '需求扩写完成',
  },
  ERROR: {
    ENHANCE_FAILED: '需求扩写失败，请稍后重试',
  },
  WARNING: {
    EMPTY_REQUIREMENT: '请先输入需求描述',
    EXPECTED_PROCESS_COUNT_INVALID: '期望的功能过程数量需为正整数',
    MIN_PROCESSES: '至少需要保留一个过程',
    COSMIC_PROCESSES_REQUIRED: '请先确认功能过程表格内容',
    EMPTY_DOCUMENT: '请先完善需求文档内容再导出',
    EMPTY_REQUIREMENT_NAME: '请先填写需求名称',
  },
};

// ==================== 验证规则 ====================
export const VALIDATION = {
  // 输入长度限制
  LIMITS: {
    REQUIREMENT_MIN_LENGTH: 10,
    REQUIREMENT_MAX_LENGTH: 5000,
    REQUIREMENT_NAME_MIN_LENGTH: 2,
    REQUIREMENT_NAME_MAX_LENGTH: 100,
    PROCESS_DESC_MIN_LENGTH: 3,
    PROCESS_DESC_MAX_LENGTH: 500,
    DATA_GROUP_MAX_LENGTH: 100,
    DATA_ATTRIBUTES_MAX_LENGTH: 200,
  },
};

// ==================== 项目链接 ====================
export const PROJECT_LINKS = {
  GITHUB_REPO: 'https://github.com/docker7530/excalicode-ai',
};
