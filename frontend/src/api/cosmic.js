/**
 * COSMIC 相关 API 封装
 * 使用统一的常量和工具函数，提供类型安全的API调用
 */
import { MESSAGES } from '@/constants';
import {
  validateExpectedProcessCount,
  validateRequirement,
  validateRequirementName,
} from '@/utils';
import { API_BASE_URL, ENDPOINTS } from './endpoints.js';
import api from './request.js';

const ERROR_FALLBACK = '服务调用失败，请稍后再试';

/**
 * COSMIC 业务服务封装
 * 负责对各类需求分析相关 API 的统一校验与错误兜底
 */
class CosmicService {
  /**
   * 需求扩写美化
   * @param {object} payload 包含原始需求描述的请求体
   */
  async enhanceRequirement(payload, options = {}) {
    this.validateRequirementPayload(payload);

    const expectedProcessValidation = validateExpectedProcessCount(
      payload?.expectedProcessCount,
    );
    if (!expectedProcessValidation.valid) {
      throw new Error(
        expectedProcessValidation.message ||
          MESSAGES.WARNING.EXPECTED_PROCESS_COUNT_INVALID,
      );
    }

    const requestBody = {
      originalRequirement: payload.requirementDescription,
    };
    if (typeof expectedProcessValidation.value === 'number') {
      requestBody.expectedProcessCount = expectedProcessValidation.value;
    }

    const { onChunk, signal } = options ?? {};
    const controller = new AbortController();

    if (signal) {
      if (signal.aborted) {
        controller.abort();
      } else {
        signal.addEventListener('abort', () => controller.abort(), {
          once: true,
        });
      }
    }

    let response;
    const endpoint = `${API_BASE_URL}${ENDPOINTS.REQUIREMENT.ENHANCE}`;
    const headers = {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
    };

    const token = localStorage.getItem('token');
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }

    try {
      response = await fetch(endpoint, {
        method: 'POST',
        headers,
        body: JSON.stringify(requestBody),
        signal: controller.signal,
      });
    } catch (error) {
      if (error?.name === 'AbortError') {
        throw new Error('流式请求已取消');
      }
      throw new Error(error?.message || MESSAGES.ERROR.ENHANCE_FAILED);
    }

    if (!response.ok) {
      const message = await this.parseStreamError(response);
      throw new Error(message);
    }

    if (!response.body) {
      throw new Error('当前浏览器不支持流式响应，请更换或升级浏览器');
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let fullText = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value, { stream: true });
      const lines = chunk.split('\n');

