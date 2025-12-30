package com.excalicode.platform.core.service.entity.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.excalicode.platform.core.entity.RequirementKnowledgeEntry;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.mapper.RequirementKnowledgeEntryMapper;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeDocument;
import com.excalicode.platform.core.service.entity.RequirementKnowledgeEntryService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/** 需求知识库条目 Service 实现（数据库存储层） */
@Slf4j
@Service
public class RequirementKnowledgeEntryServiceImpl
    extends ServiceImpl<RequirementKnowledgeEntryMapper, RequirementKnowledgeEntry>
    implements RequirementKnowledgeEntryService {

  @Override
  public RequirementKnowledgeEntry getByDocumentId(String documentId) {
    if (!StringUtils.hasText(documentId)) {
      return null;
    }
    return this.getById(documentId.trim());
  }

  @Override
  public List<RequirementKnowledgeEntry> listForManage() {
    return this.list(
        new LambdaQueryWrapper<RequirementKnowledgeEntry>()
            .orderByDesc(RequirementKnowledgeEntry::getUpdatedTime));
  }

  @Override
  public RequirementKnowledgeEntry saveDraft(RequirementKnowledgeDocument document) {
    RequirementKnowledgeDocument normalized =
        Objects.requireNonNull(document, "document 不能为空").normalized();
    if (!StringUtils.hasText(normalized.getDocumentId())) {
      throw new BusinessException("documentId 不能为空");
    }
    if (!StringUtils.hasText(normalized.getTitle())) {
      throw new BusinessException("标题不能为空");
    }
    if (!StringUtils.hasText(normalized.getContent())) {
      throw new BusinessException("知识内容不能为空");
    }

    RequirementKnowledgeEntry entry = new RequirementKnowledgeEntry();
    entry.setDocumentId(normalized.getDocumentId());
    entry.setTitle(normalized.getTitle());
    entry.setContent(normalized.getContent());
    entry.setTags(encodeTags(normalized.getSafeTags()));

    // 任何保存都只代表“草稿已更新”，向量必须由用户显式触发。
    entry.setVectorized(0);
    entry.setVectorUpdatedTime(null);

    boolean success = this.saveOrUpdate(entry);
    if (!success) {
      throw new BusinessException("保存知识条目失败");
    }
    log.info("保存知识条目草稿完成: docId={}", entry.getDocumentId());
    return this.getByDocumentId(entry.getDocumentId());
  }

  @Override
  public RequirementKnowledgeEntry updateDraft(
      String documentId, String title, String content, List<String> tags) {
    if (!StringUtils.hasText(documentId)) {
      throw new BusinessException("documentId 不能为空");
    }
    RequirementKnowledgeEntry existing = this.getByDocumentId(documentId);
    if (existing == null) {
      throw new BusinessException("知识条目不存在: " + documentId);
    }
    if (!StringUtils.hasText(title)) {
      throw new BusinessException("标题不能为空");
    }
    if (!StringUtils.hasText(content)) {
      throw new BusinessException("知识内容不能为空");
    }

    existing.setTitle(title);
    existing.setContent(content);
    existing.setTags(encodeTags(tags));
    existing.setVectorized(0);
    existing.setVectorUpdatedTime(null);

    boolean success = this.updateById(existing);
    if (!success) {
      throw new BusinessException("更新知识条目失败: " + documentId);
    }
    log.info("更新知识条目草稿完成: docId={}", documentId);
    return this.getByDocumentId(documentId);
  }

  @Override
  public boolean updateVectorState(String documentId, boolean vectorized) {
    RequirementKnowledgeEntry existing = this.getByDocumentId(documentId);
    if (existing == null) {
      return false;
    }
    existing.setVectorized(vectorized ? 1 : 0);
    existing.setVectorUpdatedTime(vectorized ? LocalDateTime.now() : null);
    return this.updateById(existing);
  }

  @Override
  public boolean removeByDocumentId(String documentId) {
    if (!StringUtils.hasText(documentId)) {
      return false;
    }
    return this.removeById(documentId.trim());
  }

  private static String encodeTags(List<String> tags) {
    if (CollectionUtils.isEmpty(tags)) {
      return null;
    }
    List<String> normalized = new ArrayList<>();
    for (String tag : tags) {
      if (StringUtils.hasText(tag)) {
        normalized.add(tag.trim());
      }
    }
    return normalized.isEmpty() ? null : String.join(",", normalized);
  }
}
