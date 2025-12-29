package com.excalicode.platform.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.excalicode.platform.core.entity.RequirementKnowledgeEntry;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeDocument;
import com.excalicode.platform.core.service.entity.RequirementKnowledgeEntryService;
import com.google.common.base.Splitter;
import com.google.common.util.concurrent.RateLimiter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 批量向量化服务：将数据库中“未向量化”的条目写入向量库 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementKnowledgeBatchVectorizeService {

  private static final double UPSERTS_PER_MINUTE = 10.0d;

  private static final Splitter TAG_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private final RequirementKnowledgeService requirementKnowledgeService;
  private final RequirementKnowledgeEntryService requirementKnowledgeEntryService;
  private final TaskExecutor taskExecutor;

  private final AtomicBoolean vectorizeAllRunning = new AtomicBoolean(false);

  /**
   * 上游 embedding 接口只有 10 RPM；这里对每次调用 upsertDocument 做限流。
   *
   * <p>阻塞式限流，批量向量化会按 10 RPM 的速度慢慢跑。
   */
  private final RateLimiter upsertLimiter = RateLimiter.create(UPSERTS_PER_MINUTE / 60.0d);

  public void submitVectorizeAllUnvectorized() {
    if (!vectorizeAllRunning.compareAndSet(false, true)) {
      log.info("批量向量化已在执行，忽略重复请求");
      return;
    }
    taskExecutor.execute(
        () -> {
          try {
            doVectorizeAllUnvectorized();
          } finally {
            vectorizeAllRunning.set(false);
          }
        });
  }

  private void doVectorizeAllUnvectorized() {
    List<RequirementKnowledgeEntry> targets =
        requirementKnowledgeEntryService.list(buildUnvectorizedWrapper());
    if (targets == null || targets.isEmpty()) {
      log.info("批量向量化结束: 没有未向量化条目");
      return;
    }

    for (RequirementKnowledgeEntry entry : targets) {
      if (entry == null || !StringUtils.hasText(entry.getDocumentId())) {
        continue;
      }
      try {
        vectorizeEntryWithRateLimit(entry);
      } catch (Exception ex) {
        log.error("批量向量化失败: docId={}", entry.getDocumentId(), ex);
      }
    }
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

  private static List<String> splitTags(String tags) {
    if (!StringUtils.hasText(tags)) {
      return List.of();
    }
    return new ArrayList<>(TAG_SPLITTER.splitToList(tags));
  }
}
