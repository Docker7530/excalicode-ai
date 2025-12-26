package com.excalicode.platform.core.service;

import com.excalicode.platform.core.config.RequirementRagProperties;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeDocument;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeMatch;
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

  private final VectorStore vectorStore;
  private final RequirementRagProperties properties;
  private final StringRedisTemplate stringRedisTemplate;
  private final DocumentRerankService documentRerankService;

  /** 删除指定 documentId 对应的向量片段（不影响数据库条目） */
  public void deleteDocumentVectors(String documentId) {
    if (!StringUtils.hasText(documentId)) {
      return;
    }
    removeExistingChunks(documentId.trim());
  }

  /** 向向量库写入或更新知识文档 */
  public void upsertDocument(RequirementKnowledgeDocument document) {
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
    int overlap = Math.max(200, properties.getChunkOverlap());

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
