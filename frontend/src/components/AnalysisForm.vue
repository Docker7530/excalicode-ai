<!--
  需求分析表单组件
  提供用户输入需求描述和配置分析参数的界面
  采用现代化的表单设计，注重用户体验和无障碍访问
-->
<template>
  <div class="analysis-form">
    <!-- 主要表单区域 -->
    <form class="form-content" novalidate @submit.prevent="handleSubmit">
      <!-- 需求描述输入区域 -->
      <div class="form-group">
        <label for="requirementInput" class="form-label">
          需求描述
          <span class="required-mark" aria-label="必填">*</span>
        </label>

        <div class="input-wrapper">
          <textarea
            id="requirementInput"
            v-model="formData.requirementDescription"
            class="form-textarea"
            :class="{ error: errors.requirementDescription }"
            placeholder="1、支持TOP企业客户的生命周期管理，包括客户信息的查询、导出、列表、详情查看、修改与删除。
2、支持客户与内部资源（如账户、运营人员）的绑定管理，明确客户归属和服务责任人。"
            rows="10"
            :maxlength="maxLength"
            :disabled="disabled"
            @input="handleInput"
            @blur="validateField('requirementDescription')"
          ></textarea>

          <!-- 字数统计 -->
          <div class="char-counter">
            <span
              class="char-count"
              :class="{ warning: charCountWarning, error: charCountError }"
            >
              {{ formData.requirementDescription.length }}
            </span>
            <span class="char-limit">/{{ maxLength }}</span>
          </div>
        </div>

        <div
          v-if="errors.requirementDescription"
          class="error-message error-message--field"
        >
          {{ errors.requirementDescription }}
        </div>

        <!-- 统一控制台 -->
        <div class="unified-control-bar">
          <div class="control-bar-content">
            <!-- 期望数量输入 -->
            <div class="control-item control-item--input">
              <label for="expectedProcessInput" class="control-label">
                期望数量
              </label>
              <input
                id="expectedProcessInput"
                :value="formData.expectedProcessCount"
                class="control-number-input"
                type="number"
                inputmode="numeric"
                pattern="\d*"
                min="1"
                step="1"
                placeholder="可选"
                :disabled="disabled"
                :class="{ error: errors.expectedProcessCount }"
                @input="handleExpectedProcessInput"
                @blur="validateField('expectedProcessCount')"
              />
            </div>

            <!-- 开始分析按钮（中间位置） -->
            <button
              type="submit"
              class="control-button control-button--primary"
              :class="{
                loading: loading,
                disabled: !canSubmit,
              }"
              :disabled="!canSubmit || loading"
            >
              <svg
                v-if="!loading"
                class="button-icon"
                viewBox="0 0 24 24"
                width="20"
                height="20"
              >
                <path
                  fill="currentColor"
                  d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.94-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"
                />
              </svg>
              <div v-else class="loading-spinner"></div>
              <span class="button-text">{{
                loading ? '分析中...' : '开始分析'
              }}</span>
            </button>

            <!-- 扩写美化按钮 -->
            <button
              type="button"
              class="control-button control-button--secondary"
              :class="{ disabled: !canSubmit || loading }"
              :disabled="!canSubmit || loading"
              @click="handleEnhance"
            >
              <svg
                class="button-icon"
                viewBox="0 0 24 24"
                width="18"
                height="18"
              >
                <path
                  fill="currentColor"
                  d="M14.06 9.02l.92.92L5.92 19H5v-.92l9.06-9.06M17.66 3c-.25 0-.51.1-.7.29l-1.83 1.83 3.75 3.75 1.83-1.83c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.2-.2-.45-.29-.71-.29zm-3.6 3.19L3 17.25V21h3.75L17.81 9.94l-3.75-3.75z"
                />
              </svg>
              <span class="button-text">扩写美化</span>
            </button>
          </div>

          <!-- 错误提示 -->
          <div v-if="errors.expectedProcessCount" class="control-bar-error">
            {{ errors.expectedProcessCount }}
          </div>
        </div>
      </div>
    </form>

    <!-- 表单底部提示 -->
    <div class="form-footer">
      <div class="security-tip">
        <svg class="tip-icon" viewBox="0 0 24 24" width="16" height="16">
          <path
            fill="currentColor"
            d="M12,1L3,5V11C3,16.55 6.84,21.74 12,23C17.16,21.74 21,16.55 21,11V5L12,1M12,7C13.4,7 14.8,8.6 14.8,10V11.5C15.4,11.5 16,12.1 16,12.7V16.2C16,16.8 15.4,17.3 14.8,17.3H9.2C8.6,17.3 8,16.8 8,16.2V12.6C8,12.1 8.6,11.5 9.2,11.5V10C9.2,8.6 10.6,7 12,7M12,8.2C11.2,8.2 10.5,8.7 10.5,10V11.5H13.5V10C13.5,8.7 12.8,8.2 12,8.2Z"
          />
        </svg>
        <span>您的数据将被安全处理，我们不会存储敏感信息</span>
      </div>
    </div>
  </div>
