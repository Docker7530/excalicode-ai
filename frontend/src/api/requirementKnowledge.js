import request from './request.js';
import { ENDPOINTS } from './endpoints.js';

/**
 * 需求知识库 API
 * - 入库：仅保存到数据库
 * - 向量化：由用户在列表中显式触发
 */
export const upsertKnowledgeDocument = (payload) =>
  request.post(ENDPOINTS.REQUIREMENT_KNOWLEDGE.UPSERT, payload);

export const listKnowledgeEntries = () =>
  request.get(ENDPOINTS.REQUIREMENT_KNOWLEDGE.ENTRIES);

export const updateKnowledgeEntry = (documentId, payload) =>
  request.put(
    ENDPOINTS.REQUIREMENT_KNOWLEDGE.ENTRY_DETAIL(documentId),
    payload,
  );

export const vectorizeKnowledgeEntry = (documentId) =>
  request.post(ENDPOINTS.REQUIREMENT_KNOWLEDGE.VECTORIZE(documentId));

export const deleteKnowledgeVector = (documentId) =>
  request.delete(ENDPOINTS.REQUIREMENT_KNOWLEDGE.DELETE_VECTOR(documentId));

export const deleteKnowledgeEntry = (documentId) =>
  request.delete(ENDPOINTS.REQUIREMENT_KNOWLEDGE.DELETE_ENTRY(documentId));

export const searchKnowledgeDocuments = (payload) =>
  request.post(ENDPOINTS.REQUIREMENT_KNOWLEDGE.SEARCH, payload);

export default {
  upsertKnowledgeDocument,
  listKnowledgeEntries,
  updateKnowledgeEntry,
  vectorizeKnowledgeEntry,
  deleteKnowledgeVector,
  deleteKnowledgeEntry,
  searchKnowledgeDocuments,
};
