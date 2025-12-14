package com.excalicode.platform.web.controller;

import com.excalicode.platform.core.api.rag.RequirementKnowledgeFolderImportResponse;
import com.excalicode.platform.core.api.rag.RequirementKnowledgeMatchResponse;
import com.excalicode.platform.core.api.rag.RequirementKnowledgeSearchRequest;
import com.excalicode.platform.core.api.rag.RequirementKnowledgeUpsertRequest;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeFolderFile;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeMatch;
import com.excalicode.platform.core.service.RequirementKnowledgeService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

  /** 将整个文件夹的文本文件批量写入向量库 */
  @PostMapping(value = "/folders/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<RequirementKnowledgeFolderImportResponse> importFolder(
      @RequestPart(value = "folderName", required = false) String folderName,
      @RequestPart("files") List<MultipartFile> files) {
    List<RequirementKnowledgeFolderFile> folderFiles = convertFolderFiles(files);
    log.info("批量导入需求知识: folder={}, files={}", folderName, folderFiles.size());
    RequirementKnowledgeFolderImportResponse response =
        requirementKnowledgeService.importFolder(folderName, folderFiles);
    return ResponseEntity.ok(response);
  }

  private List<RequirementKnowledgeFolderFile> convertFolderFiles(List<MultipartFile> files) {
    List<RequirementKnowledgeFolderFile> folderFiles = new ArrayList<>();
    if (files == null) {
      return folderFiles;
    }
    for (MultipartFile multipartFile : files) {
      if (multipartFile == null || multipartFile.isEmpty()) {
        continue;
      }
      folderFiles.add(toFolderFile(multipartFile));
    }
    return folderFiles;
  }

  private RequirementKnowledgeFolderFile toFolderFile(MultipartFile multipartFile) {
    try {
      String originalFilename = multipartFile.getOriginalFilename();
      String relativePath = resolveRelativePath(originalFilename);
      String fileName =
          StringUtils.hasText(originalFilename)
              ? StringUtils.getFilename(originalFilename)
              : multipartFile.getName();
      String content = new String(multipartFile.getBytes(), StandardCharsets.UTF_8);
      return RequirementKnowledgeFolderFile.builder()
          .fileName(fileName)
          .relativePath(relativePath)
          .content(content)
          .build();
    } catch (IOException ex) {
      throw new BusinessException("读取文件失败: " + multipartFile.getOriginalFilename(), ex);
    }
  }

  private String resolveRelativePath(String rawPath) {
    if (!StringUtils.hasText(rawPath)) {
      return null;
    }
    String cleaned = StringUtils.cleanPath(rawPath).replace('\\', '/');
    while (cleaned.startsWith("/")) {
      cleaned = cleaned.substring(1);
    }
    return cleaned;
  }
}