</template>

<script>
import { VALIDATION } from '@/constants';
import {
  debounce,
  validateExpectedProcessCount,
  validateRequirement,
} from '@/utils';

/**
 * 需求分析表单组件
 * 负责收集用户的需求输入并进行基础验证
 */
export default {
  name: 'AnalysisForm',

  props: {
    /**
     * 是否处于加载状态
     */
    loading: {
      type: Boolean,
      default: false,
    },

    /**
     * 是否禁用表单
     */
    disabled: {
      type: Boolean,
      default: false,
    },

    /**
     * 初始需求描述
     */
    initialRequirement: {
      type: String,
      default: '',
    },
    /**
     * 初始期望功能过程数量
     */
    initialExpectedProcessCount: {
      type: [Number, String],
      default: '',
    },
  },

  emits: ['submit', 'enhance'],

  data() {
    return {
      // 表单数据
      formData: {
        requirementDescription: '',
        expectedProcessCount: '',
      },

      // 验证错误
      errors: {},

      // 常量引用
      VALIDATION_LIMITS: VALIDATION.LIMITS,
    };
  },

  computed: {
    /**
     * 是否可以提交表单
     * @returns {boolean}
     */
    canSubmit() {
      const { REQUIREMENT_MIN_LENGTH } = this.VALIDATION_LIMITS;
      const descriptionValid =
        this.formData.requirementDescription.trim().length >=
        REQUIREMENT_MIN_LENGTH;
      const processCountValid = validateExpectedProcessCount(
        this.formData.expectedProcessCount,
      ).valid;

      return (
        descriptionValid &&
        processCountValid &&
        Object.keys(this.errors).length === 0
      );
    },

    /**
     * 字数统计是否接近警告线
     * @returns {boolean}
     */
    charCountWarning() {
      const { REQUIREMENT_MAX_LENGTH } = this.VALIDATION_LIMITS;
      return (
        this.formData.requirementDescription.length >
        REQUIREMENT_MAX_LENGTH * 0.8
      );
    },

    /**
     * 字数统计是否超出限制
     * @returns {boolean}
     */
    charCountError() {
      const { REQUIREMENT_MAX_LENGTH } = this.VALIDATION_LIMITS;
      return (
        this.formData.requirementDescription.length >= REQUIREMENT_MAX_LENGTH
      );
    },

    /**
     * 最大字符数
     * @returns {number}
     */
    maxLength() {
      return this.VALIDATION_LIMITS.REQUIREMENT_MAX_LENGTH;
    },
  },

  watch: {
    initialRequirement: {
      immediate: true,
      handler(value) {
        const nextValue = value || '';
        if (this.formData.requirementDescription !== nextValue) {
          this.formData.requirementDescription = nextValue;
        }
      },
    },
    initialExpectedProcessCount: {
      immediate: true,
      handler(value) {
        const sanitized = this.sanitizeExpectedProcessCountInput(value);
        if (this.formData.expectedProcessCount !== sanitized) {
          this.formData.expectedProcessCount = sanitized;
        }
      },
    },
  },

  created() {
    // 创建防抖验证函数
    this.debouncedValidate = debounce(this.validateField, 300);
  },

  methods: {
    /**
     * 处理扩写美化
     */
    handleEnhance() {
      // 全面验证表单
      this.validateForm();

      // 如果有错误，不提交
      if (Object.keys(this.errors).length > 0) {
        console.warn('表单验证失败:', this.errors);
        return;
      }

      // 触发扩写事件
      this.$emit('enhance', this.buildSubmitPayload());
    },

    /**
     * 处理表单提交
     */
    handleSubmit() {
      // 全面验证表单
      this.validateForm();

      // 如果有错误，不提交
      if (Object.keys(this.errors).length > 0) {
        console.warn('表单验证失败:', this.errors);
        return;
      }

      // 触发提交事件
      this.$emit('submit', this.buildSubmitPayload());
    },

    /**
     * 验证整个表单
     */
    validateForm() {
      this.errors = {};
      this.validateField('requirementDescription');
      this.validateField('expectedProcessCount');
    },

    /**
     * 验证单个字段
     * @param {string} fieldName 字段名
     */
    validateField(fieldName) {
      switch (fieldName) {
        case 'requirementDescription':
          this.validateRequirementDescription();
          break;
        case 'expectedProcessCount':
          this.validateExpectedProcessCount();
          break;
      }
    },

    /**
     * 验证需求描述
     */
    validateRequirementDescription() {
      const description = this.formData.requirementDescription;
      const result = validateRequirement(description);

      if (!result.valid) {
        this.errors.requirementDescription = result.message;
        return;
      }

      // 清除错误
      this.clearError('requirementDescription');
    },

    /**
     * 验证期望功能过程数量
     */
    validateExpectedProcessCount() {
      const result = validateExpectedProcessCount(
        this.formData.expectedProcessCount,
      );

      if (!result.valid) {
        this.errors.expectedProcessCount =
          result.message || '期望的功能过程数量需为正整数';
        return;
      }

      this.clearError('expectedProcessCount');
    },

    /**
     * 处理输入事件
     */
    handleInput() {
      this.clearError('requirementDescription');
      this.debouncedValidate('requirementDescription');
    },

    /**
     * 期望数量输入事件
     */
    handleExpectedProcessInput(event) {
      const rawValue = event?.target?.value ?? '';
      this.formData.expectedProcessCount =
        rawValue === '' ? '' : this.sanitizeExpectedProcessCountInput(rawValue);

      this.clearError('expectedProcessCount');
      this.debouncedValidate('expectedProcessCount');
    },

    /**
     * 组装提交载荷
     */
    buildSubmitPayload() {
      const parsedCount = Number(this.formData.expectedProcessCount);
      return {
        requirementDescription: this.formData.requirementDescription,
        expectedProcessCount:
          Number.isInteger(parsedCount) && parsedCount > 0 ? parsedCount : null,
      };
    },

    /**
     * 规范化期望数量输入
     * @param {string|number} value 原始值
     * @returns {string} 规范化后的字符串
     */
    sanitizeExpectedProcessCountInput(value) {
      if (value === null || value === undefined) return '';
      let digits = String(value).replace(/[^\d]/g, '');
      if (!digits) return '';

      // 限制长度避免异常大数
      digits = digits.slice(0, 4);

      const numeric = Number(digits);
      if (!Number.isFinite(numeric)) {
        return '';
      }

      return String(numeric);
    },

    /**
     * 清除指定字段的错误
     * @param {string} fieldName 字段名
     */
    clearError(fieldName) {
      if (this.errors[fieldName]) {
        delete this.errors[fieldName];
      }
    },

    /**
     * 重置表单
     */
    resetForm() {
      this.formData = {
        requirementDescription: '',
        expectedProcessCount: '',
      };
      this.errors = {};
    },

    /**
     * 设置表单数据（用于外部设置）
     * @param {Object} data 表单数据
     */
    setFormData(data) {
      this.formData = { ...this.formData, ...data };
    },
  },
};
</script>