      for (const rawLine of lines) {
        if (!rawLine) continue;

        if (rawLine.startsWith('data:')) {
          let data = rawLine.slice(5);
          if (data.startsWith(' ')) {
            data = data.slice(1);
          }
          if (!data || data === '[DONE]') {
            continue;
          }
          fullText += data;
          if (typeof onChunk === 'function') {
            onChunk(data, fullText);
          }
        } else if (rawLine.trim()) {
          fullText += rawLine;
          if (typeof onChunk === 'function') {
            onChunk(rawLine, fullText);
          }
        }
      }
    }

    fullText += decoder.decode();

    return {
      success: true,
      enhancedRequirement: fullText.trim() || payload.requirementDescription,
    };
  }

  /**
   * 触发需求拆解，输出功能过程列表
   * @param {object} payload 后端所需的拆解请求体
   */
  async breakdownRequirement(payload) {
    const expectedProcessCount = this.validateBreakdownPayload(payload);
    const requestBody = {
      requirementDescription: payload.requirementDescription,
    };

    if (typeof expectedProcessCount === 'number') {
      requestBody.expectedProcessCount = expectedProcessCount;
    }

    try {
      return await api.post(ENDPOINTS.REQUIREMENT.BREAKDOWN, requestBody);
    } catch (error) {
      throw this.handleApiError(error, '功能拆解');
    }
  }

  /**
   * 基于人工确认的过程信息生成功能过程表格 (V1 稳定版本)
   * @param {object} payload 拆解后的功能过程数据
   */
  async analyzeRequirement(payload) {
    this.validateAnalysisPayload(payload);
    try {
      return await api.post(ENDPOINTS.REQUIREMENT.ANALYZE, payload);
    } catch (error) {
      throw this.handleApiError(error, '功能过程表格生成');
    }
  }

  /**
   * 提交子过程生成任务（异步）
   * @param {object} payload 拆解后的功能过程数据
   */
  async submitAnalysisTask(payload) {
    this.validateAnalysisPayload(payload);
    try {
      return await api.post(ENDPOINTS.REQUIREMENT.ANALYZE_TASK, payload);
    } catch (error) {
      throw this.handleApiError(error, '子过程任务提交');
    }
  }

  /**
   * 查询当前用户的子过程生成任务列表
   */
  async listAnalysisTasks() {
    try {
      return await api.get(ENDPOINTS.REQUIREMENT.ANALYZE_TASKS);
    } catch (error) {
      throw this.handleApiError(error, '任务列表获取');
    }
  }

  /**
   * 查询子过程生成任务详情
   * @param {number|string} taskId 任务ID
   */
  async getAnalysisTaskDetail(taskId) {
    if (!taskId) {
      throw new Error('任务ID缺失');
    }
    try {
      return await api.get(ENDPOINTS.REQUIREMENT.ANALYZE_TASK_DETAIL(taskId));
    } catch (error) {
      throw this.handleApiError(error, '任务详情获取');
    }
  }

  /**
   * 导出功能过程表格 Excel
   * @param {object} payload 功能过程表格结构
   */
  async exportProcessTable(payload) {
    this.validateProcessTablePayload(payload);
    try {
      const response = await api.post(
        ENDPOINTS.REQUIREMENT.EXPORT_TABLE,
        payload,
        {
          responseType: 'blob',
        },
      );
      const filename =
        this.extractFilenameFromResponse(response) || '功能过程表格.xlsx';
      this.downloadBlob(response.data, filename);
      return { success: true };
    } catch (error) {
      throw this.handleApiError(error, '表格导出');
    }
  }

  /**
   * 导入 Excel 功能过程
   * @param {File} file 选中的 Excel 文件
   */
  async importFunctionalProcesses(file) {
    if (!(file instanceof File)) {
      throw new Error('请提供待导入的功能过程Excel文件');
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
      return await api.post(ENDPOINTS.REQUIREMENT.IMPORT_PROCESSES, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
    } catch (error) {
      throw this.handleApiError(error, '功能过程导入');
    }
  }

  /**
   * 导入 COSMIC 子过程表格
   * @param {File} file Excel 文件
   */
  async importCosmicProcesses(file) {
    if (!(file instanceof File)) {
      throw new Error('请提供待导入的子过程Excel文件');
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
      return await api.post(
        ENDPOINTS.REQUIREMENT.IMPORT_COSMIC_PROCESSES,
        formData,
        {
          headers: { 'Content-Type': 'multipart/form-data' },
        },
      );
    } catch (error) {
      throw this.handleApiError(error, '子过程导入');
    }
  }

  /**
   * 生成需求文档预览内容
   * @param {object} payload 功能过程与元数据
   */
  async generateDocumentPreview(payload) {
    this.validateDocumentPreviewPayload(payload);
    try {
      return await api.post(ENDPOINTS.REQUIREMENT.GENERATE_DOCUMENT, payload);
    } catch (error) {
      throw this.handleApiError(error, '需求文档预览');
    }
  }

  /**
   * 基于功能过程生成 Mermaid 时序图
   * @param {object} payload 功能过程表结构
   */
  async generateSequenceDiagram(payload) {
    this.validateProcessTablePayload(payload);
    try {
      return await api.post(ENDPOINTS.REQUIREMENT.SEQUENCE_DIAGRAM, payload);
    } catch (error) {
      throw this.handleApiError(error, '时序图生成');
    }
  }

  /**
   * 导出最终需求文档
   * @param {object} payload 导出参数，包含可选的文档覆盖内容
   */
  async exportRequirementDocument(payload) {
    const sanitized = this.validateDocumentExportPayload(payload);
    payload.overrideDocumentContent = sanitized.content;
    try {
      const response = await api.post(
        ENDPOINTS.REQUIREMENT.EXPORT_DOCUMENT,
        payload,
        {
          responseType: 'blob',
        },
      );
      const filename =
        this.extractFilenameFromResponse(response) || '需求文档.docx';
      this.downloadBlob(response.data, filename);
      return { success: true };
    } catch (error) {
      throw this.handleApiError(error, '需求文档导出');
    }
  }

  // ==================== 下载与导出辅助 ====================

  /**
   * 从响应头推断下载文件名
   */
  extractFilenameFromResponse(response) {
    try {
      const disposition = response.headers?.['content-disposition'];
      if (!disposition) return null;

      // 从 Content-Disposition 头中提取文件名
      const filenameMatch = disposition.match(/filename\*=UTF-8''(.+)/);
      if (filenameMatch && filenameMatch[1]) {
        return decodeURIComponent(filenameMatch[1]);
      }

      const fallbackMatch = disposition.match(/filename="?(.+?)"?$/);
      if (fallbackMatch && fallbackMatch[1]) {
        return decodeURIComponent(fallbackMatch[1]);
      }

      return null;
    } catch (error) {
      console.warn('提取文件名失败:', error);
      return null;
    }
  }

  /**
   * 触发浏览器下载二进制文件
   */
  downloadBlob(blob, filename) {
    try {
      if (!(blob instanceof Blob)) {
        throw new Error('没有检测到可下载的文件流');
      }

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      link.rel = 'noopener noreferrer';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      throw new Error(`无法触发下载: ${error.message}`);
    }
  }

  async parseStreamError(response) {
    const fallback = MESSAGES.ERROR.ENHANCE_FAILED || ERROR_FALLBACK;

    try {
      const text = await response.text();
      if (!text) {
        return `${fallback}（${response.status}）`;
      }

      try {
        const payload = JSON.parse(text);
        if (payload && typeof payload === 'object') {
          return (
            payload.detail ||
            payload.message ||
            payload.title ||
            `${fallback}（${response.status}）`
          );
        }
      } catch {
        const trimmed = text.trim();
        if (trimmed) {
          return trimmed;
        }
      }

      return `${fallback}（${response.status}）`;
    } catch {
      return fallback;
    }
  }

  // ==================== 请求数据校验 ====================

  /**
   * 验证基础请求参数
   * @param {Object} request 请求对象
   */
  validateRequirementPayload(request) {
    const description = request?.requirementDescription;
    if (!description) {
      throw new Error(MESSAGES.WARNING.EMPTY_REQUIREMENT);
    }

    const result = validateRequirement(description);
    if (!result.valid) {
      throw new Error(result.message);
    }
  }

  validateBreakdownPayload(request) {
    this.validateRequirementPayload(request);

    const validation = validateExpectedProcessCount(
      request?.expectedProcessCount,
    );

    if (!validation.valid) {
      throw new Error(
        validation.message || MESSAGES.WARNING.EXPECTED_PROCESS_COUNT_INVALID,
      );
    }

    return validation.value ?? null;
  }

  validateAnalysisPayload(request) {
    const processes = request?.functionalProcesses;
    if (!Array.isArray(processes) || processes.length === 0) {
      throw new Error(MESSAGES.WARNING.MIN_PROCESSES);
    }
  }

  validateProcessTablePayload(request) {
    if (!Array.isArray(request?.processes) || request.processes.length === 0) {
      throw new Error(MESSAGES.WARNING.COSMIC_PROCESSES_REQUIRED);
    }
  }

  validateDocumentPreviewPayload(request) {
    this.validateProcessTablePayload(request);
    request.requirementName = this.ensureRequirementName(
      request?.requirementName,
    );
  }

  validateDocumentExportPayload(request) {
    const content = request?.overrideDocumentContent?.trim();
    if (!content) {
      throw new Error(MESSAGES.WARNING.EMPTY_DOCUMENT);
    }
    return { content };
  }

  /**
   * 确保需求名称存在且符合校验规则
   */
  ensureRequirementName(name) {
    const trimmed = (name ?? '').trim();
    const result = validateRequirementName(trimmed);
    if (!result.valid) {
      throw new Error(
        result.message || MESSAGES.WARNING.EMPTY_REQUIREMENT_NAME,
      );
    }
    return trimmed;
  }

  /**
   * 将后端 ProblemDetail 转换为友好的错误文案
   */
  handleApiError(error, scene) {
    const problem = error?.problemDetail;
    if (problem) {
      const detail = problem.detail || problem.title;
      if (detail) {
        return new Error(`${scene}失败：${detail}`);
      }
      return new Error(`${scene}失败`);
    }

    const response = error?.response;
    if (!response) {
      return new Error(error?.message || ERROR_FALLBACK);
    }

    const { status } = response;
    const payload = response.data;

    if (payload instanceof Blob) {
      return new Error(`${scene}失败，服务返回了文件流错误信息`);
    }

    if (payload && typeof payload === 'object') {
      const detail = payload.detail || payload.message || payload.title;
      if (detail) {
        return new Error(`${scene}失败：${detail}`);
      }
    }

    if (typeof payload === 'string' && payload.trim()) {
      return new Error(`${scene}失败：${payload.trim()}`);
    }

    const statusMap = {
      400: `${scene}请求参数异常`,
      401: '请重新登录后再试',
      403: '暂无权限执行该操作',
      404: '未找到相关资源',
      429: '请求过于频繁，请稍后再试',
      500: `${scene}失败，服务器开小差了`,
    };

    return new Error(
      statusMap[status] || `${scene}失败（${status}）` || ERROR_FALLBACK,
    );
  }
}

const cosmicService = new CosmicService();
export default cosmicService;
export { CosmicService };
