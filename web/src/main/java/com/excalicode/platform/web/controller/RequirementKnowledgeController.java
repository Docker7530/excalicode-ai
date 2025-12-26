package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.rag.RequirementKnowledgeEntryResponse;
import com.excalicode.platform.core.api.rag.RequirementKnowledgeEntryUpdateRequest;
import com.excalicode.platform.core.api.rag.RequirementKnowledgeMatchResponse;
import com.excalicode.platform.core.api.rag.RequirementKnowledgeSearchRequest;
import com.excalicode.platform.core.api.rag.RequirementKnowledgeUpsertRequest;
import com.excalicode.platform.core.entity.RequirementKnowledgeEntry;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeDocument;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeMatch;
import com.excalicode.platform.core.service.RequirementKnowledgeService;
import com.excalicode.platform.core.service.entity.RequirementKnowledgeEntryService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 知识库管理接口 */
@Slf4j
@RestController
@RequestMapping("/api/requirement/knowledge")
@RequiredArgsConstructor
public class RequirementKnowledgeController {

  private final RequirementKnowledgeService requirementKnowledgeService;
  private final RequirementKnowledgeEntryService requirementKnowledgeEntryService;

  /**
   * 提交知识条目（仅保存到数据库，不自动向量化）。
   *
   * <p>向量化由列表里的「向量化」按钮显式触发。
   */
  @PostMapping("/documents")
  public ResponseEntity<RequirementKnowledgeEntryResponse> upsertKnowledge(
      @RequestBody @Valid RequirementKnowledgeUpsertRequest request) {
    RequirementKnowledgeDocument document = request.toDocument().normalized();
    log.info("保存需求知识草稿: title={}, docId={}", document.getTitle(), document.getDocumentId());
    RequirementKnowledgeEntry entry = requirementKnowledgeEntryService.saveDraft(document);
    if (requirementKnowledgeService.isEnabled()) {
      // 草稿更新后向量必然过期：保证“未向量化”状态不会污染检索结果。
      requirementKnowledgeService.deleteDocumentVectors(entry.getDocumentId());
    }
    return ResponseEntity.ok(RequirementKnowledgeEntryResponse.fromEntity(entry));
  }

  /** 列出所有知识条目（用于知识入库页面的表格展示） */
  @GetMapping("/entries")
  public ResponseEntity<List<RequirementKnowledgeEntryResponse>> listEntries() {
    List<RequirementKnowledgeEntry> entries = requirementKnowledgeEntryService.listForManage();
    List<RequirementKnowledgeEntryResponse> responses =
        entries.stream().map(RequirementKnowledgeEntryResponse::fromEntity).toList();
    return ResponseEntity.ok(responses);
  }

  /** 更新知识条目（只改数据库，不自动向量化） */
  @PutMapping("/entries/{documentId}")
  public ResponseEntity<RequirementKnowledgeEntryResponse> updateEntry(
      @PathVariable String documentId,
      @RequestBody @Valid RequirementKnowledgeEntryUpdateRequest request) {
    log.info("更新需求知识草稿: docId={}, title={}", documentId, request.getTitle());
    RequirementKnowledgeEntry updated =
        requirementKnowledgeEntryService.updateDraft(
            documentId, request.getTitle(), request.getContent(), request.getTags());
    if (requirementKnowledgeService.isEnabled()) {
      // 草稿更新后向量必然过期：保证“未向量化”状态不会污染检索结果。
      requirementKnowledgeService.deleteDocumentVectors(documentId);
    }
    return ResponseEntity.ok(RequirementKnowledgeEntryResponse.fromEntity(updated));
  }

  /** 将数据库条目向量化写入向量库 */
  @PostMapping("/entries/{documentId}/vectorize")
  public ResponseEntity<Void> vectorize(@PathVariable String documentId) {
    if (!requirementKnowledgeService.isEnabled()) {
      throw new BusinessException("RAG 功能关闭，无法向量化");
    }
    RequirementKnowledgeEntry entry = requirementKnowledgeEntryService.getByDocumentId(documentId);
    if (entry == null) {
      return ResponseEntity.notFound().build();
    }

    RequirementKnowledgeDocument document =
        RequirementKnowledgeDocument.builder()
            .documentId(entry.getDocumentId())
            .title(entry.getTitle())
            .content(entry.getContent())
            .tags(splitTags(entry.getTags()))
            .build();

    requirementKnowledgeService.upsertDocument(document);
    requirementKnowledgeEntryService.updateVectorState(documentId, true);
    return ResponseEntity.ok().build();
  }

  /** 删除向量（保留数据库条目） */
  @DeleteMapping("/entries/{documentId}/vector")
  public ResponseEntity<Void> deleteVector(@PathVariable String documentId) {
    if (!requirementKnowledgeService.isEnabled()) {
      throw new BusinessException("RAG 功能关闭，无法删除向量");
    }
    RequirementKnowledgeEntry entry = requirementKnowledgeEntryService.getByDocumentId(documentId);
    if (entry == null) {
      return ResponseEntity.notFound().build();
    }
    requirementKnowledgeService.deleteDocumentVectors(documentId);
    requirementKnowledgeEntryService.updateVectorState(documentId, false);
    return ResponseEntity.ok().build();
  }

  /** 删除数据库条目（同时尝试删除向量） */
  @DeleteMapping("/entries/{documentId}")
  public ResponseEntity<Void> deleteEntry(@PathVariable String documentId) {
    log.info("删除需求知识条目: docId={}", documentId);
    RequirementKnowledgeEntry entry = requirementKnowledgeEntryService.getByDocumentId(documentId);
    if (entry == null) {
      return ResponseEntity.notFound().build();
    }
    if (requirementKnowledgeService.isEnabled()) {
      requirementKnowledgeService.deleteDocumentVectors(documentId);
    }
    requirementKnowledgeEntryService.removeByDocumentId(documentId);
    return ResponseEntity.ok().build();
  }

  /** 查询知识片段，便于自检 */
  @PostMapping("/search")
  public ResponseEntity<List<RequirementKnowledgeMatchResponse>> search(
      @RequestBody @Valid RequirementKnowledgeSearchRequest request) {
    List<RequirementKnowledgeMatch> matches =
        requirementKnowledgeService.search(
            request.getQuery(), request.getTopK(), request.getMinScore());
    List<RequirementKnowledgeMatchResponse> responses =
        matches.stream().map(RequirementKnowledgeMatchResponse::fromMatch).toList();
    return ResponseEntity.ok(responses);
  }

  private static List<String> splitTags(String tags) {
    if (!StringUtils.hasText(tags)) {
      return List.of();
    }
    String[] parts = tags.split(",");
    List<String> result = new ArrayList<>();
    for (String part : parts) {
      if (StringUtils.hasText(part)) {
        result.add(part.trim());
      }
    }
    return result;
  }
}
