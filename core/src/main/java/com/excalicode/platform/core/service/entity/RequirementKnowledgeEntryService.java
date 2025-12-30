package com.excalicode.platform.core.service.entity;

import com.baomidou.mybatisplus.extension.service.IService;
import com.excalicode.platform.core.entity.RequirementKnowledgeEntry;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeDocument;
import java.util.List;

/** 需求知识库条目 */
public interface RequirementKnowledgeEntryService extends IService<RequirementKnowledgeEntry> {

  /** 根据 documentId 获取条目 */
  RequirementKnowledgeEntry getByDocumentId(String documentId);

  /** 列出所有条目（后台管理使用，按更新时间倒序） */
  List<RequirementKnowledgeEntry> listForManage();

  /** 保存或更新知识条目草稿（不触发向量化，写入后会将向量状态置为未向量化） */
  RequirementKnowledgeEntry saveDraft(RequirementKnowledgeDocument document);

  /** 更新可编辑字段（标题/正文/标签），不允许修改 documentId */
  RequirementKnowledgeEntry updateDraft(
      String documentId, String title, String content, List<String> tags);

  /** 更新向量化状态 */
  boolean updateVectorState(String documentId, boolean vectorized);

  /** 删除条目（逻辑删除） */
  boolean removeByDocumentId(String documentId);
}
