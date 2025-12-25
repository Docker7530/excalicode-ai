package com.excalicode.platform.core.service;

import com.excalicode.platform.core.api.rag.RequirementKnowledgeFolderImportResponse;
import com.excalicode.platform.core.config.RequirementRagProperties;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeDocument;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeFolderFile;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeMatch;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/** 基于 Redis Vector Store 的需求知识库服务 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementKnowledgeService {

  private static final String METADATA_DOCUMENT_ID = "documentId";
  private static final String METADATA_TITLE = "title";
  private static final String METADATA_TAGS = "tags";
  private static final String METADATA_CHUNK_INDEX = "chunkIndex";
  private static final Set<String> SUPPORTED_FOLDER_EXTENSIONS =
      Set.of("md", "markdown", "txt", "json");
  private static final String DEFAULT_FOLDER_NAME = "folder-upload";

  private final VectorStore vectorStore;
  private final RequirementRagProperties properties;
  private final StringRedisTemplate stringRedisTemplate;
  private final DocumentRerankService documentRerankService;

  /** 向向量库写入或更新知识文档 */
  public void upsertDocument(RequirementKnowledgeDocument document) {
    if (!properties.isEnabled()) {
      log.warn("RAG 功能关闭，忽略知识入库");
      return;
    }
    RequirementKnowledgeDocument normalized =
        Objects.requireNonNull(document, "document 不能为空").normalized();
    if (!StringUtils.hasText(normalized.getContent())) {
      throw new BusinessException("知识内容不能为空");
    }

    List<Document> chunks = buildDocuments(normalized);
    if (chunks.isEmpty()) {
      throw new BusinessException("无法从知识内容中提取有效片段");
    }

    removeExistingChunks(normalized.getDocumentId());
    vectorStore.add(chunks);
    cacheChunkIds(normalized.getDocumentId(), chunks);
    log.info("知识文档 {} 入库完成，片段数 {}", normalized.getDocumentId(), chunks.size());
  }

  /** 遍历文件夹上传的文本文件，批量完成向量化 */
  public RequirementKnowledgeFolderImportResponse importFolder(
      String folderName, List<RequirementKnowledgeFolderFile> uploadedFiles) {
    if (!properties.isEnabled()) {
      throw new BusinessException("RAG 功能已关闭，无法执行批量导入");
    }
    List<RequirementKnowledgeFolderFile> safeFiles =
        CollectionUtils.isEmpty(uploadedFiles) ? List.of() : uploadedFiles;
    if (safeFiles.isEmpty()) {
      throw new BusinessException("文件夹为空，未发现可处理的文本文件");
    }

    String normalizedFolderName = normalizeFolderName(folderName);
    List<RequirementKnowledgeFolderImportResponse.FileImportResult> results =
        new ArrayList<>(safeFiles.size());
    int eligibleFiles = 0;
    int ingestedFiles = 0;

    for (RequirementKnowledgeFolderFile folderFile : safeFiles) {
      if (folderFile == null) {
        results.add(
            RequirementKnowledgeFolderImportResponse.FileImportResult.builder()
                .path("(unknown)")
                .ingested(false)
                .reason("空文件，已跳过")
                .build());
        continue;
      }

      String normalizedPath =
          sanitizeRelativePath(folderFile.getRelativePath(), folderFile.getFileName());
      if (!StringUtils.hasText(normalizedPath)) {
        results.add(
            RequirementKnowledgeFolderImportResponse.FileImportResult.builder()
                .path(folderFile.getFileName())
                .ingested(false)
                .reason("无法识别文件路径")
                .build());
        continue;
      }

      String extension = resolveExtension(normalizedPath, folderFile.getFileName());
      if (!isSupportedFolderExtension(extension)) {
        results.add(
            RequirementKnowledgeFolderImportResponse.FileImportResult.builder()
                .path(normalizedPath)
                .ingested(false)
                .reason("仅支持 .md/.txt/.json 文件")
                .build());
        continue;
      }

      String content = folderFile.getContent();
      if (!StringUtils.hasText(content)) {
        results.add(
            RequirementKnowledgeFolderImportResponse.FileImportResult.builder()
                .path(normalizedPath)
                .ingested(false)
                .reason("文件内容为空")
                .build());
        continue;
      }

      eligibleFiles++;
      try {
        RequirementKnowledgeDocument document =
            buildDocumentFromFolderFile(normalizedFolderName, normalizedPath, extension, content);
        upsertDocument(document);
        ingestedFiles++;
        results.add(
            RequirementKnowledgeFolderImportResponse.FileImportResult.builder()
                .path(normalizedPath)
                .documentId(document.getDocumentId())
                .ingested(true)
                .build());
      } catch (Exception ex) {
        log.warn("批量导入知识失败: path={}, reason={}", normalizedPath, ex.getMessage());
        results.add(
            RequirementKnowledgeFolderImportResponse.FileImportResult.builder()
                .path(normalizedPath)
                .ingested(false)
                .reason(ex.getMessage())
                .build());
      }
    }

    int skippedFiles = results.size() - ingestedFiles;
    log.info(
        "批量导入需求知识: folder={}, 总计 {}，合格 {}，成功 {}，跳过 {}",
        normalizedFolderName,
        safeFiles.size(),
        eligibleFiles,
        ingestedFiles,
        skippedFiles);

    return RequirementKnowledgeFolderImportResponse.builder()
        .folderName(normalizedFolderName)
        .totalFiles(safeFiles.size())
        .eligibleFiles(eligibleFiles)
        .ingestedFiles(ingestedFiles)
        .skippedFiles(skippedFiles)
        .fileResults(results)
        .build();
  }

  /**
   * 将大段文本切片并补充 metadata，供向量库写入。
   *
   * @param document 业务领域文档
   * @return 每个 chunk 对应的向量文档
   */
  private List<Document> buildDocuments(RequirementKnowledgeDocument document) {
    List<String> contentChunks = splitIntoChunks(document.getContent());
    List<Document> documents = new ArrayList<>();
    int chunkIndex = 0;
    for (String chunk : contentChunks) {
      Map<String, Object> metadata = new HashMap<>();
      metadata.put(METADATA_DOCUMENT_ID, document.getDocumentId());
      putIfText(metadata, METADATA_TITLE, document.getTitle());
      if (!CollectionUtils.isEmpty(document.getSafeTags())) {
        metadata.put(METADATA_TAGS, document.getSafeTags());
      }
      metadata.put(METADATA_CHUNK_INDEX, chunkIndex);
      Document ragDocument =
          new Document(document.getDocumentId() + "::" + chunkIndex, chunk, metadata);
      documents.add(ragDocument);
      chunkIndex++;
    }
    return documents;
  }

  private RequirementKnowledgeDocument buildDocumentFromFolderFile(
      String folderName, String normalizedPath, String extension, String content) {
    String safeFolderName = StringUtils.hasText(folderName) ? folderName : DEFAULT_FOLDER_NAME;
    String identifierSeed = safeFolderName + "::" + normalizedPath;
    String documentId = DigestUtils.md5DigestAsHex(identifierSeed.getBytes(StandardCharsets.UTF_8));

    List<String> tags = new ArrayList<>();
    tags.add(DEFAULT_FOLDER_NAME);
    if (StringUtils.hasText(folderName) && !DEFAULT_FOLDER_NAME.equals(folderName)) {
      tags.add(folderName);
    }
    if (StringUtils.hasText(extension)) {
      tags.add(extension);
    }

    return RequirementKnowledgeDocument.builder()
        .documentId(documentId)
        .title(normalizedPath)
        .content(content)
        .tags(tags)
        .build();
  }

  private String normalizeFolderName(String folderName) {
    if (!StringUtils.hasText(folderName)) {
      return DEFAULT_FOLDER_NAME;
    }
    String trimmed = folderName.trim().replace('\\', '/');
    if (trimmed.contains("/")) {
      trimmed = trimmed.substring(trimmed.lastIndexOf('/') + 1);
    }
    if (trimmed.length() > 64) {
      trimmed = trimmed.substring(0, 64);
    }
    return StringUtils.hasText(trimmed) ? trimmed : DEFAULT_FOLDER_NAME;
  }

  private String sanitizeRelativePath(String rawPath, String fallback) {
    String candidate = StringUtils.hasText(rawPath) ? rawPath : fallback;
    if (!StringUtils.hasText(candidate)) {
      return "";
    }
    String cleaned = StringUtils.cleanPath(candidate).replace('\\', '/');
    while (cleaned.startsWith("/")) {
      cleaned = cleaned.substring(1);
    }
    if (cleaned.startsWith("..")) {
      return StringUtils.hasText(fallback) ? fallback : "";
    }
    return cleaned;
  }

  private String resolveExtension(String path, String fallbackName) {
    String target = StringUtils.hasText(path) ? path : fallbackName;
    if (!StringUtils.hasText(target)) {
      return "";
    }
    String fileName = target;
    int slashIndex = fileName.lastIndexOf('/');
    if (slashIndex >= 0 && slashIndex < fileName.length() - 1) {
      fileName = fileName.substring(slashIndex + 1);
    }
    int dotIndex = fileName.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
      return "";
    }
    return fileName.substring(dotIndex + 1).toLowerCase();
  }

  private boolean isSupportedFolderExtension(String extension) {
    if (!StringUtils.hasText(extension)) {
      return false;
    }
    return SUPPORTED_FOLDER_EXTENSIONS.contains(extension.toLowerCase());
  }

  private void putIfText(Map<String, Object> metadata, String key, String value) {
    if (StringUtils.hasText(value)) {
      metadata.put(key, value);
    }
  }

  /** 根据配置的 chunk 尺寸将文本分段，并尽量在换行处分割，减少语义断裂。 */
  private List<String> splitIntoChunks(String content) {
    if (!StringUtils.hasText(content)) {
      return List.of();
    }
    String normalized = content.replace("\r", "").trim();
    int chunkSize = Math.max(200, properties.getChunkSize());
    int overlap = Math.clamp(0, properties.getChunkOverlap(), chunkSize - 1);

    List<String> chunks = new ArrayList<>();
    int start = 0;
    while (start < normalized.length()) {
      int end = Math.min(normalized.length(), start + chunkSize);
      if (end < normalized.length()) {
        int newline = normalized.lastIndexOf('\n', end);
        if (newline > start + chunkSize / 2) {
          end = newline;
        }
      }
      String chunk = normalized.substring(start, end).trim();
      if (StringUtils.hasText(chunk)) {
        chunks.add(chunk);
      }
      if (end >= normalized.length()) {
        break;
      }
      start = Math.max(0, end - overlap);
      if (start == end) {
        start = end + 1;
      }
    }
    return chunks;
  }

  /** 清理旧的 chunk 记录，避免重复写入导致语义冲突或冗余向量。 */
  private void removeExistingChunks(String documentId) {
    String redisKey = buildRedisKey(documentId);
    Set<String> existingChunkIds = stringRedisTemplate.opsForSet().members(redisKey);
    if (CollectionUtils.isEmpty(existingChunkIds)) {
      return;
    }
    try {
      vectorStore.delete(new ArrayList<>(existingChunkIds));
      log.info("删除知识文档 {} 旧片段 {} 个", documentId, existingChunkIds.size());
    } catch (Exception ex) {
      log.warn("删除旧片段失败: {}", documentId, ex);
    } finally {
      stringRedisTemplate.delete(redisKey);
    }
  }

  /** 将 chunk id 写入 Redis，便于后续删除或同步操作直接获取需要清理的键。 */
  private void cacheChunkIds(String documentId, List<Document> chunks) {
    if (CollectionUtils.isEmpty(chunks)) {
      return;
    }
    String redisKey = buildRedisKey(documentId);
    List<String> chunkIds = new ArrayList<>();
    for (Document doc : chunks) {
      if (StringUtils.hasText(doc.getId())) {
        chunkIds.add(doc.getId());
      }
    }
    if (chunkIds.isEmpty()) {
      return;
    }
    stringRedisTemplate.opsForSet().add(redisKey, chunkIds.toArray(String[]::new));
    Duration ttl = properties.getChunkKeyTtl();
    if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
      stringRedisTemplate.expire(redisKey, ttl);
    }
  }

  /** 根据配置的前缀拼装 chunk 索引在 Redis 中的存储地址 */
  private String buildRedisKey(String documentId) {
    return properties.getRedisChunkKeyPrefix() + documentId;
  }

  /** 根据查询语句检索相关知识片段 */
  public List<RequirementKnowledgeMatch> search(String query, Integer topK, Double minScore) {
    if (!properties.isEnabled() || !StringUtils.hasText(query)) {
      return List.of();
    }
    int actualTopK =
        topK != null && topK > 0 ? Math.min(topK, 20) : Math.max(1, properties.getTopK());
    double threshold =
        minScore != null
            ? Math.clamp(0.0d, minScore, 1.0d)
            : Math.clamp(0.0d, properties.getMinScore(), 1.0d);

    SearchRequest request =
        SearchRequest.builder()
            .query(query.trim())
            .topK(actualTopK)
            .similarityThreshold(threshold)
            .build();

    List<Document> documents = vectorStore.similaritySearch(request);
    if (CollectionUtils.isEmpty(documents)) {
      return List.of();
    }
    List<RequirementKnowledgeMatch> matches = new ArrayList<>();
    for (Document doc : documents) {
      RequirementKnowledgeMatch match = toMatch(doc);
      if (match != null) {
        matches.add(match);
      }
    }
    return rerankIfNeeded(query, matches);
  }

  /** 将向量库返回的 Document 转为领域对象 */
  private RequirementKnowledgeMatch toMatch(Document document) {
    if (document == null || !StringUtils.hasText(document.getText())) {
      return null;
    }
    Map<String, Object> metadata = document.getMetadata();
    String documentId = (String) metadata.get(METADATA_DOCUMENT_ID);
    String title = (String) metadata.get(METADATA_TITLE);

    // 处理 chunkIndex
    Object rawChunkIndex = metadata.get(METADATA_CHUNK_INDEX);
    int chunkIndex = rawChunkIndex instanceof Number number ? number.intValue() : 0;

    // 处理其他复杂类型
    List<String> tags = readTags(metadata.get(METADATA_TAGS));

    return RequirementKnowledgeMatch.builder()
        .documentId(documentId)
        .title(title)
        .chunkContent(document.getText())
        .tags(tags)
        .chunkIndex(chunkIndex)
        .similarityScore(document.getScore())
        .build();
  }

  /** metadata 中的标签字段可能为数组或逗号分隔字符串，需要统一解析 */
  private List<String> readTags(Object raw) {
    if (raw instanceof List<?> list) {
      List<String> tags = new ArrayList<>();
      for (Object element : list) {
        if (element != null) {
          tags.add(String.valueOf(element));
        }
      }
      return tags;
    }
    if (raw instanceof String str) {
      String[] parts = str.split(",");
      List<String> tags = new ArrayList<>();
      for (String part : parts) {
        if (StringUtils.hasText(part)) {
          tags.add(part.trim());
        }
      }
      return tags;
    }
    return List.of();
  }

  /** 在相似度检索的基础上调用重排模型，对结果进行语义排序。 */
  private List<RequirementKnowledgeMatch> rerankIfNeeded(
      String query, List<RequirementKnowledgeMatch> matches) {
    if (CollectionUtils.isEmpty(matches)) {
      return List.of();
    }
    if (!properties.isRerankEnabled()) {
      return matches;
    }
    List<String> contents = new ArrayList<>(matches.size());
    for (RequirementKnowledgeMatch match : matches) {
      contents.add(match.getChunkContent());
    }
    List<DocumentRerankService.DocumentRerankResult> rerankResults =
        documentRerankService.rerank(query, contents);
    if (CollectionUtils.isEmpty(rerankResults)) {
      return matches;
    }
    Map<Integer, RequirementKnowledgeMatch> reranked = new HashMap<>();
    for (DocumentRerankService.DocumentRerankResult result : rerankResults) {
      int index = result.index();
      if (index >= 0 && index < matches.size()) {
        RequirementKnowledgeMatch rerankedMatch =
            matches.get(index).toBuilder().rerankScore(result.score()).build();
        reranked.put(index, rerankedMatch);
      }
    }
    List<RequirementKnowledgeMatch> ordered = new ArrayList<>();
    for (DocumentRerankService.DocumentRerankResult result : rerankResults) {
      RequirementKnowledgeMatch match = reranked.get(result.index());
      if (match != null) {
        ordered.add(match);
      }
    }
    if (ordered.size() < matches.size()) {
      for (int i = 0; i < matches.size(); i++) {
        if (!reranked.containsKey(i)) {
          ordered.add(matches.get(i));
        }
      }
    }
    log.info("RAG 重排完成，query=\"{}\"，原始 {} 条，重排后 Top {} 条", query, matches.size(), ordered.size());
    return ordered;
  }

  /** 为需求扩写构造可嵌入 Prompt 的上下文文本，自动控制长度、防止标题重复。 */
  public String buildContext(String query) {
    List<RequirementKnowledgeMatch> matches = search(query, null, null);
    if (CollectionUtils.isEmpty(matches)) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    Set<String> usedTitles = new HashSet<>();
    int maxChars = Math.max(500, properties.getContextMaxChars());
    for (int i = 0; i < matches.size(); i++) {
      RequirementKnowledgeMatch match = matches.get(i);
      String title =
          StringUtils.hasText(match.getTitle())
              ? match.getTitle()
              : String.format("知识片段 %d", i + 1);
      if (!usedTitles.add(title)) {
        title = title + "#" + match.getChunkIndex();
      }
      String block =
          "【" + title + "】" + System.lineSeparator() + match.getChunkContent().trim() + "\n\n";
      if (builder.length() + block.length() > maxChars) {
        int remain = maxChars - builder.length();
        if (remain > 0) {
          builder.append(block, 0, Math.min(remain, block.length()));
        }
        break;
      }
      builder.append(block);
    }
    return builder.toString().trim();
  }
}