<style lang="scss" scoped>
/**
 * 分析表单样式
 * 采用卡片式设计，重点突出内容层次和交互反馈
 */
.analysis-form {
  background: $background-primary;
  border-radius: $border-radius-2xl;
  box-shadow: $shadow-lg;
  border: 1px solid $border-light;
  overflow: hidden;
  transition: all $transition-base;

  &:hover {
    box-shadow: $shadow-xl;
  }
}

// 表单内容区域
.form-content {
  padding: $spacing-3xl $spacing-2xl;

  @media (max-width: $breakpoint-md) {
    padding: $spacing-2xl $spacing-xl;
  }

  @media (max-width: $breakpoint-sm) {
    padding: $spacing-xl $spacing-lg;
  }
}

.form-group {
  margin-bottom: $spacing-2xl;

  &:last-child {
    margin-bottom: 0;
  }

  &--compact {
    margin-bottom: $spacing-lg;
  }

  &--button {
    margin-bottom: 0;
    display: flex;
    justify-content: center;
    align-items: center;
  }
}

// 标签样式
.form-label {
  display: block;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  color: $primary-color;
  margin-bottom: $spacing-sm;
  line-height: 1.3;
}

.required-mark {
  color: $error-color;
  margin-left: $spacing-xs;
}

// 输入框包装器
.input-wrapper {
  position: relative;
}

