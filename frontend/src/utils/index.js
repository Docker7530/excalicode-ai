/**
 * 工具函数库
 * 统一管理项目中实际使用的工具函数，避免冗余逻辑
 */

import { MESSAGES, VALIDATION } from '@/constants';

// ==================== 调度工具 ====================
/**
 * 防抖函数
 * @param {Function} func 要防抖的函数
 * @param {number} wait 等待时间(ms)
 * @param {boolean} immediate 是否在触发时立即执行
 * @returns {Function} 防抖后的函数
 */
export function debounce(func, wait, immediate = false) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      timeout = null;
      if (!immediate) func.apply(this, args);
    };
    const callNow = immediate && !timeout;
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
    if (callNow) func.apply(this, args);
  };
}

// ==================== 校验工具 ====================
/**
 * 校验需求描述文本
 * @param {string} text 需求描述
 * @returns {{valid: boolean, message: string}} 校验结果
 */
export function validateRequirement(text) {
  const trimmed = text?.trim() || '';
  const { REQUIREMENT_MIN_LENGTH, REQUIREMENT_MAX_LENGTH } = VALIDATION.LIMITS;

  if (trimmed.length < REQUIREMENT_MIN_LENGTH) {
    return {
      valid: false,
      message: `需求描述至少需要${REQUIREMENT_MIN_LENGTH}个字符`,
    };
  }

  if (trimmed.length > REQUIREMENT_MAX_LENGTH) {
    return {
      valid: false,
      message: `需求描述不能超过${REQUIREMENT_MAX_LENGTH}个字符`,
    };
  }

  return { valid: true, message: '' };
}

/**
 * 校验需求名称
 * @param {string} text 需求名称
 * @returns {{valid: boolean, message: string}} 校验结果
 */
export function validateRequirementName(text) {
  const trimmed = text?.trim() || '';
  const { REQUIREMENT_NAME_MIN_LENGTH, REQUIREMENT_NAME_MAX_LENGTH } =
    VALIDATION.LIMITS;

  if (!trimmed) {
    return {
      valid: false,
      message: '需求名称不能为空',
    };
  }

  if (trimmed.length < REQUIREMENT_NAME_MIN_LENGTH) {
    return {
      valid: false,
      message: `需求名称至少需要${REQUIREMENT_NAME_MIN_LENGTH}个字符`,
    };
  }

  if (trimmed.length > REQUIREMENT_NAME_MAX_LENGTH) {
    return {
      valid: false,
      message: `需求名称不能超过${REQUIREMENT_NAME_MAX_LENGTH}个字符`,
    };
  }

  return { valid: true, message: '' };
}

/**
 * 校验期望的功能过程数量
 * @param {string|number} value 期望数量
 * @returns {{valid: boolean, message: string, value: number|null}} 校验结果
 */
export function validateExpectedProcessCount(value) {
  const normalized =
    typeof value === 'number' ? String(value) : String(value ?? '').trim();

  if (!normalized) {
    return {
      valid: true,
      message: '',
      value: null,
    };
  }

  const parsed = Number(normalized);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    return {
      valid: false,
      message: MESSAGES.WARNING.EXPECTED_PROCESS_COUNT_INVALID,
      value: null,
    };
  }

  return { valid: true, message: '', value: parsed };
}
