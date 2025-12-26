package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.rag.RequirementKnowledgeMatchResponse;
import com.excalicode.platform.core.api.rag.RequirementKnowledgeSearchRequest;
import com.excalicode.platform.core.api.rag.RequirementKnowledgeUpsertRequest;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeMatch;
import com.excalicode.platform.core.service.RequirementKnowledgeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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

  /** 手动入库知识文档 */
  @PostMapping("/documents")
  public ResponseEntity<Void> upsertKnowledge(
      @RequestBody @Valid RequirementKnowledgeUpsertRequest request) {
    log.info("手动入库需求知识: title={}, docId={}", request.getTitle(), request.getDocumentId());
    requirementKnowledgeService.upsertDocument(request.toDocument());
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
}
