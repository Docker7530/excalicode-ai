import request from './request.js';
import { ENDPOINTS } from './endpoints.js';

/**
 * 需求知识库 API
 * 提供知识片段入库与检索能力
 */
export const upsertKnowledgeDocument = (payload) =>
  request.post(ENDPOINTS.REQUIREMENT_KNOWLEDGE.UPSERT, payload);

export const searchKnowledgeDocuments = (payload) =>
  request.post(ENDPOINTS.REQUIREMENT_KNOWLEDGE.SEARCH, payload);

export default {
  upsertKnowledgeDocument,
  searchKnowledgeDocuments,
};