// ========== 统一控制台样式 ==========
.unified-control-bar {
  margin-top: $spacing-2xl;
}

.control-bar-content {
  display: flex;
  align-items: center;
  gap: $spacing-md;
  padding: $spacing-md;
  background: linear-gradient(
    135deg,
    rgba(255, 255, 255, 0.95) 0%,
    rgba(248, 250, 252, 0.98) 100%
  );
  backdrop-filter: blur(12px);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: $border-radius-2xl;
  box-shadow:
    0 4px 16px rgba(0, 0, 0, 0.06),
    0 2px 8px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all $transition-base;

  &:hover {
    box-shadow:
      0 8px 24px rgba(0, 0, 0, 0.08),
      0 4px 12px rgba(0, 0, 0, 0.05),
      inset 0 1px 0 rgba(255, 255, 255, 0.9);
    border-color: rgba(148, 163, 184, 0.3);
  }

  @media (max-width: $breakpoint-md) {
    flex-direction: column;
    align-items: stretch;
    gap: $spacing-sm;
  }
}

// 控制项（期望数量输入）
.control-item {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  padding: $spacing-xs $spacing-md;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.15);
  border-radius: $border-radius-xl;
  transition: all $transition-fast;

  &:focus-within {
    background: rgba(255, 255, 255, 0.95);
    border-color: rgba($accent-color, 0.4);
    box-shadow: 0 0 0 3px rgba($accent-color, 0.1);
  }

  @media (max-width: $breakpoint-md) {
    justify-content: space-between;
  }
}

.control-label {
  font-size: $font-size-sm;
  font-weight: $font-weight-medium;
  color: rgba($primary-color, 0.75);
  white-space: nowrap;
  margin: 0;
}

.control-number-input {
  width: 80px;
  padding: $spacing-xs $spacing-sm;
  border: none;
  background: transparent;
  font-size: $font-size-base;
  font-weight: $font-weight-medium;
  font-family: inherit;
  text-align: center;
  color: $primary-color;
  transition: all $transition-fast;

  &::placeholder {
    color: $tertiary-color;
    font-weight: $font-weight-normal;
  }

  &:focus {
    outline: none;
  }

  &.error {
    color: $error-color;
  }

  &:disabled {
    color: $tertiary-color;
    cursor: not-allowed;
  }

  @media (max-width: $breakpoint-md) {
    width: 100px;
  }
}

// 控制按钮通用样式
.control-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: $spacing-xs;
  padding: $spacing-sm $spacing-lg;
  border: none;
  border-radius: $border-radius-xl;
  font-size: $font-size-base;
  font-weight: $font-weight-semibold;
  font-family: inherit;
  cursor: pointer;
  transition: all $transition-base;
  white-space: nowrap;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(
      90deg,
      transparent,
      rgba(255, 255, 255, 0.3),
      transparent
    );
    transition: left 0.5s ease;
  }

  &:hover:not(.disabled):not(:disabled) {
    transform: translateY(-1px);

    &::before {
      left: 100%;
    }
  }

  &:active:not(.disabled):not(:disabled) {
    transform: translateY(0);
  }

  &.disabled,
  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }

  .button-icon {
    transition: transform $transition-fast;
  }

  @media (max-width: $breakpoint-md) {
    width: 100%;
  }
}

