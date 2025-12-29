package com.excalicode.platform.core.service;

import com.excalicode.platform.core.config.RequirementRagProperties;
import com.excalicode.platform.core.exception.BusinessException;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeDocument;
import com.excalicode.platform.core.model.rag.RequirementKnowledgeMatch;
import com.google.common.base.Splitter;
import com.google.common.primitives.Ints;
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

  private static final Splitter TAG_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

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
        metadata.put(METADATA_TAGS, String.join(",", document.getSafeTags()));
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
    if (content == null || content.isBlank()) return List.of();

    int chunkSize = properties.getChunkSize();
    int chunkOverlap = properties.getChunkOverlap();
    int length = content.length();

    if (length <= chunkSize) return List.of(content);

    if (chunkSize <= 0) {
      throw new IllegalArgumentException("chunkSize 必须大于 0");
    }
    if (chunkOverlap >= chunkSize) {
      throw new IllegalArgumentException("chunkOverlap 不能大于或等于 chunkSize");
    }

    // 执行切分 (滑动窗口算法)
    int step = chunkSize - chunkOverlap;
    int estimatedChunks = (length / step) + 1;
    var chunks = new ArrayList<String>(estimatedChunks);
    int start = 0;
    while (start < length) {
      int end = Math.min(start + chunkSize, length);
      var chunk = content.substring(start, end);
      chunks.add(chunk);
      if (end == length) {
        break;
      }
      // 移动窗口指针
      start += step;
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
    int actualTopK = Ints.constrainToRange(topK != null ? topK : properties.getTopK(), 1, 20);
    double candidate = (minScore != null) ? minScore : properties.getMinScore();
    double threshold = Math.clamp(0.0d, candidate, 1.0d);
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
    String documentId = Objects.toString(metadata.get(METADATA_DOCUMENT_ID), null);
    String title = Objects.toString(metadata.get(METADATA_TITLE), null);
    int chunkIndex = readChunkIndex(metadata.get(METADATA_CHUNK_INDEX));
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

  private int readChunkIndex(Object raw) {
    if (raw instanceof Number number) {
      return number.intValue();
    }
    if (raw instanceof String str) {
      String trimmed = str.trim();
      if (!StringUtils.hasText(trimmed)) {
        return 0;
      }
      try {
        return Integer.parseInt(trimmed);
      } catch (NumberFormatException ignored) {
        return 0;
      }
    }
    return 0;
  }

  private List<String> readTags(Object raw) {
    if (raw instanceof List<?> list) {
      return list.stream().filter(Objects::nonNull).map(Object::toString).toList();
    }
    if (raw instanceof String str) {
      String trimmed = str.trim();
      return StringUtils.hasText(trimmed) ? TAG_SPLITTER.splitToList(trimmed) : List.of();
    }
    return List.of();
  }

  /** 在相似度检索的基础上调用重排模型，对结果进行语义排序。 */
  private List<RequirementKnowledgeMatch> rerankIfNeeded(
      String query, List<RequirementKnowledgeMatch> matches) {
    if (CollectionUtils.isEmpty(matches)) return List.of();
    if (!properties.isRerankEnabled()) return matches;
    List<String> contents =
        matches.stream().map(RequirementKnowledgeMatch::getChunkContent).toList();
    var rerankResults = documentRerankService.rerank(query, contents);
    if (CollectionUtils.isEmpty(rerankResults)) return matches;
    List<RequirementKnowledgeMatch> ordered = new ArrayList<>();
    Set<Integer> processedIndices = new HashSet<>();
    for (var res : rerankResults) {
      int idx = res.index();
      if (idx >= 0 && idx < matches.size() && processedIndices.add(idx)) {
        ordered.add(matches.get(idx).toBuilder().rerankScore(res.score()).build());
      }
    }
    if (ordered.size() < matches.size()) {
      for (int i = 0; i < matches.size(); i++) {
        if (!processedIndices.contains(i)) {
          ordered.add(matches.get(i));
        }
      }
    }
    log.info("RAG重排完成 | query={} | 原始={} | Top={}", query, matches.size(), ordered.size());
    return ordered;
  }

  /** 为需求扩写构造可嵌入 Prompt 的上下文文本，防止标题重复。 */
  public String buildContext(String query) {
    List<RequirementKnowledgeMatch> matches = search(query, null, null);
    if (CollectionUtils.isEmpty(matches)) {
      return "";
    }
    StringBuilder builder = new StringBuilder();
    Set<String> usedTitles = new HashSet<>();
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
      builder.append(block);
    }
    return builder.toString().trim();
  }
}
