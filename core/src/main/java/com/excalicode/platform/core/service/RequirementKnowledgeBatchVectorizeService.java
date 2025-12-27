package com.excalicode.platform.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.excalicode.platform.core.api.rag.RequirementKnowledgeBatchVectorizeResponse;
import com.excalicode.platform.core.api.rag.RequirementKnowledgeVectorizeError;
import com.excalicode.platform.core.entity.RequirementKnowledgeEntry;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeDocument;
import com.excalicode.platform.core.service.entity.RequirementKnowledgeEntryService;
import com.google.common.base.Splitter;
import com.google.common.util.concurrent.RateLimiter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 批量向量化服务：将数据库中“未向量化”的条目写入向量库 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementKnowledgeBatchVectorizeService {

  private static final int MAX_ERRORS = 20;
  private static final double UPSERTS_PER_MINUTE = 10.0d;

  private static final Splitter TAG_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private final RequirementKnowledgeService requirementKnowledgeService;
  private final RequirementKnowledgeEntryService requirementKnowledgeEntryService;

  /**
   * 上游 embedding 接口只有 10 RPM；这里对每次调用 upsertDocument 做限流。
   *
   * <p>阻塞式限流，批量向量化会按 10 RPM 的速度慢慢跑。
   */
  private final RateLimiter upsertLimiter = RateLimiter.create(UPSERTS_PER_MINUTE / 60.0d);

  public RequirementKnowledgeBatchVectorizeResponse vectorizeAllUnvectorized() {
    List<RequirementKnowledgeEntry> targets =
        requirementKnowledgeEntryService.list(buildUnvectorizedWrapper());
    int targetCount = targets != null ? targets.size() : 0;
    if (targetCount <= 0) {
      return RequirementKnowledgeBatchVectorizeResponse.builder()
          .targetCount(0)
          .successCount(0)
          .failedCount(0)
          .errors(List.of())
          .build();
    }

    int successCount = 0;
    int failedCount = 0;
    List<RequirementKnowledgeVectorizeError> errors = new ArrayList<>();

    log.info("批量向量化开始: targetCount={}, 速率限制={} RPM", targetCount, (int) UPSERTS_PER_MINUTE);

    for (RequirementKnowledgeEntry entry : targets) {
      if (entry == null || !StringUtils.hasText(entry.getDocumentId())) {
        continue;
      }
      try {
        vectorizeEntryWithRateLimit(entry);
        successCount++;
      } catch (BusinessException ex) {
        failedCount++;
        addError(errors, entry, ex.getMessage());
      } catch (Exception ex) {
        failedCount++;
        log.error("批量向量化失败: docId={}", entry.getDocumentId(), ex);
        addError(errors, entry, "系统异常: " + ex.getMessage());
      }
    }

    log.info(
        "批量向量化完成: targetCount={}, successCount={}, failedCount={}",
        targetCount,
        successCount,
        failedCount);

    return RequirementKnowledgeBatchVectorizeResponse.builder()
        .targetCount(targetCount)
        .successCount(successCount)
        .failedCount(failedCount)
        .errors(errors)
        .build();
  }

  private static LambdaQueryWrapper<RequirementKnowledgeEntry> buildUnvectorizedWrapper() {
    LambdaQueryWrapper<RequirementKnowledgeEntry> wrapper = new LambdaQueryWrapper<>();
    wrapper
        .and(
            inner ->
                inner
                    .isNull(RequirementKnowledgeEntry::getVectorized)
                    .or()
                    .eq(RequirementKnowledgeEntry::getVectorized, 0))
        .orderByAsc(RequirementKnowledgeEntry::getDocumentId);
    return wrapper;
  }

  private void vectorizeEntryWithRateLimit(RequirementKnowledgeEntry entry) {
    String documentId = entry.getDocumentId();
    RequirementKnowledgeDocument document =
        RequirementKnowledgeDocument.builder()
            .documentId(documentId)
            .title(entry.getTitle())
            .content(entry.getContent())
            .tags(splitTags(entry.getTags()))
            .build();

    upsertLimiter.acquire();
    requirementKnowledgeService.upsertDocument(document);

    boolean updated = requirementKnowledgeEntryService.updateVectorState(documentId, true);
    if (!updated) {
      requirementKnowledgeService.deleteDocumentVectors(documentId);
      throw new BusinessException("更新向量化状态失败: " + documentId);
    }
  }

  private void addError(
      List<RequirementKnowledgeVectorizeError> errors,
      RequirementKnowledgeEntry entry,
      String message) {
    if (errors.size() >= MAX_ERRORS) {
      return;
    }
    errors.add(
        RequirementKnowledgeVectorizeError.builder()
            .documentId(entry != null ? entry.getDocumentId() : null)
            .title(entry != null ? entry.getTitle() : null)
            .message(message)
            .build());
  }

  private static List<String> splitTags(String tags) {
    if (!StringUtils.hasText(tags)) {
      return List.of();
    }
    return new ArrayList<>(TAG_SPLITTER.splitToList(tags));
  }
}