// 次要按钮（扩写美化）
.control-button--secondary {
  background: linear-gradient(
    135deg,
    rgba(139, 92, 246, 0.1) 0%,
    rgba(167, 139, 250, 0.08) 100%
  );
  color: #7c3aed;
  border: 1px solid rgba(139, 92, 246, 0.25);
  box-shadow: 0 2px 8px rgba(139, 92, 246, 0.1);

  &:hover:not(.disabled):not(:disabled) {
    background: linear-gradient(
      135deg,
      rgba(139, 92, 246, 0.15) 0%,
      rgba(167, 139, 250, 0.12) 100%
    );
    border-color: rgba(139, 92, 246, 0.35);
    box-shadow: 0 4px 12px rgba(139, 92, 246, 0.15);
  }
}

// 主要按钮（开始分析）
.control-button--primary {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border: 1px solid rgba(37, 99, 235, 0.3);
  box-shadow:
    0 4px 14px rgba(37, 99, 235, 0.25),
    0 2px 6px rgba(37, 99, 235, 0.15);
  flex: 1;
  min-width: 140px;

  &:hover:not(.disabled):not(:disabled) {
    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
    box-shadow:
      0 6px 20px rgba(37, 99, 235, 0.35),
      0 3px 10px rgba(37, 99, 235, 0.2);
  }

  &.loading {
    background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  }
}

.button-text {
  line-height: 1;
}

// 控制栏错误提示
.control-bar-error {
  margin-top: $spacing-sm;
  padding: $spacing-sm $spacing-md;
  background: rgba($error-color, 0.05);
  border-left: 3px solid $error-color;
  border-radius: $border-radius-md;
  font-size: $font-size-sm;
  color: $error-color;
  display: flex;
  align-items: center;
  gap: $spacing-xs;

  &::before {
    content: '⚠️';
    font-size: $font-size-xs;
  }
}

.error-message--field {
  margin-top: $spacing-sm;
}

// 文本域样式
.form-textarea {
  width: 100%;
  padding: $spacing-md;
  border: 2px solid $border-medium;
  border-radius: $border-radius-lg;
  font-size: $font-size-base;
  font-family: inherit;
  color: $primary-color;
  background: $background-primary;
  resize: vertical;

  // 默认给足高度，减少“写两行就得滚动/拖拽”的尴尬
  min-height: 220px;
  height: clamp(220px, 36vh, 520px);

  transition: all $transition-fast;
  line-height: $line-height-relaxed;

  &::placeholder {
    color: $tertiary-color;
  }

  &:focus {
    outline: none;
    border-color: $accent-color;
    box-shadow: 0 0 0 4px rgba($accent-color, 0.1);
  }

  &:disabled {
    background: $background-secondary;
    color: $tertiary-color;
    cursor: not-allowed;
  }

  &.error {
    border-color: $error-color;

    &:focus {
      box-shadow: 0 0 0 4px rgba($error-color, 0.1);
    }
  }
}

// 字数统计
.char-counter {
  position: absolute;
  bottom: $spacing-sm;
  right: $spacing-sm;
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: $font-size-xs;
  background: rgba($background-primary, 0.9);
  padding: $spacing-xs $spacing-sm;
  border-radius: $border-radius-md;
  backdrop-filter: blur(4px);
}

.char-count {
  color: $secondary-color;
  font-weight: $font-weight-medium;
  transition: color $transition-fast;

  &.warning {
    color: $warning-color;
  }

  &.error {
    color: $error-color;
  }
}

.char-limit {
  color: $tertiary-color;
}

// 加载动画
.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(white, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

// 错误提示
.error-message {
  margin-top: $spacing-sm;
  font-size: $font-size-sm;
  color: $error-color;
  display: flex;
  align-items: center;
  gap: $spacing-xs;

  &::before {
    content: '⚠️';
    font-size: $font-size-xs;
  }
}

// 表单底部
.form-footer {
  padding: $spacing-lg $spacing-2xl;
  background: $background-tertiary;
  border-top: 1px solid $border-light;

  @media (max-width: $breakpoint-md) {
    padding: $spacing-md $spacing-xl;
  }
}

.security-tip {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  justify-content: center;
  font-size: $font-size-sm;
  color: $tertiary-color;
}

.tip-icon {
  color: $accent-color;
  flex-shrink: 0;
}
</style>
